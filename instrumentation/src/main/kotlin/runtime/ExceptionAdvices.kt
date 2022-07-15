package al.aoli.exchain.instrumentation.runtime

import al.aoli.exchain.instrumentation.runtime.exceptions.ExceptionInjector
import al.aoli.exchain.instrumentation.server.ExceptionServiceImpl
import net.bytebuddy.asm.Advice
import net.bytebuddy.asm.Advice.Origin
import java.lang.reflect.Method
object ExceptionAdvices {


    @Advice.OnMethodEnter()
    @JvmStatic
    fun enter(@Advice.Origin("#t@#m@#s@#r") origin: String, @Origin method: Method) {
        if (!ExceptionServiceImpl.started) return
        ExceptionInjector.methodEnter(origin, method)
        ExceptionTreeAnalyzer.methodEnter(origin, method)
    }


    @Advice.OnMethodExit(onThrowable = Throwable::class)
    @JvmStatic
    fun exit(@Advice.Thrown e: Throwable?) {
        if (!ExceptionServiceImpl.started) return
        if (e != null) {
            ExceptionInjector.thrownExceptions(e)
        }
    }
}