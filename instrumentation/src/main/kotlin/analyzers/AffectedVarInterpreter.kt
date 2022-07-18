package al.aoli.exchain.instrumentation.analyzers

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.SourceInterpreter
import org.objectweb.asm.tree.analysis.SourceValue

class AffectedVarInterpreter(val thrownIndex: Int, val instructions: InsnList) : SourceInterpreter(ASM8) {

    private var thrownInstructionVisited = false

    private fun checkExceptionInstructionStarts(insn: AbstractInsnNode) {
        if (instructions[thrownIndex] == insn) {
            thrownInstructionVisited = true
        }
    }

    override fun newOperation(insn: AbstractInsnNode): SourceValue {
        checkExceptionInstructionStarts(insn)
        return super.newOperation(insn)
    }

    override fun copyOperation(insn: AbstractInsnNode, value: SourceValue): SourceValue {
        checkExceptionInstructionStarts(insn)
        return super.copyOperation(insn, value)
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: SourceValue): SourceValue {
        checkExceptionInstructionStarts(insn)
        if (thrownInstructionVisited && insn is FieldInsnNode && insn.opcode == PUTSTATIC) {
            println(insn.owner + "." + insn.name)
        }
        return super.unaryOperation(insn, value)
    }

    override fun binaryOperation(insn: AbstractInsnNode, value1: SourceValue, value2: SourceValue): SourceValue {
        checkExceptionInstructionStarts(insn)
        if (thrownInstructionVisited && insn is FieldInsnNode) {
            for (sourceInsn in value1.insns) {
                if (sourceInsn is VarInsnNode) {
                    println(sourceInsn.`var`)
                }
            }
        }
        return super.binaryOperation(insn, value1, value2)
    }

    override fun ternaryOperation(
        insn: AbstractInsnNode, value1: SourceValue, value2: SourceValue, value3: SourceValue
    ): SourceValue {
        checkExceptionInstructionStarts(insn)
        return super.ternaryOperation(insn, value1, value2, value3)
    }

    override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out SourceValue>): SourceValue {
        checkExceptionInstructionStarts(insn)
        return super.naryOperation(insn, values)
    }

    override fun returnOperation(insn: AbstractInsnNode, value: SourceValue, expected: SourceValue) {
        checkExceptionInstructionStarts(insn)
        super.returnOperation(insn, value, expected)
    }

}