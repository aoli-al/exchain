package al.aoli.exchain.analyzer

import soot.ArrayType
import soot.BooleanType
import soot.ByteType
import soot.CharType
import soot.DoubleType
import soot.ErroneousType
import soot.FloatType
import soot.IntType
import soot.LongType
import soot.RefType
import soot.ShortType
import soot.Type
import soot.tagkit.Host
import soot.tagkit.Tag

fun Host.addAll(result: Set<Tag>?) {
    if (result != null) {
        for (labelTag in result) {
            this.addTag(labelTag)
        }
    }
}

fun getParamTypes(descriptor: String): List<Type> {
    val types = mutableListOf<Type>()
    var currentOffset = 1
    var currentTypeBegin = 1
    while (descriptor[currentOffset] != ')') {
        while (descriptor[currentOffset] == '[') {
            currentOffset++
        }

        if (descriptor[currentOffset] == 'L') {
            currentOffset = descriptor.indexOf(';', currentOffset)
        }
        types.add(stringToType(descriptor.substring(currentTypeBegin..currentOffset)))
        currentOffset++
        currentTypeBegin = currentOffset
    }
    return types
}

fun stringToType(signature: String): Type {
    if (signature.startsWith("[")) {
        var count = 0
        for (c in signature) {
            if (c == '[') {
                count += 1
            } else {
                break
            }
        }
        return ArrayType.v(stringToType(signature.replace("[", "")), count)
    }
    if (signature.startsWith("L")) {
        return RefType.v(signature.substring(1, signature.length - 1).replace("/", "."))
    }
    return when (signature) {
        "B" -> ByteType.v()
        "C" -> CharType.v()
        "D" -> DoubleType.v()
        "F" -> FloatType.v()
        "I" -> IntType.v()
        "J" -> LongType.v()
        "S" -> ShortType.v()
        "Z" -> BooleanType.v()
        else -> ErroneousType.v()
    }
}
