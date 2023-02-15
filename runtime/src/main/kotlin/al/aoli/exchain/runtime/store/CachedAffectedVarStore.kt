package al.aoli.exchain.runtime.store

import al.aoli.exchain.runtime.analyzers.ExceptionLogger
import al.aoli.exchain.runtime.objects.AffectedVarResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

class CachedAffectedVarStore : AffectedVarStore {

    val executor = Executors.newSingleThreadExecutor()

    val affectedVarResult: MutableMap<String, AffectedVarResult>
    var storeType = object : TypeToken<MutableMap<String, AffectedVarResult>>() {}.type
    val storeFileName = ExceptionLogger.outBasePath + "/cached_affected_var_store.json"

    init {
        affectedVarResult =
            try {
                val f = File(storeFileName)
                if (f.isFile) {
                    Gson().fromJson(f.readText(), storeType)
                } else {
                    mutableMapOf()
                }
            } catch (e: IOException) {
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
        val sig = "$clazz:$method:$throwLocation:$catchLocation:$isThrowInsn"
        affectedVarResult[sig] = result
        executor.submit {
            File(storeFileName).writeText(Gson().toJson(affectedVarResult))
        }
    }
}
