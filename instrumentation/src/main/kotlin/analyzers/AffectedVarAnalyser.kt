package al.aoli.exchain.instrumentation.analyzers

import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.analysis.Value

class AffectedVarAnalyser<V: Value>(interpreter: Interpreter<V>, val throwIndex: Int, val catchIndex: Int) :
    Analyzer<V>(interpreter) {

    override fun newControlFlowExceptionEdge(insnIndex: Int, successorIndex: Int): Boolean {
        return throwIndex == insnIndex && successorIndex == catchIndex
    }

    override fun newControlFlowExceptionEdge(insnIndex: Int, tryCatchBlock: TryCatchBlockNode?): Boolean {
        return super.newControlFlowExceptionEdge(insnIndex, tryCatchBlock)
    }
}