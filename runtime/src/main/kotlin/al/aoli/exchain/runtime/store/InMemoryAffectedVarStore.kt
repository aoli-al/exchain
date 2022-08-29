package al.aoli.exchain.runtime.store

import al.aoli.exchain.runtime.analyzers.AffectedVarResult

class InMemoryAffectedVarStore: AffectedVarStore {

    val store = mutableMapOf<String, AffectedVarResult>()
    override fun getCachedAffectedVarResult(
        clazz: String,
        method: String,
        throwLocation: Long,
        catchLocation: Long
    ): AffectedVarResult? {
        val sig = "$clazz:$method:$throwLocation:$catchLocation"
        return store[sig]
    }

    override fun putCachedAffectedVarResult(
        clazz: String,
        method: String,
        throwLocation: Long,
        catchLocation: Long,
        result: AffectedVarResult
    ) {
        val sig = "$clazz:$method:$throwLocation:$catchLocation"
        store[sig] = result
    }


}