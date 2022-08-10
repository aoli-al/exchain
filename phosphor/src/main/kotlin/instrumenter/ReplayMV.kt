package al.aoli.exchain.phosphor.instrumenter

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.*

class ReplayMV(val mvs: List<MethodVisitor>): MethodVisitor(Opcodes.ASM9) {
    constructor(vararg mvs: MethodVisitor): this(mvs.asList())

    override fun visitParameter(name: String?, access: Int) {
        for (mv in mvs) {
            mv.visitParameter(name, access)
        }
    }

    override fun visitAnnotationDefault(): AnnotationVisitor {
        return ReplayAV(mvs.map { it.visitAnnotationDefault() }.toList())
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
        return ReplayAV(mvs.map { it.visitAnnotation(descriptor, visible) }.toList())
    }

    override fun visitTypeAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor {
        return ReplayAV(mvs.map {
            it.visitTypeAnnotation(typeRef, typePath, descriptor, visible)
        }.toList())
    }

    override fun visitAnnotableParameterCount(parameterCount: Int, visible: Boolean) {
        for (mv in mvs) {
            mv.visitAnnotableParameterCount(parameterCount, visible)
        }
    }

    override fun visitParameterAnnotation(parameter: Int, descriptor: String?, visible: Boolean): AnnotationVisitor {
        return ReplayAV(mvs.map {
            it.visitParameterAnnotation(parameter, descriptor, visible)
        }.toList())
    }

    override fun visitAttribute(attribute: Attribute?) {
        for (mv in mvs) {
            mv.visitAttribute(attribute)
        }
    }

    override fun visitCode() {
        for (mv in mvs) {
            mv.visitCode()
        }
    }

    override fun visitFrame(type: Int, numLocal: Int, local: Array<out Any>?, numStack: Int, stack: Array<out Any>?) {
        for (mv in mvs) {
            mv.visitFrame(type, numLocal, local, numStack, stack)
        }
    }

    override fun visitInsn(opcode: Int) {
        for (mv in mvs) {
            mv.visitInsn(opcode)
        }
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        for (mv in mvs) {
            mv.visitIntInsn(opcode, operand)
        }
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        for (mv in mvs) {
            mv.visitVarInsn(opcode, `var`)
        }
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        for (mv in mvs) {
            mv.visitTypeInsn(opcode, type)
        }
    }

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        for (mv in mvs) {
            mv.visitFieldInsn(opcode, owner, name, descriptor)
        }
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        for (mv in mvs) {
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
    }

    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        for (mv in mvs) {
            mv.visitMethodInsn(opcode, owner, name, descriptor)
        }
    }

    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ) {
        for (mv in mvs) {
            mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
        }
    }

    override fun visitJumpInsn(opcode: Int, label: Label?) {
        for (mv in mvs) {
            mv.visitJumpInsn(opcode, label)
        }
    }

    override fun visitLabel(label: Label?) {
        for (mv in mvs) {
            mv.visitLabel(label)
        }
    }

    override fun visitLdcInsn(value: Any?) {
        for (mv in mvs) {
            mv.visitLdcInsn(value)
        }
    }


    override fun visitIincInsn(`var`: Int, increment: Int) {
        for (mv in mvs) {
            mv.visitIincInsn(`var`, increment)
        }
    }

    override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label, vararg labels: Label) {
        for (mv in mvs) {
            mv.visitTableSwitchInsn(min, max, dflt, *labels)
        }
    }

    override fun visitLookupSwitchInsn(dflt: Label?, keys: IntArray?, labels: Array<out Label>?) {
        for (mv in mvs) {
            mv.visitLookupSwitchInsn(dflt, keys, labels)
        }
    }

    override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
        for (mv in mvs) {
            mv.visitMultiANewArrayInsn(descriptor, numDimensions)
        }
    }

    override fun visitInsnAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor {
        return ReplayAV(mvs.map {
            it.visitInsnAnnotation(typeRef, typePath, descriptor, visible)
        }.toList())
    }

    override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
        for (mv in mvs) {
            mv.visitTryCatchBlock(start, end, handler, type)
        }
    }

    override fun visitTryCatchAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean): AnnotationVisitor {
        return ReplayAV(mvs.map {
            it.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible)
        }.toList())
    }

    override fun visitLocalVariable(
        name: String?,
        descriptor: String?,
        signature: String?,
        start: Label?,
        end: Label?,
        index: Int
    ) {
        for (mv in mvs) {
            mv.visitLocalVariable(name, descriptor, signature, start, end, index)
        }
    }

    override fun visitLocalVariableAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        start: Array<out Label>?,
        end: Array<out Label>?,
        index: IntArray?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor {
        return ReplayAV(mvs.map {
            it.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible)
        }.toList())
    }

    override fun visitLineNumber(line: Int, start: Label?) {
        for (mv in mvs) {
            mv.visitLineNumber(line, start)
        }
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        for (mv in mvs) {
            mv.visitMaxs(maxStack, maxLocals)
        }
    }

    override fun visitEnd() {
        for (mv in mvs) {
            mv.visitEnd()
        }
    }
}