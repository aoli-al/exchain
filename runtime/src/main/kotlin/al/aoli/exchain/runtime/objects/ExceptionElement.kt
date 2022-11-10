package al.aoli.exchain.runtime.objects

import java.sql.Timestamp

data class ExceptionElement(val label: Int, val type: String, val stack: List<String>, val message: String?) {
    val time: Timestamp = Timestamp(System.currentTimeMillis())
}