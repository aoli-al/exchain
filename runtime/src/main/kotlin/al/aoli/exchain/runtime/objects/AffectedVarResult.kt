package al.aoli.exchain.runtime.objects

import com.google.gson.GsonBuilder

enum class SourceType {
  JUMP,
  FIELD,
  ARRAY,
  INVOKE
}

data class AffectedVarResult(
    var label: Int,
    val exceptionType: String,
    val clazz: String,
    val method: String,
    val throwIndex: Long,
    val catchIndex: Long,
    val isThrownInsn: Boolean,
    val affectedLocalIndex: IntArray = intArrayOf(),
    val affectedLocalName: Array<String> = emptyArray(),
    val affectedLocalLine: IntArray = intArrayOf(),
    val affectedFieldName: Array<String> = emptyArray(),
    val affectedFieldLine: IntArray = intArrayOf(),
    val affectedStaticFieldName: Array<String> = emptyArray(),
    val affectedStaticFieldLine: IntArray = intArrayOf(),
    val sourceLines: Array<Pair<Int, SourceType>> = emptyArray(),
    val sourceLocalVariable: IntArray = intArrayOf(),
    val sourceField: Array<String> = emptyArray(),
    val sourceStaticField: Array<String> = emptyArray()
) {
  override fun toString(): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(this)
  }
  fun getSootClassName(): String {
    return clazz.substring(1, clazz.length - 1).replace("/", ".")
  }

  fun getSignature(): String {
    return "$label:${getSootMethodSignature()}"
  }

  fun getSootMethodSignature(): String {
    return "<${getSootClassName()}: ${getSootMethodSubsignature()}>"
  }

  fun getSootMethodSubsignature(): String {
    val parameterTypes = mutableListOf<String>()
    var currentOffset = method.indexOf("(") + 1
    var currentTypeBegin = currentOffset
    val methodName = method.substring(0 until currentOffset - 1)
    while (method[currentOffset] != ')') {
      while (method[currentOffset] == '[') {
        currentOffset++
      }

      if (method[currentOffset] == 'L') {
        currentOffset = method.indexOf(';', currentOffset)
      }
      parameterTypes.add(stringToType(method.substring(currentTypeBegin..currentOffset)))
      currentOffset++
      currentTypeBegin = currentOffset
    }
    currentOffset += 1
    val returnType = stringToType(method.substring(currentOffset))
    return "$returnType $methodName(${parameterTypes.joinToString(",")})"
  }

  fun stringToType(signature: String): String {
    if (signature.startsWith("[")) {
      var count = 0
      for (c in signature) {
        if (c == '[') {
          count += 1
        } else {
          break
        }
      }
      return stringToType(signature.replace("[", "")) + "[]".repeat(count)
    }
    if (signature.startsWith("L")) {
      return signature.substring(1, signature.length - 1).replace("/", ".")
    }
    return when (signature) {
      "B" -> "byte"
      "C" -> "char"
      "D" -> "double"
      "F" -> "float"
      "I" -> "int"
      "J" -> "long"
      "S" -> "short"
      "Z" -> "boolean"
      "V" -> "void"
      else -> "error"
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AffectedVarResult

    if (label != other.label) return false
    if (clazz != other.clazz) return false
    if (method != other.method) return false

    return true
  }

  override fun hashCode(): Int {
    var result = label
    result = 31 * result + clazz.hashCode()
    result = 31 * result + method.hashCode()
    return result
  }
}
