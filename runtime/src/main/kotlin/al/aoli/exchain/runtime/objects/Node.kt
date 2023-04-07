package al.aoli.exchain.runtime.objects

class Node(val throwable: Throwable, val origin: String, val predecessor: Node? = null) {
  val successors = mutableSetOf<Node>()
}
