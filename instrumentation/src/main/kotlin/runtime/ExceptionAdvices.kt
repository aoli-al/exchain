package al.aoli.exception.instrumentation.runtime

import al.aoli.exception.instrumentation.analyzers.ExceptionTreeAnalyzer
import net.bytebuddy.asm.Advice
import org.objectweb.asm.Opcodes.ASM8
import org.objectweb.asm.signature.SignatureVisitor
import java.io.File
import java.util.StringJoiner

class SimpleSignatureVisitor : SignatureVisitor(ASM8) {

}


object ExceptionAdvices {


    @Advice.OnMethodEnter()
    @JvmStatic
    fun enter(@Advice.Origin("#t@#m@#s@#r") origin: String) {
        ExceptionTreeAnalyzer.methodEnter(origin)
    }
}