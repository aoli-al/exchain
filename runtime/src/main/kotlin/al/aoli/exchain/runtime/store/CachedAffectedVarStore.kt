package al.aoli.exchain.runtime.store

import al.aoli.exchain.runtime.analyzers.ExceptionLogger
import al.aoli.exchain.runtime.objects.AffectedVarResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.Executors

object CachedAffectedVarStore : AffectedVarStore {

    val executor = Executors.newSingleThreadExecutor()

    val affectedVarResult: MutableMap<String, AffectedVarResult>
    var storeType = object : TypeToken<MutableMap<String, AffectedVarResult>>() {}.type
    val storeFileName = ExceptionLogger.outBasePath + "/cached_affected_var_store.json"
    private var enabled = false

    init {
        if (System.getenv("EXCHAIN_ENABLE_CACHE") == "true") {
            enabled = true
        }
        affectedVarResult =
            try {
                val f = File(storeFileName)
                if (f.isFile) {
                    val text = f.readText()
                    println(text)
                    Gson().fromJson(text, storeType)
                } else {
                    mutableMapOf()
                }
            } catch (e: Exception) {
                mutableMapOf()
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
        val sig = "$clazz:$method:$throwLocation:$catchLocation:$isThrowInsn"
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
        executor.submit { File(storeFileName).writeText(Gson().toJson(affectedVarResult)) }
    }
}
