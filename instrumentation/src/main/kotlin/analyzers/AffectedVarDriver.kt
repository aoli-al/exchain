package al.aoli.exchain.instrumentation.analyzers

import al.aoli.exchain.instrumentation.store.InMemoryAffectedVarStore
import al.aoli.exchain.instrumentation.store.TransformedCodeStore
import com.github.ajalt.mordant.rendering.TextColors
import edu.columbia.cs.psl.phosphor.runtime.Taint
import edu.columbia.cs.psl.phosphor.struct.PowerSetTree.SetNode
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag
import mu.KotlinLogging
import org.objectweb.asm.*
import java.io.IOException

private val logger = KotlinLogging.logger {}
object AffectedVarDriver {
    val store = InMemoryAffectedVarStore()
    var taintEnabled: Boolean = false
    fun analyzeAffectedVar(clazz: String, method: String, throwIndex: Long, catchIndex: Long, isThrowInsn: Boolean) : AffectedVarResult? {
        if (method.contains("configureClient")) {
            println("?")
        }
        val cached = store.getCachedAffectedVarResult(clazz, method, throwIndex, catchIndex)
        if (cached != null) {
            return cached
        }
        val classPath = clazz.substring(1 until clazz.length-1)
        val className = classPath.replace("/", ".")
        logger.info { "Start processing ${className}, method: $method, throwIndex: $throwIndex, catchIndex: $catchIndex" }
        val classReader = if (className in TransformedCodeStore.store) {
            ClassReader(TransformedCodeStore.store[className])
        } else {
            try {
                ClassReader(className)
            } catch (e1: IOException) {
                try {
                    ClassReader("BOOT-INF/classes/$classPath")
                }
                catch (e2: IOException) {
                    logger.warn { "Cannot access bytecode for class $className:$method. Error: $e1, $e2" }
                    return null;
                }
            }

        }
        val visitor = AffectedVarClassVisitor(throwIndex, catchIndex, isThrowInsn, className, method)
        classReader.accept(visitor, 0)
        // We are going to taint class fields here and local variables in native.
        val affectedFields = visitor.methodVisitor?.affectedFields?.toTypedArray() ?: emptyArray()
        val affectedVars = visitor.methodVisitor?.affectedVars?.toIntArray() ?: intArrayOf()
        val sourceVars = visitor.methodVisitor?.sourceVars?.toIntArray() ?: intArrayOf()
        val affectedParams = visitor.methodVisitor?.affectedParams?.toIntArray() ?: intArrayOf()
        val result = AffectedVarResult(affectedVars, affectedFields, sourceVars)
        store.putCachedAffectedVarResult(clazz, method, throwIndex, catchIndex, result)
        return result
    }

    private val exceptionStore = mutableMapOf<Int, Any>()

    fun updateTaint(obj: SetNode, exception: Any): Taint<*> {
        val label = System.identityHashCode(exception)
        exceptionStore[label] = exception
        return obj.union(Taint.withLabel(label))
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



    fun analyzeSource(obj: Any, exception: Any, location: String) {
        val taint = when(obj) {
            is Taint<*> -> obj
            is TaintedWithObjTag -> obj.phosphoR_TAG as Taint<*>?
            else -> null
        } ?: return
        for (label in taint.labels) {
            if (label is Int && label in exceptionStore) {
                println(TextColors.cyan("Exception ${exception.javaClass.name} thrown at $location possible caused by: ${exceptionStore[label]}"))
            }
        }
    }
}