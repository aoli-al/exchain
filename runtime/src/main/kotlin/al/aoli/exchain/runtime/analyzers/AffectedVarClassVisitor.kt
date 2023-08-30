package al.aoli.exchain.runtime.analyzers

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class AffectedVarClassVisitor(
    private val exception: Throwable,
    private val throwIndex: Long,
    private val catchIndex: Long,
    private val isThrowInsn: Boolean,
    val findSource: Boolean,
    val owner: String,
    val method: String,
    val classReader: AffectedVarClassReader
) : ClassVisitor(Opcodes.ASM8) {
    var methodVisitor: AffectedVarMethodVisitor? = null
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        return if (name + descriptor == method) {
            methodVisitor =
                AffectedVarMethodVisitor(
                    exception,
                    throwIndex,
                    catchIndex,
                    isThrowInsn,
                    findSource,
                    owner,
                    access,
                    name,
                    descriptor,
                    signature,
                    exceptions,
                    classReader)
            methodVisitor
        } else {
            null
        }
    }
}
