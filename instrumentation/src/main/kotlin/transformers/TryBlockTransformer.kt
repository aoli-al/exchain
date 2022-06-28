package al.aoli.exception.instrumentation.transformers

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM8
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter

class TryBlockTransformer(visitor: MethodVisitor, access: Int, methodName: String, descriptor: String):
    GeneratorAdapter(ASM8, visitor, access, methodName, descriptor) {
    private val endLabels = mutableMapOf<Label, Label>()

    override fun visitTryCatchBlock(start: Label, end: Label, handler: Label, type: String?) {
        if (type != null) {
            val e = Label()
            endLabels[end] = e
            super.visitTryCatchBlock(start, e, e, "java/lang/Throwable")
        }
        super.visitTryCatchBlock(start, end, handler, type)
    }

    override fun visitLabel(label: Label) {
        if (label in endLabels) {
            goTo(label)
            mark(endLabels[label])
            val id = newLocal(Type.getType(Throwable::class.java))
            storeLocal(id)
            loadLocal(id)
            visitMethodInsn(
                INVOKESTATIC,
                "al/aoli/exception/instrumentation/runtime/ExceptionRuntime",
                "onException",
                "(Ljava/lang/Throwable;)V",
                false
            )
            loadLocal(id)
            throwException()
            super.visitLabel(label)
        } else {
            super.visitLabel(label)
        }
    }


}

