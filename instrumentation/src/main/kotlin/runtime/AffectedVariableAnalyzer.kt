package al.aoli.exception.instrumentation.runtime

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM8
import org.objectweb.asm.Opcodes.PUTFIELD
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.SourceInterpreter

class AffectedVarMethodVisitor(val owner: String, access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?):
    MethodNode(ASM8, access, name, descriptor, signature, exceptions) {


    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        println(instructions.size())
        super.visitFieldInsn(opcode, owner, name, descriptor)
    }


    override fun visitEnd() {
        super.visitEnd()
        val sourceInterpreter = SourceInterpreter()
        val analyzer = Analyzer(sourceInterpreter)
        analyzer.analyze(owner, this)
    }
}

class AffectedVarClassVisitor(val owner: String): ClassVisitor(ASM8) {
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        return AffectedVarMethodVisitor(owner, access, name, descriptor, signature, exceptions)
    }
}

object AffectedVariableAnalyzer {
    fun analyzeAffectedVar(clazz: Class<out Any>) {
        val classReader = ClassReader(clazz.name)
        classReader.accept(AffectedVarClassVisitor(clazz.name), ClassReader.EXPAND_FRAMES)
    }
}