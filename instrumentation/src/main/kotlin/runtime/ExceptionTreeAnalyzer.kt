package al.aoli.exchain.instrumentation.runtime

import java.io.File
import java.lang.reflect.Method

object ExceptionTreeAnalyzer {
    private val DUMMY = Node(Throwable("dummy"), "")

    private val exceptionStack = mutableListOf(DUMMY)
    private val caughtExceptions = mutableListOf(DUMMY)
    private val output = File("/tmp/enter.txt")
    private val visitedOrigins = mutableSetOf<String>()

    init {
        output.writeText("")
    }


    fun methodEnter(origin: String, method: Method) {
        if (origin in visitedOrigins) return
        val data = origin.split("@")
        visitedOrigins.add(origin)
        val exceptions = method.exceptionTypes.joinToString(",") { it.name }
        output.appendText("Enter: ${data[0]}.${data[1]}${data[2]}, throws: [$exceptions]\n")
    }


    fun push(throwable: Throwable, origin: String): Boolean {
        if (throwable == exceptionStack.last().throwable) return false
        val node = Node(throwable, origin, exceptionStack.last())
        exceptionStack.last().successors.add(node)
        exceptionStack.add(node)
        return true
    }

    fun exceptionCaught(e: Throwable) {
        assert(exceptionStack.last().throwable == e)
        caughtExceptions.add(exceptionStack.last())
    }

    fun catchWithException(e: Throwable, origin: String) {
        push(e, origin)
        if (e != caughtExceptions.last().throwable) {
            val node = exceptionStack.removeAt(exceptionStack.size - 2)
            showDependency(node)
        }
        caughtExceptions.removeLast()
    }

    fun showDependency(node: Node) {
        var last: Node? = node
        println("=========  Exception Caught With Chain: =======")
        var first = true
        while (last != DUMMY && last != null) {
            if (first) {
                println("Exception: ${last.throwable::class.java}, thrown at: ${last.origin}")
                first = false
            } else {
                println("Caused By: ${last.throwable::class.java}, thrown at: ${last.origin}")
            }
            last = last.predecessor
        }
        println("=========  end  =======")
    }

    fun catchEnd() {
        caughtExceptions.removeLast()
        showDependency(exceptionStack.removeLast())
    }
}
