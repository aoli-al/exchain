package al.aoli.exchain.runtime.store

import al.aoli.exchain.runtime.objects.AffectedVarResult

class InMemoryAffectedVarStore : AffectedVarStore {

  val affectedVarResult = mutableMapOf<String, AffectedVarResult>()
  private var enabled = false

  init {
    if (System.getenv("EXCHAIN_ENABLED_CACHE") == "true") {
      enabled = true
    }
  }

  override fun getCachedAffectedVarResult(
      clazz: String,
      method: String,
      throwLocation: Long,
      catchLocation: Long,
      isThrowInsn: Boolean
  ): AffectedVarResult? {
    if (!enabled) {
      return null
    }
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
    if (!enabled) {
      return
    }
    val sig = "$clazz:$method:$throwLocation:$catchLocation:$isThrowInsn"
    affectedVarResult[sig] = result
  }
}
