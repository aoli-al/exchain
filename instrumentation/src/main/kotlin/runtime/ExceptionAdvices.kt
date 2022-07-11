package al.aoli.exception.instrumentation.runtime

import net.bytebuddy.asm.Advice
import net.bytebuddy.asm.Advice.Origin
import org.objectweb.asm.Opcodes.ASM8
import org.objectweb.asm.signature.SignatureVisitor
import java.lang.reflect.Method

class SimpleSignatureVisitor : SignatureVisitor(ASM8) {

}


object ExceptionAdvices {


    @Advice.OnMethodEnter()
    @JvmStatic
    fun enter(@Advice.Origin("#t@#m@#s@#r") origin: String, @Origin method: Method) {
        ExceptionTreeAnalyzer.methodEnter(origin, method)
    }
}