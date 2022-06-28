package al.aoli.exception.instrumentation.runtime

import net.bytebuddy.asm.Advice

object ExceptionAdvices {
    @Advice.OnMethodExit(onThrowable = Throwable::class)
    @JvmStatic
    fun exit(@Advice.Thrown thrown: Throwable?) {
        if (thrown != null) {
            println("from function")
            thrown.printStackTrace(System.out)
        }
    }
}