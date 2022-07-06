package al.aoli.exception.instrumentation.analyzers

import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.analysis.Value

class DataFlowAnalyzer<V: Value>(interpreter: Interpreter<V>, val includeCatchBlock: Boolean): Analyzer<V>(interpreter) {
    val successors = mutableMapOf<Int, MutableSet<Int>>()
    val predecessors = mutableMapOf<Int, MutableSet<Int>>()

    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        successors.getOrPut(insnIndex) { mutableSetOf() }.add(successorIndex)
        super.newControlFlowEdge(insnIndex, successorIndex)
    }


    override fun newControlFlowExceptionEdge(insnIndex: Int, tryCatchBlock: TryCatchBlockNode?): Boolean {
        return includeCatchBlock
    }
}
