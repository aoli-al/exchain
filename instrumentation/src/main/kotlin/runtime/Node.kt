package al.aoli.exception.instrumentation.runtime


class Node(val throwable: Throwable, val origin: String, val predecessor: Node? = null) {
    val successors = mutableSetOf<Node>()
}