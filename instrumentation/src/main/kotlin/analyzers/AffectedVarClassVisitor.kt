package al.aoli.exchain.instrumentation.analyzers

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class AffectedVarClassVisitor(private val throwIndex: Int, private val catchIndex: Int, val owner: String, val method: String): ClassVisitor(Opcodes.ASM8) {
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        return if (name + descriptor == method) {
            AffectedVarMethodVisitor(throwIndex, catchIndex, owner, access, name, descriptor, signature, exceptions)
        } else {
            null
        }
    }
}

