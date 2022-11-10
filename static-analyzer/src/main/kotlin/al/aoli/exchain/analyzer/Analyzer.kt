package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.objects.AffectedVarResult
import mu.KotlinLogging
import soot.*
import soot.tagkit.Tag
import java.lang.RuntimeException


private val logger = KotlinLogging.logger {}
class Analyzer(val affectedVarResults: List<AffectedVarResult>) {
    val fieldTaints = mutableMapOf<String, MutableSet<Int>>()
    val exceptionGraph = mutableMapOf<Int, MutableSet<Int>>()
    val methodDependency = mutableMapOf<SootMethod, MutableSet<SootMethod>>()
    val methodParameterTaintMap = mutableMapOf<SootMethod, List<MutableSet<Tag>>>()
    val localMap = mutableMapOf<Local, MutableSet<Tag>>()
    val workList = mutableListOf<SootMethod>()

    fun addEdge(from: Int, to: Int) {
        if (from != to) {
            exceptionGraph.getOrPut(from) { mutableSetOf() }.add(to)
            println("New edge from: $from, to: $to")
        }
    }

    fun process() {
//        for (affectedVarResult in affectedVarResults) {
//            val className = affectedVarResult.clazz
//                .substring(1, affectedVarResult.clazz.length - 1)
//                .replace("/", ".")
//            if (affectedVarResult.sourceFields.isEmpty() && affectedVarResult.affectedFields.isEmpty()
//                && affectedVarResult.affectedVars.isEmpty() && affectedVarResult.sourceVars.isEmpty()) continue
//            Scene.v().forceResolve(className, SootClass.BODIES)
//            val clazz = Scene.v().getSootClass(className)
//            clazz.setApplicationClass()
//            Scene.v().loadNecessaryClasses()
//            val name = affectedVarResult.method.split("(")[0]
//            val paramTypes = getParamTypes("(" + affectedVarResult.method.split("(")[1])
//            val method = try {
//                clazz.getMethod(name, paramTypes)
//            } catch (e: AmbiguousMethodException) {
//                logger.warn("Failed to get method: $name", e)
//                continue
//            }
//            for (affectedField in affectedVarResult.affectedFields) {
//                val key = "$className.$affectedField"
//                fieldTaints
//                    .getOrPut(key) { mutableSetOf() }
//                    .add(affectedVarResult.label)
//            }
//            propagateAffectedVars(method, affectedVarResult)
//        }
    }

    fun propagateAffectedVars(method: SootMethod, affectedVarResult: AffectedVarResult) {
        workList.clear()
        workList.add(method)
        var result: AffectedVarResult? = affectedVarResult

        while (workList.isNotEmpty()) {
            val m = workList.removeFirst()
            val body = try {
                 m.retrieveActiveBody()
            } catch (e: RuntimeException) {
                logger.warn("Failed to retrieve active body of method: ${m.name} " +
                        "of class ${m.declaringClass.name}", e)
                continue
            }
            val analysis = PropagationStmtSwitch(this, result, m)
            for (unit in body.units) {
                unit.apply(analysis)
            }
            result = null
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
            currentOffset ++
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

    companion object {
    }

}