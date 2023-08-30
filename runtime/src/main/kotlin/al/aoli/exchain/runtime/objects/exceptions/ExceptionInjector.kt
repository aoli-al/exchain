package al.aoli.exchain.runtime.objects.exceptions

import java.io.File
import java.lang.reflect.Method
import kotlin.random.Random

object ExceptionInjector {
    //    private const val generatorThreshold = 1
    //    private const val generatorThreshold = 0.05
    private const val generatorThreshold = 0
    private var exceptionThrown = false
    val caughtExceptions = mutableSetOf<Throwable>()
    val caughtLog = File("/tmp/caught.txt")
    val injectionLog = File("/tmp/injection.txt")
    val random = Random(System.currentTimeMillis())

    init {
        caughtLog.writeText("")
        injectionLog.writeText("")
    }

    fun methodEnter(origin: String, method: Method) {
        if (exceptionThrown) return
        val generators =
            method.exceptionTypes
                .filter { it in Generators.generators }
                .map { Generators.generators[it]!! }
                .toList()

        if (generators.isEmpty()) return
        if (random.nextDouble() < generatorThreshold) {
            exceptionThrown = true

            val exception = generators.random(random).generate()
            injectionLog.writeText(
                "Exception injected: $exception at ${stackTraceToString(Thread.currentThread().stackTrace)}")
            throw exception
        }
    }

    fun stackTraceToString(e: Array<StackTraceElement>): String {
        return "<" + e.joinToString(",") + ">"
    }

    fun thrownExceptions(e: Throwable) {
        try {
            if (e in caughtExceptions) return
            caughtExceptions.add(e)
            caughtLog.appendText("$e thrown at ${stackTraceToString(e.stackTrace)}\n")
        } catch (thrown: Throwable) {
            println("WHAT!, $thrown")
            thrown.printStackTrace()
        }
    }
}
