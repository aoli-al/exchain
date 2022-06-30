package al.aoli.exception.instrumentation.runtime

import al.aoli.exception.instrumentation.analyzers.ExceptionTreeAnalyzer
import net.bytebuddy.asm.Advice

object ExceptionAdvices {
    @Advice.OnMethodExit(onThrowable = Throwable::class)
    @JvmStatic
    fun exit(@Advice.Thrown thrown: Throwable?, @Advice.Origin origin: String) {
        if (thrown != null) {
            ExceptionTreeAnalyzer.push(thrown, origin)
        }
    }
}