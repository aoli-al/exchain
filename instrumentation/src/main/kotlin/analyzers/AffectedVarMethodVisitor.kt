package al.aoli.exchain.instrumentation.analyzers

import org.objectweb.asm.ConstantDynamic
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.SourceInterpreter


class AffectedVarMethodVisitor(val throwIndex: Int, val catchIndex: Int, val owner: String, access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?):
    MethodNode(Opcodes.ASM8, access, name, descriptor, signature, exceptions) {
    var byteCodeOffset = 0;

    override fun visitInsn(opcode: Int) {
        byteCodeOffset += 1
        super.visitInsn(opcode)
    }

    override fun visitVarInsn(opcode: Int, varIndex: Int) {
        byteCodeOffset += if (varIndex < 4 && opcode != Opcodes.RET) {
            1
        } else if (varIndex > 256) {
            4
        } else {
            2
        }
        super.visitVarInsn(opcode, varIndex)
    }

    override fun visitJumpInsn(opcode: Int, label: Label?) {
        byteCodeOffset += 3
        super.visitJumpInsn(opcode, label)
    }

    override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label, vararg labels: Label) {
        byteCodeOffset += 4 - (byteCodeOffset and 3)
        byteCodeOffset += 12 + 4 * labels.size
        super.visitTableSwitchInsn(min, max, dflt, *labels)
    }

    override fun visitLookupSwitchInsn(dflt: Label, keys: IntArray, labels: Array<out Label>) {
        byteCodeOffset += 4 - (byteCodeOffset and 3)
        byteCodeOffset += 8 + 8 * labels.size
        super.visitLookupSwitchInsn(dflt, keys, labels)
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        byteCodeOffset += 3
        super.visitFieldInsn(opcode, owner, name, descriptor)
    }

    override fun visitMethodInsn(
        opcodeAndSource: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        val opcode = opcodeAndSource and Opcodes.SOURCE_MASK.inv()
        byteCodeOffset += if (opcode == Opcodes.INVOKEINTERFACE) {
            5
        } else {
            3
        }
        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
    }

    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ) {
        byteCodeOffset += 5
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        byteCodeOffset += 3
        super.visitTypeInsn(opcode, type)
    }

    override fun visitIincInsn(varIndex: Int, increment: Int) {
        byteCodeOffset += if (varIndex > 255 || increment > 127 || increment < -128) {
            6
        } else {
            3
        }
        super.visitIincInsn(varIndex, increment)
    }

    override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
        byteCodeOffset += 4
        super.visitMultiANewArrayInsn(descriptor, numDimensions)
    }


    override fun visitIntInsn(opcode: Int, operand: Int) {
        byteCodeOffset += when (opcode) {
            Opcodes.BIPUSH, Opcodes.NEWARRAY -> 2
            else -> 3
        }
        super.visitIntInsn(opcode, operand)
    }

    override fun visitLdcInsn(value: Any) {
        byteCodeOffset += if (value is Long ||
            value is Double ||
            value is ConstantDynamic && value.size == 2) {
            3
        } else {
            2
        }
        super.visitLdcInsn(value)
    }

    override fun visitLabel(label: Label) {
        println(byteCodeOffset)
        super.visitLabel(label)
    }


    override fun visitEnd() {
        super.visitEnd()
        val sourceInterpreter = AffectedVarInterpreter(throwIndex, instructions)
        val analyzer = AffectedVarAnalyser(sourceInterpreter, throwIndex, catchIndex)
        analyzer.analyze(owner, this)
    }
}

