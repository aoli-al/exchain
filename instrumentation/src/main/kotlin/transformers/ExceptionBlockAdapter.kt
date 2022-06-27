package al.aoli.exception.instrumentation.transformers

import net.bytebuddy.asm.AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.pool.TypePool
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter

class TryCatchBlock(val start: Label, val end: Label, val handler: Label, val signature: String)

class ExceptionBlockAdapter(visitor: MethodVisitor, access: Int, methodName: String, descriptor: String):
    AdviceAdapter(ASM8, visitor, access, methodName, descriptor) {
    private val startLabels = mutableMapOf<Label, Label>()
    private val endLabels = mutableMapOf<Label, Label>()
    private val handlerLabels = mutableMapOf<Label, Label>()

    override fun visitTryCatchBlock(start: Label, end: Label, handler: Label, type: String) {
        super.visitTryCatchBlock(start, end, handler, type)
        val s = Label()
        val e = Label()
        val h = Label()
        startLabels[start] = s
        endLabels[end] = e
        handlerLabels[end] = h
        super.visitTryCatchBlock(s, e, h, "java/lang/Throwable")
    }

    override fun visitLabel(label: Label) {
        if (label in startLabels) {
            super.visitLabel(label)
            super.visitLabel(startLabels[label])
        } else if (label in endLabels) {
            val end = Label()
            goTo(end)
            mark(endLabels[label])
            mark(handlerLabels[label])
            visitMethodInsn(
                INVOKESTATIC,
                "al/aoli/exception/instrumentation/runtime/ExceptionRuntime",
                "onException",
                "(Ljava/lang/Throwable;)V",
                false
            )
            mark(end)
            super.visitLabel(label)
        } else {
            super.visitLabel(label)
        }
    }
}

class ExceptionBlockTransformer: MethodVisitorWrapper {
    override fun wrap(
        instrumentedType: TypeDescription,
        instrumentedMethod: MethodDescription,
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context,
        typePool: TypePool,
        writerFlags: Int,
        readerFlags: Int
    ): MethodVisitor {
        return ExceptionBlockAdapter(methodVisitor, instrumentedMethod.modifiers, instrumentedMethod.name,
            instrumentedMethod.descriptor)
    }
}