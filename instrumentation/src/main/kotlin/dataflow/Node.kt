package al.aoli.exception.instrumentation.dataflow

import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.Value

class Node<V: Value>: Frame<V> {
    constructor(frame: Frame<out V>) : super(frame)

    constructor(local: Int, stack: Int) : super(local, stack)

    val successors = mutableSetOf<Node<V>>()
    val predecessors = mutableSetOf<Node<V>>()
}