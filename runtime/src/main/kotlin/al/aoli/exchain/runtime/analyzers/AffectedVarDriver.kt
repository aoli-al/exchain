package al.aoli.exchain.runtime.analyzers

import al.aoli.exchain.runtime.logger.Logger
import al.aoli.exchain.runtime.objects.AffectedVarResult
import al.aoli.exchain.runtime.objects.Type
import al.aoli.exchain.runtime.store.AffectedVarStore
import al.aoli.exchain.runtime.store.CachedAffectedVarStore
import al.aoli.exchain.runtime.store.InMemoryAffectedVarStore
import edu.columbia.cs.psl.phosphor.runtime.Taint
import edu.columbia.cs.psl.phosphor.struct.PowerSetTree
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag
import java.io.File
import java.io.IOException
import kotlin.Exception

private val logger = Logger()

object AffectedVarDriver {
    var instrumentedClassPath: String? = null
    var type = Type.Dynamic
    val store: AffectedVarStore by lazy {
        if (type == Type.Dynamic) {
            InMemoryAffectedVarStore()
        } else {
            CachedAffectedVarStore()
        }
    }
    val exceptionSourceIdentified = mutableMapOf<Int, Boolean>()
    fun analyzeAffectedVar(
        e: Throwable,
        clazz: String,
        method: String,
        throwIndex: Long,
        catchIndex: Long,
        isThrowInsn: Boolean
    ): AffectedVarResult? {
        val label = ExceptionLogger.logException(e)
        if (type == Type.Static) {
            val result = AffectedVarResult(
                label,
                e.javaClass.name,
                clazz,
                method,
                throwIndex,
                catchIndex,
                isThrowInsn
            )
            ExceptionLogger.logAffectedVarResult(result)
            return null
        }
        val cached =
            store.getCachedAffectedVarResult(clazz, method, throwIndex, catchIndex, isThrowInsn)
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
            try {
                AffectedVarClassReader(className)
            } catch (e1: IOException) {
                try {
                    val path = instrumentedClassPath ?: return null
                    AffectedVarClassReader(File("$path/$classPath.class").readBytes())
                } catch (e2: Throwable) {
                    logger.error {
                        "Failed to get class file $className"
                    }
                    return null
                }
            }
        val sourceIdentified = exceptionSourceIdentified.getOrDefault(label, false)

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
        val sourceLocalVariable =
            visitor.methodVisitor?.sourceLocalVariable?.toIntArray() ?: intArrayOf()
        if (method.contains("clinit")) {
            visitor.methodVisitor?.affectedStaticField?.clear()
            visitor.methodVisitor?.sourceStaticField?.clear()
        }

        val (affectedLocalLine, affectedLocalIndex, affectedLocalName) = affectedVars.unzip()
        val (affectedStaticFieldLine, affectedStaticFieldName) =
            (visitor.methodVisitor?.affectedStaticField?.toTypedArray() ?: emptyArray()).unzip()
        val sourceLines = visitor.methodVisitor?.sourceLines?.toTypedArray() ?: emptyArray()
        if (sourceLines.isNotEmpty()) {
            exceptionSourceIdentified[label] = true
        }
        val sourceField = visitor.methodVisitor?.sourceField?.toTypedArray() ?: emptyArray()
        val sourceStaticField = visitor.methodVisitor?.sourceStaticField?.toTypedArray() ?: emptyArray()
        val result =
            AffectedVarResult(
                label,
                e.javaClass.name,
                clazz,
                method,
                throwIndex,
                catchIndex,
                isThrowInsn,
                if (type == Type.Hybrid) { intArrayOf() } else { affectedLocalIndex.toIntArray() },
                affectedLocalName.toTypedArray(),
                affectedLocalLine.toIntArray(),
                affectedFieldName.toTypedArray(),
                affectedFieldLine.toIntArray(),
                affectedStaticFieldName.toTypedArray(),
                affectedStaticFieldLine.toIntArray(),
                sourceLines,
                if (type == Type.Hybrid) { intArrayOf() } else { sourceLocalVariable },
                sourceField,
                sourceStaticField
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

    fun updateAffectedFields(obj: Any?, affectedVarResult: AffectedVarResult, exception: Any) {
        val label = System.identityHashCode(exception)
        exceptionStore[label] = exception
        if (obj != null) {
            for (name in affectedVarResult.affectedFieldName) {
                try {
                    val field = obj.javaClass.getField(name + "PHOSPHOR_TAG")
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

        for (name in affectedVarResult.affectedStaticFieldName) {
            try {
                val (clazzName, fieldName) = name.split("#")
                val clazz = Class.forName(clazzName.replace("/", "."))
                val field = clazz.getField(fieldName + "PHOSPHOR_TAG")
                val value = field.get(null) as Taint<Int>?
                val tag = if (value == null) {
                    Taint.withLabel(label)
                } else {
                    value.union(Taint.withLabel(label))
                }
                field.set(null, tag)
            } catch (e: Exception) {
                logger.warn { "Cannot access static field: $name, error: $e" }
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
        obj: Any?,
        affectedVarResult: AffectedVarResult,
        exception: Any,
        location: String
    ) {
        val origin = System.identityHashCode(exception)

        if (obj != null && affectedVarResult.sourceField.isNotEmpty()) {
            try {
                val field = obj.javaClass.getDeclaredField( "PHOSPHOR_TAG")
                field.isAccessible = true
                val taint = field.get(obj) as Taint<*>?
                if (taint != null) {
                    for (label in taint.labels) {
                        if (label is Int && label in exceptionStore && label != origin) {
                            ExceptionLogger.logDependency(label, origin)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn { "Cannot access PHOSPHOR_TAG for type: ${obj.javaClass.name}, " + "error: $e" }
            }
        }

        if (obj != null) {
            for (name in affectedVarResult.sourceField) {
                try {
                    val field = obj.javaClass.getDeclaredField(name + "PHOSPHOR_TAG")
                    field.isAccessible = true
                    val taint = field.get(obj) as Taint<Int>? ?: continue
                    for (label in taint.labels) {
                        if (label is Int && label in exceptionStore && label != origin) {
                            ExceptionLogger.logDependency(label, origin)
                        }
                    }
                } catch (e: Exception) {
                    logger.warn { "Cannot access field: ${name}PHOSRPHOR_TAG for type: ${obj.javaClass.name}, " +
                            "error:$e" }
                }
            }
        }

        for (name in affectedVarResult.sourceStaticField) {
            try {
                val (clazzName, fieldName) = name.split("#")
                val clazz = Class.forName(clazzName.replace("/", "."))
                val field = clazz.getField(fieldName + "PHOSPHOR_TAG")
                val taint = field.get(obj) as Taint<Int>? ?: continue
                for (label in taint.labels) {
                    if (label is Int && label in exceptionStore && label != origin) {
                        ExceptionLogger.logDependency(label, origin)
                    }
                }
            } catch (e: Exception) {
                logger.warn { "Cannot access static field: $name, error: $e" }
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
                ExceptionLogger.logDependency(label, origin)
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
