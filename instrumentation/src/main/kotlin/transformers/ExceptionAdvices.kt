package al.aoli.exception.instrumentation.transformers

import net.bytebuddy.asm.Advice

object ExceptionAdvices {
    @Advice.OnMethodExit(onThrowable = Throwable::class)
    @JvmStatic
    fun exit(@Advice.Thrown thrown: Throwable?) {
    }
}