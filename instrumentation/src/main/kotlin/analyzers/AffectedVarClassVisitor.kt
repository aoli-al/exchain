package al.aoli.exchain.instrumentation.analyzers

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class AffectedVarClassVisitor(private val throwIndex: Int, private val catchIndex: Int, val owner: String, val method: String): ClassVisitor(Opcodes.ASM8) {
    var methodVisitor: AffectedVarMethodVisitor? = null
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        return if (name + descriptor == method) {
            methodVisitor = AffectedVarMethodVisitor(throwIndex, catchIndex, owner, access, name, descriptor, signature, exceptions)
            methodVisitor
        } else {
            null
        }
    }
}

