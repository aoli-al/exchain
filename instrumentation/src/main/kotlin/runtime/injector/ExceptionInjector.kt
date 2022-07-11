package al.aoli.exception.instrumentation.runtime.exceptions

import al.aoli.exception.instrumentation.server.ExceptionServiceImpl
import java.io.File
import java.lang.reflect.Method
import kotlin.random.Random

object ExceptionInjector {
    private const val generatorThreshold = 0.0
    private var exceptionThrown = false
    val caughtExceptions = mutableSetOf<Throwable>()
    val output = File("/tmp/caught.txt")

    init {
        output.writeText("")
    }

    fun methodEnter(origin: String, method: Method) {
        try {
            if (exceptionThrown) return
            val generators = method.exceptionTypes
                .filter { it in Generators.generators }
                .map { Generators.generators[it]!! }
                .toList()

            if (generators.isEmpty()) return
            if (Random.nextDouble() < generatorThreshold) {
                exceptionThrown = true
                throw generators.random().generate()
            }
        } catch (thrown: Throwable) {
            println("WHAT!, $thrown")
            thrown.printStackTrace()
        }
    }

    fun thrownExceptions(e: Throwable) {
        try {
            if (e in caughtExceptions) return
            caughtExceptions.add(e)
            output.appendText("$e at ${e.stackTrace.firstOrNull()?.toString()}\n")
        } catch (thrown: Throwable) {
            println("WHAT!, $thrown")
            thrown.printStackTrace()
        }
    }
}