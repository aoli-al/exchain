package al.aoli.exchain.runtime.analyzers

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.analysis.Value

class AffectedVarAnalyser<V : Value>(
    insnList: InsnList,
    interpreter: Interpreter<V>,
    val throwInsn: AbstractInsnNode,
    val catchInsn: AbstractInsnNode?
) : DataFlowAnalyzer<V>(insnList, interpreter) {

  override fun newControlFlowExceptionEdge(insnIndex: Int, successorIndex: Int): Boolean {
    if (catchInsn == null) return false
    return instructions[insnIndex] == throwInsn && instructions[successorIndex] == catchInsn
  }

  override fun newControlFlowExceptionEdge(
      insnIndex: Int,
      tryCatchBlock: TryCatchBlockNode?
  ): Boolean {
    return super.newControlFlowExceptionEdge(insnIndex, tryCatchBlock)
  }
}
