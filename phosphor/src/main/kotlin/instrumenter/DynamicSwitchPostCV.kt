package al.aoli.exchain.phosphor.instrumenter

import al.aoli.exchain.phosphor.instrumenter.Constants
import al.aoli.exchain.phosphor.instrumenter.Constants.methodNameMapping
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.*
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_STATIC
import kotlin.math.sign

class DynamicSwitchPostCV(cv: ClassVisitor, val skipFrames: Boolean, val bytes: ByteArray): ClassVisitor(Opcodes.ASM9,
    cv) {
    private var owner = ""

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        owner = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (name.endsWith(Constants.originMethodSuffix) || name.endsWith("PHOSPHOR_TAG")) {
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
        val newName = methodNameMapping(name)

        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        addSwitch(mv, newName, descriptor, (access and ACC_STATIC) != 0)

        return super.visitMethod(access, newName + Constants.instrumentedMethodSuffix,
            descriptor, signature, exceptions)
    }

    private fun addBranch(mv: MethodVisitor, label: Label, name: String, descriptor: String, isStatic: Boolean) {
        mv.visitLabel(label)
        val (offset, insn) = if (!isStatic) {
            mv.visitVarInsn(Opcodes.ALOAD, 0) // load this
            Pair(1, Opcodes.INVOKESTATIC)
        } else Pair(0, Opcodes.INVOKEINTERFACE)
        val argumentTypes = Type.getArgumentTypes(descriptor)
        for (idx in argumentTypes.indices) {
            when (argumentTypes[idx]) {
                Type.BOOLEAN_TYPE, Type.BYTE_TYPE, Type.CHAR_TYPE, Type.INT_TYPE, Type.SHORT_TYPE ->
                    mv.visitVarInsn(Opcodes.ILOAD, idx + offset)
                Type.LONG_TYPE ->
                    mv.visitVarInsn(Opcodes.LLOAD, idx + offset)
                Type.FLOAT_TYPE ->
                    mv.visitVarInsn(Opcodes.FLOAD, idx + offset)
                Type.DOUBLE_TYPE ->
                    mv.visitVarInsn(Opcodes.DLOAD, idx + offset)
                else ->
                    mv.visitVarInsn(Opcodes.ALOAD, idx + offset)
            }
        }
        mv.visitMethodInsn(
            insn,
            owner,
            name,
            descriptor,
            true
        )
        mv.visitInsn(Opcodes.RETURN)
    }

    private fun addSwitch(mv: MethodVisitor, name: String, descriptor: String, isStatic: Boolean) {
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "al.aoli.exchain.instrumentation.runtime.ExceptionRuntime",
            "taintEnabled",
            "()Z",
            false
        )
        val trueLabel = Label()
        val falseLabel = Label()

        mv.visitJumpInsn(Opcodes.IFEQ, falseLabel)
        addBranch(mv, trueLabel, name + Constants.instrumentedMethodSuffix, descriptor, isStatic)
        addBranch(mv, falseLabel, name + Constants.originMethodSuffix, descriptor, isStatic)
    }
}