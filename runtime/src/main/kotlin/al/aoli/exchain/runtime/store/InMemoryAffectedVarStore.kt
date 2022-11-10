package al.aoli.exchain.runtime.store

import al.aoli.exchain.runtime.objects.AffectedVarResult

class InMemoryAffectedVarStore: AffectedVarStore {

    val affectedVarResult = mutableMapOf<String, AffectedVarResult>()
    val exceptionSourceIdentified = mutableMapOf<Int, Boolean>()
    override fun getCachedAffectedVarResult(
        clazz: String,
        method: String,
        throwLocation: Long,
        catchLocation: Long,
        isThrowInsn: Boolean
    ): AffectedVarResult? {
        val sig = "$clazz:$method:$throwLocation:$catchLocation$isThrowInsn"
        return affectedVarResult[sig]
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
        affectedVarResult[sig] = result
    }


}