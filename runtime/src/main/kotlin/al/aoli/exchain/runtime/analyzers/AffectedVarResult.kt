package al.aoli.exchain.runtime.analyzers

enum class SourceType {
    JUMP,
    FIELD,
    INVOKE
}

class AffectedVarResult(var label: Int, val clazz: String, val method: String,
                        val affectedLocalIndex: IntArray,
                        val affectedLocalName: Array<String>,
                        val affectedLocalLine: IntArray,
                        val affectedFieldName: Array<String>,
                        val affectedFieldLine: IntArray,
                        val sourceLines: Array<Pair<Int, SourceType>>,
                        val sourceLocalIndex: IntArray = intArrayOf(),
                        val sourceLocalField: Array<String> = emptyArray()) {
    fun getSootClassName(): String {
        return clazz.substring(1, clazz.length - 1)
            .replace("/", ".")
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
            currentOffset ++
            currentTypeBegin = currentOffset
        }
        currentOffset += 1;
        val returnType = stringToType(method.substring(currentOffset))
        return "$returnType $methodName(${parameterTypes.joinToString(".")})"
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
}