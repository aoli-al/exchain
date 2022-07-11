package al.aoli.exception.instrumentation.runtime.exceptions

import java.lang.reflect.Method
import kotlin.random.Random

object ExceptionInjector {
    private const val generatorThreshold = 0.5

    fun methodEnter(origin: String, method: Method) {
        val generators = method.exceptionTypes
            .filter { it in Generators.generators }
            .map { Generators.generators[it]!! }
            .toList()

        if (generators.isEmpty()) return
        if (Random.nextDouble() < generatorThreshold) {
            throw generators.random().generate()
        }
    }
}