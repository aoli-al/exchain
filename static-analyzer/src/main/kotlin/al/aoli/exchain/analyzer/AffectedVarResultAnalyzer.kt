package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.analyzers.AffectedVarResult
import soot.*


class AffectedVarResultAnalyzer(val affectedVarResults: List<AffectedVarResult>) {
    val fieldTaints = mutableMapOf<String, MutableSet<Int>>()
    val exceptionGraph = mutableMapOf<Int, MutableSet<Int>>()

    fun addEdge(from: Int, to: Int) {
        if (from != to) {
            exceptionGraph.getOrPut(from) { mutableSetOf() }.add(to)
            println("New edge from: $from, to: $to")
        }
    }

    fun process() {
        for (affectedVarResult in affectedVarResults) {
            val className = affectedVarResult.clazz
                .substring(1, affectedVarResult.clazz.length - 1)
                .replace("/", ".")
            val clazz = Scene.v().getSootClass(className)
            val name = affectedVarResult.method.split("(")[0]
            val paramTypes = getParamTypes(affectedVarResult.method)
            val method = clazz.getMethod(name, paramTypes)
            for (affectedField in affectedVarResult.affectedFields) {
                val key = "$className.$affectedField"
                fieldTaints
                    .getOrPut(key) { mutableSetOf() }
                    .add(affectedVarResult.label)
            }
            propagateAffectedVars(method, affectedVarResult)
        }
    }

    fun propagateAffectedVars(method: SootMethod, affectedVarResult: AffectedVarResult) {
        val body = method.retrieveActiveBody()
        val analysis = PropagationStmtSwitch(this, affectedVarResult, method)
        for (unit in body.units) {
            unit.apply(analysis)
        }
    }

    fun getParamTypes(signature: String): List<Type> {
        val params = signature
            .split("(")[1]
            .split(")")[0]
        return if (params.isEmpty()) {
            emptyList()
        } else {
            params.split(",").map(this::stringToType)
        }
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

    companion object {
    }

}