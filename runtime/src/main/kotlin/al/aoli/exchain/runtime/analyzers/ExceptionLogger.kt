package al.aoli.exchain.runtime.analyzers

import al.aoli.exchain.runtime.objects.AffectedVarResult
import al.aoli.exchain.runtime.objects.ExceptionElement
import al.aoli.exchain.runtime.objects.Type
import com.google.gson.Gson
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

object ExceptionLogger {
    val executor = Executors.newSingleThreadExecutor()

    fun logException(e: Throwable): Int {
        val label = System.identityHashCode(e)
        if (label in savedExceptions) {
            return label
        }
        savedExceptions.add(label)
        executor.submit {
            val elements = ArrayList<String>()
            for (stackTrace in e.stackTrace) {
                elements.add("${stackTrace.className}/${stackTrace.methodName}:${stackTrace.lineNumber}")
            }
            val item =
                Gson()
                    .toJson(
                        ExceptionElement(label, e.javaClass.name, elements, e.message?.replace("\n", ""))
                    )
            exceptionLog.appendText(item + "\n")
        }
        return label
    }

    fun stop() {
        executor.shutdown()
    }

    fun logDependency(e1: Int, e2: Int) {
        executor.submit { dynamicDependencyLog.appendText("$e1, $e2\n") }
    }

    fun logAffectedVarResult(result: AffectedVarResult) {
        executor.submit {
            val item = Gson().toJson(result)
            affectedVarResults.appendText(item + "\n")
        }
    }

    fun logStats(
        e: Throwable,
        affectedVarResult: AffectedVarResult,
        numOfObjects: Int,
        numOfArrays: Int,
        numOfPrimitives: Int,
        numOfNulls: Int,
        shouldReport: Boolean
    ) {
        if (e !in exceptionMap) {
            exceptionMap[e] = mutableListOf(0, 0, 0, 0, 0)
        }

        exceptionMap[e]!![0] += numOfObjects
        exceptionMap[e]!![1] += numOfArrays
        exceptionMap[e]!![2] += numOfPrimitives
        exceptionMap[e]!![3] += numOfNulls
        exceptionMap[e]!![4] += affectedVarResult.affectedFieldName.size

        if (shouldReport) {
            val label = System.identityHashCode(e)
            exceptionStats.appendText("$label, " + exceptionMap[e]!!.joinToString(",") + "\n")
        }
    }

    private val savedExceptions = mutableSetOf<Int>()
    private val exceptionMap = mutableMapOf<Throwable, MutableList<Int>>()
    private val exceptionLog: File
    private val exceptionStats: File
    private val dynamicDependencyLog: File
    private val affectedVarResults: File
    val outBasePath: String
    init {
        val basePath =
            when (AffectedVarDriver.type) {
                Type.Static -> "static-results"
                Type.Dynamic -> "dynamic-results"
                Type.Hybrid -> "hybrid-results"
            }
        val baseFolder = File(basePath)
        if (!baseFolder.isDirectory) {
            baseFolder.mkdir()
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
        val path = formatter.format(LocalDateTime.now())
        outBasePath = (System.getenv("EXCHAIN_OUT_DIR") ?: "/tmp/") + "/$basePath"
        val outPath = "$outBasePath/$path"
        val dataFolder = File(outPath)
        dataFolder.mkdirs()
        File("$outBasePath/latest").writeText(path)
        exceptionLog = File("$outPath/exception.json")
        exceptionStats = File("$outPath/stats.csv")
        affectedVarResults = File("$outPath/affected-var-results.json")
        dynamicDependencyLog = File("$outPath/dynamic_dependency.json")
    }
}
