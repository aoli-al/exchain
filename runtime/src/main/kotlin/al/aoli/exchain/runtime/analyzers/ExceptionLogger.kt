package al.aoli.exchain.runtime.analyzers

import al.aoli.exchain.runtime.objects.ExceptionElement
import com.google.gson.Gson
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ExceptionLogger {
    fun logException(e: Throwable): Int {
        val label = System.identityHashCode(e)
        if (label in savedExceptions) {
            return label
        }
        savedExceptions.add(label)
        val elements = ArrayList<String>()
        for (stackTrace in e.stackTrace) {
            elements.add("${stackTrace.className}/${stackTrace.methodName}:${stackTrace.lineNumber}")
        }
        val item = Gson().toJson(ExceptionElement(label,
            e.javaClass.name, elements, e.message?.replace("\n", "")))
        exceptionLog.appendText(item + "\n")
        return label
    }

    fun logAffectedVarResult(result: AffectedVarResult) {
        val item = Gson().toJson(result)
        affectedVarResults.appendText(item + "\n")
    }

    fun logStats(e: Throwable, affectedVarResult: AffectedVarResult, numOfObjects: Int, numOfArrays: Int, numOfPrimitives: Int, numOfNulls: Int,
                         shouldReport: Boolean) {
        if (e !in exceptionMap) {
            exceptionMap[e] = mutableListOf(0, 0, 0, 0, 0)
        }

        exceptionMap[e]!![0] += numOfObjects
        exceptionMap[e]!![1] += numOfArrays
        exceptionMap[e]!![2] += numOfPrimitives
        exceptionMap[e]!![3] += numOfNulls
        exceptionMap[e]!![4] += affectedVarResult.affectedFieldName.size

        if (shouldReport) {
            exceptionStats.appendText("${e.javaClass.name}, " +
                    exceptionMap[e]!!.joinToString(",") + "\n")
        }
    }

    private val savedExceptions = mutableSetOf<Int>()
    private val exceptionMap = mutableMapOf<Throwable, MutableList<Int>>()
    private val exceptionLog: File
    private val exceptionStats: File
    private val affectedVarResults: File
    init {
        val basePath = "results"
        val baseFolder = File(basePath)
        if (!baseFolder.isDirectory) {
            baseFolder.mkdir()
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
        val path = formatter.format(LocalDateTime.now())
        File("$basePath/latest").writeText(path)
        val dataFolder = File("$basePath/$path")
        dataFolder.mkdir()

        exceptionLog = File("$basePath/$path/exception.json")
        exceptionStats = File("$basePath/$path/stats.csv")
        affectedVarResults = File("$basePath/$path/affected-var-results.json")
    }
}