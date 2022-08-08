package al.aoli.exchain.instrumentation.store

import al.aoli.exchain.instrumentation.analyzers.AffectedVarResult

interface AffectedVarStore {
    fun getCachedAffectedVarResult(clazz: String, method: String, throwLocation: Long, catchLocation: Long): AffectedVarResult?
    fun putCachedAffectedVarResult(clazz: String, method: String, throwLocation: Long, catchLocation: Long, result: AffectedVarResult)
}