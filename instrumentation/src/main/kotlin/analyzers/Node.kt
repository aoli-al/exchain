package al.aoli.exception.instrumentation.analyzers

class Node(val throwable: Throwable, val predecessor: Node? = null) {
    val successors = mutableSetOf<Node>()
}