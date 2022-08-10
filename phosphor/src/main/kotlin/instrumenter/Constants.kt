package al.aoli.exchain.phosphor.instrumenter

object Constants {
    const val originMethodSuffix = "ExchainOrigin"
    const val instrumentedMethodSuffix = "ExchainInst"
    fun methodNameMapping(name: String): String {
        return when (name) {
            "<init>" -> "exchainConstructor"
            "<clinit>" -> "exchainStaticConstructor"
            else -> name
        }
    }
}