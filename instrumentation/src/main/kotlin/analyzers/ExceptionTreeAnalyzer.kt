package al.aoli.exception.instrumentation.analyzers

object ExceptionTreeAnalyzer {
    private val DUMMY = Node(Throwable("dummy"), "")

    private val exceptionStack = mutableListOf(DUMMY)
    private val caughtExceptions = mutableListOf(DUMMY)

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
        showDependency(exceptionStack.removeLast())
    }
}
