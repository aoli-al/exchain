package al.aoli.exchain.runtime.analyzers

import al.aoli.exchain.runtime.objects.ExceptionElement
import com.google.gson.Gson
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ExceptionLogger {
    fun logException(e: Throwable) {
        val elements = ArrayList<String>()
        for (stackTrace in e.stackTrace) {
            elements.add("${stackTrace.className}/${stackTrace.methodName}:${stackTrace.lineNumber}")
        }
        val item = Gson().toJson(ExceptionElement(e.javaClass.name, elements, e.message?.replace("\n", "")))
        exceptionLog.appendText(item + "\n")
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
        exceptionMap[e]!![4] += affectedVarResult.affectedFields.size

        if (shouldReport) {
            exceptionStats.appendText("${e.javaClass.name}, " +
                    exceptionMap[e]!!.joinToString(",") + "\n")
        }
    }

    val exceptionMap = mutableMapOf<Throwable, MutableList<Int>>()
    private val exceptionLog: File
    private val exceptionStats: File
    init {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
        val path = formatter.format(LocalDateTime.now())
        exceptionLog = File( path + ".log")
        exceptionStats = File(path + ".csv")
    }
}