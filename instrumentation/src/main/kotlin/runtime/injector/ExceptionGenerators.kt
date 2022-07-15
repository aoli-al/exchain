package al.aoli.exchain.instrumentation.runtime.exceptions

import java.io.IOException

object Generators {
    val generators = mapOf(
        IOException::class.java to IOExceptionGenerator()
    )
}

abstract class Generator<E: Exception>(val type: Class<E>) {
    abstract fun generate(): E
}

class IOExceptionGenerator: Generator<IOException>(IOException::class.java) {
    override fun generate(): IOException {
        return IOException("generated IOException")
    }
}
