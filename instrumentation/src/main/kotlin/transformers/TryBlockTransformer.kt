package al.aoli.exchain.instrumentation.transformers

import al.aoli.exchain.instrumentation.runtime.ExceptionRuntime
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM8
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method

class TryBlockTransformer(private val owner: String, visitor: MethodVisitor, access: Int, methodName: String, descriptor: String):
    GeneratorAdapter(ASM8, visitor, access, methodName, descriptor) {
    private val endLabels = mutableMapOf<Label, Label>()
    private val visitedTryBlock = mutableSetOf<Pair<Label, Label>>()

    override fun visitTryCatchBlock(start: Label, end: Label, handler: Label, type: String?) {
        val range = Pair(start, end)
        if (type != null && handler !is InstrumentationLabel && range !in visitedTryBlock) {
            visitedTryBlock.add(range)
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
            push("$owner:$name")
            invokeStatic(Type.getType(ExceptionRuntime::class.java),
                Method("onException", Type.VOID_TYPE,
                    arrayOf(Type.getType(Throwable::class.java), Type.getType(String::class.java)))
            )
            loadLocal(id)
            throwException()
            super.visitLabel(label)
        } else {
            super.visitLabel(label)
        }
    }


}

