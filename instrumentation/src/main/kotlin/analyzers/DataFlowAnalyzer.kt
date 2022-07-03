package al.aoli.exception.instrumentation.analyzers

import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.analysis.Value

class DataFlowAnalyzer<V: Value>(interpreter: Interpreter<V>): Analyzer<V>(interpreter) {
    val successors = mutableMapOf<Int, MutableSet<Int>>()
    val predecessors = mutableMapOf<Int, MutableSet<Int>>()

    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        successors.getOrPut(insnIndex) { mutableSetOf() }.add(successorIndex)
        super.newControlFlowEdge(insnIndex, successorIndex)
    }

}
