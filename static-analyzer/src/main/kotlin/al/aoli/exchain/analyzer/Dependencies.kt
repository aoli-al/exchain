package al.aoli.exchain.analyzer

class Dependencies(val exceptionGraph: MutableMap<Int, MutableSet<Int>> = mutableMapOf(),
                   val processed: MutableSet<String> = mutableSetOf()) {
}