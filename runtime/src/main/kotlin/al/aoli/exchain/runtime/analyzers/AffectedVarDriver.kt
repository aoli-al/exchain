package al.aoli.exchain.runtime.analyzers

import al.aoli.exchain.runtime.objects.AffectedVarResult
import al.aoli.exchain.runtime.store.InMemoryAffectedVarStore
import al.aoli.exchain.runtime.store.TransformedCodeStore
import edu.columbia.cs.psl.phosphor.runtime.Taint
import edu.columbia.cs.psl.phosphor.struct.PowerSetTree
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag
import mu.KotlinLogging
import java.io.IOException
import java.lang.Exception

private val logger = KotlinLogging.logger {}

object AffectedVarDriver {
    val store = InMemoryAffectedVarStore()
    fun analyzeAffectedVar(
        e: Throwable,
        clazz: String,
        method: String,
        throwIndex: Long,
        catchIndex: Long,
        isThrowInsn: Boolean
    ): AffectedVarResult? {
        val cached =
            store.getCachedAffectedVarResult(clazz, method, throwIndex, catchIndex, isThrowInsn)
        val label = ExceptionLogger.logException(e)
        if (cached != null) {
            cached.label = label
            ExceptionLogger.logAffectedVarResult(cached)
            return cached
        }
        val classPath = clazz.substring(1 until clazz.length - 1)
        val className = classPath.replace("/", ".")
        logger.info {
            "Start processing $className, method: $method, throwIndex: $throwIndex, catchIndex: $catchIndex"
        }
        val classReader =
            if (className in TransformedCodeStore.store) {
                AffectedVarClassReader(TransformedCodeStore.store[className]!!)
            } else {
                try {
                    AffectedVarClassReader(className)
                } catch (e1: IOException) {
                    try {
                        AffectedVarClassReader("BOOT-INF/classes/$classPath")
                    } catch (e2: IOException) {
                        logger.warn { "Cannot access bytecode for class $className:$method. Error: $e1, $e2" }
                        return null
                    }
                }
            }
        val sourceIdentified = store.exceptionSourceIdentified.getOrDefault(label, false)

        val visitor =
            AffectedVarClassVisitor(
                e,
                throwIndex,
                catchIndex,
                isThrowInsn,
                !sourceIdentified,
                className,
                method,
                classReader
            )
        classReader.accept(visitor, 0)

        // We are going to taint class fields here and local variables in native.
        val affectedFields =
            visitor.methodVisitor
                ?.affectedFields
                ?.filter { !it.second.contains("PHOSPHOR") }
                ?.toTypedArray()
                ?: emptyArray()
        val (affectedFieldLine, affectedFieldName) = affectedFields.unzip()
        val affectedVars = visitor.methodVisitor?.affectedVars?.toTypedArray() ?: emptyArray()
        val (affectedLocalLine, affectedLocalIndex, affectedLocalName) = affectedVars.unzip()
        val sourceLines = visitor.methodVisitor?.sourceLines?.toTypedArray() ?: emptyArray()
        if (sourceLines.isNotEmpty()) {
            store.exceptionSourceIdentified[label] = true
        }
        val sourceLocalVariable =
            visitor.methodVisitor?.sourceLocalVariable?.toIntArray() ?: intArrayOf()
        val sourceField = visitor.methodVisitor?.sourceField?.toTypedArray() ?: emptyArray()
        val result =
            AffectedVarResult(
                label,
                clazz,
                method,
                throwIndex,
                catchIndex,
                affectedLocalIndex.toIntArray(),
                affectedLocalName.toTypedArray(),
                affectedLocalLine.toIntArray(),
                affectedFieldName.toTypedArray(),
                affectedFieldLine.toIntArray(),
                sourceLines,
                sourceLocalVariable,
                sourceField
            )
        ExceptionLogger.logAffectedVarResult(result)
        store.putCachedAffectedVarResult(clazz, method, throwIndex, catchIndex, isThrowInsn, result)
        logger.info { "Affected analyzer result: $result" }
        return result
    }

    private val exceptionStore = mutableMapOf<Int, Any>()

    fun updateTaint(obj: PowerSetTree.SetNode, exception: Any): Taint<*> {
        val label = System.identityHashCode(exception)
        exceptionStore[label] = exception
        return obj.union(Taint.withLabel(label))
    }

    fun updateAffectedFields(obj: Any, affectedVarResult: AffectedVarResult, exception: Any) {
        val label = System.identityHashCode(exception)
        exceptionStore[label] = exception
        for (name in affectedVarResult.affectedFieldName) {
            try {
                val field = obj.javaClass.getDeclaredField(name + "PHOSPHOR_TAG")
                field.isAccessible = true
                val value = field.get(obj) as Taint<Int>?
                val tag =
                    if (value == null) {
                        Taint.withLabel(label)
                    } else {
                        value.union(Taint.withLabel(label))
                    }
                field.set(obj, tag)
            } catch (e: Exception) {
                logger.warn { "Cannot access field: $name for type: ${obj.javaClass.name}, " + "error: $e" }
            }
        }
    }

    fun taintObject(obj: TaintedWithObjTag, exception: Any) {
        val label = System.identityHashCode(exception)
        exceptionStore[label] = exception
        if (obj.phosphoR_TAG != null) {
            obj.phosphoR_TAG = (obj.phosphoR_TAG as Taint<Int>).union(Taint.withLabel(label))
        } else {
            obj.phosphoR_TAG = Taint.withLabel(label)
        }
    }

    fun analyzeSourceFields(
        obj: Any,
        affectedVarResult: AffectedVarResult,
        exception: Any,
        location: String
    ) {
        val origin = System.identityHashCode(exception)
        for (name in affectedVarResult.sourceField) {
            try {
                val field = obj.javaClass.getDeclaredField(name + "PHOSPHOR_TAG")
                field.isAccessible = true
                val taint = field.get(obj) as Taint<Int>? ?: continue
                for (label in taint.labels) {
                    if (label is Int && label in exceptionStore && label != origin) {
                        println(
                            "Exception ${exception.javaClass.name} thrown at $location possible caused by: ${exceptionStore[label]}"
                        )
                    }
                }
            } catch (e: Exception) {
                logger.warn { "Cannot access field: $name for type: ${obj.javaClass.name}, " + "error: $e" }
            }
        }
    }

    fun analyzeSourceVars(obj: Any, exception: Any, location: String) {
        logger.info { "Start processing source var: $obj at $location" }
        val origin = System.identityHashCode(exception)
        val taint =
            when (obj) {
                is Taint<*> -> obj
                is TaintedWithObjTag -> obj.phosphoR_TAG as Taint<*>?
                else -> null
            }
                ?: return
        for (label in taint.labels) {
            if (label is Int && label in exceptionStore && label != origin) {
                println(
                    "Exception ${exception.javaClass.name} thrown at $location possible caused by: ${exceptionStore[label]}"
                )
            }
        }
    }
}

private fun <A, B, C> Array<out Triple<A, B, C>>.unzip(): Triple<List<A>, List<B>, List<C>> {
    val listA = ArrayList<A>(size)
    val listB = ArrayList<B>(size)
    val listC = ArrayList<C>(size)
    for (pair in this) {
        listA.add(pair.first)
        listB.add(pair.second)
        listC.add(pair.third)
    }
    return Triple(listA, listB, listC)
}
