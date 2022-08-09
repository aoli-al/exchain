package al.aoli.exchain.phosphor.instrumenter

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy


class ReplayMV(val mvs: List<MethodVisitor>): InvocationHandler {

    constructor(vararg mvs: MethodVisitor): this(mvs.asList())

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any? {
        val result = mvs.map { method.invoke(it, args) }
        if (method.returnType == AnnotationVisitor::class.java) {
            return Proxy.newProxyInstance(
                ReplayMV::class.java.classLoader,
                arrayOf(AnnotationVisitor::class.java),
                ReplayAV(result as List<AnnotationVisitor>)
            )
        }
        return null
    }
}