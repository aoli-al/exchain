package al.aoli.exchain.analyzer

class Dependencies(val exceptionGraph: MutableMap<Int, MutableSet<Pair<Int, String>>> = mutableMapOf(),
                   val processed: MutableSet<String> = mutableSetOf()) {
}