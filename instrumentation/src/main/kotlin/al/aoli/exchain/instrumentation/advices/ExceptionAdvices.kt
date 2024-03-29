package al.aoli.exchain.instrumentation.advices

import al.aoli.exchain.runtime.ExceptionTreeAnalyzer
import al.aoli.exchain.runtime.objects.exceptions.ExceptionInjector
import al.aoli.exchain.runtime.server.ExceptionServiceImpl
import java.lang.reflect.Method
import net.bytebuddy.asm.Advice
import net.bytebuddy.asm.Advice.Origin

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
