package al.aoli.exception.instrumentation.runtime

object ExceptionRuntime {
    fun onException(e: Throwable) {
        println("123")
        e.printStackTrace()
    }
}