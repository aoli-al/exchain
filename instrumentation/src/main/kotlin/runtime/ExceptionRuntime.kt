package al.aoli.exception.instrumentation.runtime

import al.aoli.exception.instrumentation.runtime.exceptions.ExceptionInjector
import al.aoli.exception.instrumentation.server.ExceptionServiceImpl
import java.lang.reflect.Method

object ExceptionRuntime {

    @JvmStatic
    fun onException(e: Throwable, origin: String) {
        if (!ExceptionServiceImpl.started) return
        ExceptionInjector.thrownExceptions(e)
//        ExceptionTreeAnalyzer.push(e, origin)
    }

    @JvmStatic
    fun onCatch() {
        if (!ExceptionServiceImpl.started) return
//        ExceptionTreeAnalyzer.catchEnd()
    }

    @JvmStatic
    fun onCatchBegin(e: Throwable) {
        if (!ExceptionServiceImpl.started) return
        ExceptionInjector.thrownExceptions(e)
//        ExceptionTreeAnalyzer.exceptionCaught(e)
    }

    @JvmStatic
    fun onCatchWithException(e: Throwable, origin: String) {
        if (!ExceptionServiceImpl.started) return
        ExceptionInjector.thrownExceptions(e)
//        ExceptionTreeAnalyzer.catchWithException(e, origin)
    }

    @JvmStatic
    fun onExceptionStackInfo(method: Array<Method>, loc: Array<Int>) {
    }
}