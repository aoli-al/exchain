package al.aoli.exception.instrumentation.analyzers

object ExceptionTreeAnalyzer {
    val exceptionStack = mutableListOf(Node(Throwable("dummy")))

    fun push(throwable: Throwable) {
        if (throwable == exceptionStack.last().throwable) return
        val node = Node(throwable, exceptionStack.last())
        exceptionStack.last().successors.add(node)
        exceptionStack.add(node)
    }

    fun pushThenPop(throwable: Throwable) {
        push(throwable)
        exceptionStack.removeAt(exceptionStack.size - 2)
    }

    fun showDependency(throwable: Throwable) {
        push(throwable)
        var last: Node? = exceptionStack.last()
        while (last != null) {
            println("Exception: ${last.throwable}")
            last = last.predecessor
        }
    }

    fun pop() {
        exceptionStack.removeLast()
    }
}
