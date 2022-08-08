package al.aoli.exchain.instrumentation.store

import al.aoli.exchain.instrumentation.analyzers.AffectedVarResult

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