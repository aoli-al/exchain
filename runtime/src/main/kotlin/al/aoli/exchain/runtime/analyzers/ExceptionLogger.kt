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
        outputFile.appendText(item + "\n")
    }

    private val outputFile: File
    init {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
        outputFile = File(formatter.format(LocalDateTime.now()) + ".log")
    }
}