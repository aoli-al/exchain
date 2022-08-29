package al.aoli.exchain.runtime.analyzers

import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.analysis.Value

class ExceptionFlowAnalyzer<V: Value>(insnList: InsnList, interpreter: Interpreter<V>):
    DataFlowAnalyzer<V>(insnList, interpreter) {
    private var includeCatchBlock = false


    fun analyze(owner: String?, method: MethodNode?, enableException: Boolean = false): Array<Frame<V>> {
        includeCatchBlock = enableException
        return analyze(owner, method)
    }

    override fun newControlFlowExceptionEdge(insnIndex: Int, successorIndex: Int): Boolean {
        if (includeCatchBlock) {
            val insn = instructions.get(insnIndex)!!
            return when (insn.opcode) {
                AALOAD, AASTORE, ANEWARRAY, ARETURN, ARRAYLENGTH, ATHROW, BALOAD, BASTORE, CALOAD, CASTORE, CHECKCAST,
                DALOAD, DASTORE, DRETURN, FALOAD, FASTORE, FRETURN, GETFIELD, GETSTATIC, IALOAD, IASTORE, INSTANCEOF,
                INVOKEDYNAMIC, INVOKEINTERFACE, INVOKESPECIAL, INVOKESTATIC, INVOKEVIRTUAL, IRETURN, LALOAD, LASTORE,
                LDC, LRETURN, MONITOREXIT, MULTIANEWARRAY, NEW, PUTFIELD, PUTSTATIC, RETURN, SALOAD, SASTORE -> true
                else -> false
            }
        }
        return false
    }
}