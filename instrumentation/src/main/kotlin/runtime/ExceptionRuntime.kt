package al.aoli.exception.instrumentation.runtime

object ExceptionRuntime {

    @JvmStatic
    fun onException(e: Throwable) {
        println("from try")
        e.printStackTrace(System.out)
    }
}