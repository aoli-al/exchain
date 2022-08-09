package al.aoli.exchain.phosphor.instrumenter

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.AnnotationVisitor
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class ReplayAV(val avs: List<AnnotationVisitor>): InvocationHandler {
    constructor(vararg avs: AnnotationVisitor): this(avs.asList())

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any? {
        avs.forEach { method.invoke(it, args) }
        return null
    }

}