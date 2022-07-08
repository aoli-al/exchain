package al.aoli.exception.instrumentation.analyzers

import java.io.File

object ExceptionTreeAnalyzer {
    private val DUMMY = Node(Throwable("dummy"), "")

    private val exceptionStack = mutableListOf(DUMMY)
    private val caughtExceptions = mutableListOf(DUMMY)
    private val output = File("/tmp/enter.txt")
    private val visitedOrigins = mutableSetOf<String>()

    init {
        output.writeText("")
    }

    fun methodEnter(origin: String) {
        if (origin in visitedOrigins) return
        visitedOrigins.add(origin)
        val data = origin.split("@")

        val clazz = Class.forName(data[0])

        for (method in clazz.declaredMethods) {
//            if (method.name == results[1] && method.para)
            if (data[1] == method.name) {
//                method.isAccessible = true
                println(method)
                method.toString()

                val params = method.parameterTypes.joinToString(",") { it.name }

                if ("($params)" == data[2]) {
                    val exceptions = method.exceptionTypes.joinToString(",") { it.name }
                    output.appendText("Enter: ${data[0]}.${data[1]}${data[2]}, throws: [$exceptions]\n")
                    return
                }
            }
        }

        output.appendText("Error: cannot find method with origin: $origin\n")

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

    fun catchWithException(throwable: Throwable, origin: String) {
        push(throwable, origin)
        if (throwable != caughtExceptions.last().throwable) {
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
