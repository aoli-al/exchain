package al.aoli.exchain.runtime.store

import al.aoli.exchain.runtime.analyzers.AffectedVarResult

interface AffectedVarStore {
    fun getCachedAffectedVarResult(clazz: String, method: String, throwLocation: Long, catchLocation: Long,
                                   isThrowInsn:Boolean): AffectedVarResult?
    fun putCachedAffectedVarResult(clazz: String, method: String, throwLocation: Long, catchLocation: Long,
                                   isThrowInsn: Boolean, result: AffectedVarResult)
}