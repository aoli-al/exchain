package al.aoli.exception.instrumentation.runtime

import al.aoli.exception.instrumentation.analyzers.ExceptionTreeAnalyzer

object ExceptionRuntime {

    @JvmStatic
    fun onException(e: Throwable) {
        ExceptionTreeAnalyzer.push(e)
    }

    @JvmStatic
    fun onCatch() {
        ExceptionTreeAnalyzer.pop()
    }

    @JvmStatic
    fun onCatchWithException(e: Throwable) {
        ExceptionTreeAnalyzer.pushThenPop(e)
        throw e
    }
}