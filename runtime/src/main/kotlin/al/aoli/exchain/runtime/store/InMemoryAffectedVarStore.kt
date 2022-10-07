package al.aoli.exchain.runtime.store

import al.aoli.exchain.runtime.analyzers.AffectedVarResult

class InMemoryAffectedVarStore: AffectedVarStore {

    val store = mutableMapOf<String, AffectedVarResult>()
    override fun getCachedAffectedVarResult(
        clazz: String,
        method: String,
        throwLocation: Long,
        catchLocation: Long,
        isThrowInsn: Boolean
    ): AffectedVarResult? {
        val sig = "$clazz:$method:$throwLocation:$catchLocation$isThrowInsn"
        return store[sig]
    }

    override fun putCachedAffectedVarResult(
        clazz: String,
        method: String,
        throwLocation: Long,
        catchLocation: Long,
        isThrowInsn: Boolean,
        result: AffectedVarResult
    ) {
        val sig = "$clazz:$method:$throwLocation:$catchLocation:$isThrowInsn"
        store[sig] = result
    }


}