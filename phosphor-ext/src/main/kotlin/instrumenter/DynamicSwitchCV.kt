package instrumenter

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes
import java.lang.reflect.Proxy

class DynamicSwitchCV(cv: ClassVisitor, val skipFrames: Boolean): ClassVisitor(Opcodes.ASM9, cv) {
    class MethodInfo(val access: Int, val name: String, val descriptor: String, val signature: String, val exceptions: Array<out String>)
    val methodList = mutableListOf<MethodInfo>()
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String,
        exceptions: Array<out String>
    ): MethodVisitor {
        val newName = when (name) {
            "<init>" -> "exchainConstructor"
            "<clinit>" -> "exchainStaticConstructor"
            else -> name
        }
        methodList.add(MethodInfo(access, name, descriptor, signature, exceptions))
        val mv1 = super.visitMethod(access, newName+Constants.originMethodSuffix, descriptor, signature, exceptions)
        val mv2 = super.visitMethod(access, newName+Constants.instrumentedMethodSuffix, descriptor, signature,
            exceptions)
        return Proxy.newProxyInstance(DynamicSwitchMV::class.java.classLoader,
            arrayOf(MethodVisitor::class.java),
            ReplayMV.InvokeProxy(emptyList())
        ) as MethodVisitor
    }

    override fun visitEnd() {
        super.visitEnd()


    }
}