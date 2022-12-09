package al.aoli.exchain.runtime.analyzers

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.analysis.SourceInterpreter
import org.objectweb.asm.tree.analysis.SourceValue

class AffectedVarInterpreter(
    val insnList: InsnList,
    val throwInsnIndex: Int,
    val tryBlocks: List<Pair<Int, Int>>
) : SourceInterpreter(ASM8) {

    val affectedInsns = mutableSetOf<AbstractInsnNode>()
    val affectedInsnInTry = mutableSetOf<AbstractInsnNode>()
    val visitedInsns = mutableSetOf<AbstractInsnNode>()

    private fun checkExceptionInstructionStarts(insn: AbstractInsnNode) {
        // We only want to process instructions once to avoid loops.
        if (insn in visitedInsns) {
            return
        }
        visitedInsns.add(insn)
        if (insnList.indexOf(insn) > throwInsnIndex) {
            affectedInsns.add(insn)
            val insnIndex = insnList.indexOf(insn)
            for (tryBlock in tryBlocks) {
                if (insnIndex in tryBlock.first..tryBlock.second) {
                    affectedInsnInTry.add(insn)
                }
            }
        }
    }

    override fun newOperation(insn: AbstractInsnNode): SourceValue? {
        checkExceptionInstructionStarts(insn)
        return super.newOperation(insn)
    }

    override fun copyOperation(insn: AbstractInsnNode, value: SourceValue): SourceValue? {
        checkExceptionInstructionStarts(insn)
        return when (insn.opcode) {
            DUP,
            DUP_X1,
            DUP_X2,
            DUP2,
            DUP2_X1,
            DUP2_X2,
            SWAP -> value
            else -> super.copyOperation(insn, value)
        }
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: SourceValue): SourceValue? {
        checkExceptionInstructionStarts(insn)
        //        if (thrownInstructionVisited && insn is FieldInsnNode && insn.opcode == PUTSTATIC) {
        //            println(insn.owner + "." + insn.name)
        //        }
        return super.unaryOperation(insn, value)
    }

    override fun binaryOperation(
        insn: AbstractInsnNode,
        value1: SourceValue,
        value2: SourceValue
    ): SourceValue? {
        checkExceptionInstructionStarts(insn)
        //        if (thrownInstructionVisited && insn is FieldInsnNode) {
        //            for (sourceInsn in value1.insns) {
        //                if (sourceInsn is VarInsnNode) {
        //                    println(sourceInsn.`var`)
        //                }
        //            }
        //        }
        return super.binaryOperation(insn, value1, value2)
    }

    override fun ternaryOperation(
        insn: AbstractInsnNode,
        value1: SourceValue,
        value2: SourceValue,
        value3: SourceValue
    ): SourceValue? {
        checkExceptionInstructionStarts(insn)
        return super.ternaryOperation(insn, value1, value2, value3)
    }

    override fun naryOperation(
        insn: AbstractInsnNode,
        values: MutableList<out SourceValue>
    ): SourceValue? {
        checkExceptionInstructionStarts(insn)
        return super.naryOperation(insn, values)
    }

    override fun returnOperation(insn: AbstractInsnNode, value: SourceValue, expected: SourceValue) {
        checkExceptionInstructionStarts(insn)
        super.returnOperation(insn, value, expected)
    }
}
