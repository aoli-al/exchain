package al.aoli.exception.instrumentation.runtime

object ExceptionRuntime {

    @JvmStatic
    fun onException(e: Throwable, origin: String) {
        ExceptionTreeAnalyzer.push(e, origin)
    }

    @JvmStatic
    fun onCatch() {
        ExceptionTreeAnalyzer.catchEnd()
    }

    @JvmStatic
    fun onCatchBegin(e: Throwable) {
        ExceptionTreeAnalyzer.exceptionCaught(e)
    }

    @JvmStatic
    fun onCatchWithException(e: Throwable, origin: String) {
        ExceptionTreeAnalyzer.catchWithException(e, origin)
    }
}