package al.aoli.exchain.instrumentation.analyzers

import al.aoli.exchain.instrumentation.store.TransformedCodeStore
import com.github.ajalt.mordant.rendering.TextColors
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame
import edu.columbia.cs.psl.phosphor.runtime.Taint
import edu.columbia.cs.psl.phosphor.struct.PowerSetTree.SetNode
import edu.columbia.cs.psl.phosphor.struct.Tainted
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag
import mu.KotlinLogging
import org.objectweb.asm.*

private val logger = KotlinLogging.logger {}
object AffectedVarDriver {
    fun analyzeAffectedVar(clazz: String, method: String, throwIndex: Long, catchIndex: Long, isThrowInsn: Boolean) : AffectedVarResults {
        val className = clazz.replace("/", ".").substring(1 until clazz.length-1)
        logger.info { "Start processing ${className}, method: $method, throwIndex: $throwIndex, catchIndex: $catchIndex" }
        val classReader = if (className in TransformedCodeStore.store) {
            ClassReader(TransformedCodeStore.store[className])
        } else {
            ClassReader(className)
        }
        val visitor = AffectedVarClassVisitor(throwIndex, catchIndex, isThrowInsn, className, method)
        classReader.accept(visitor, 0)
        // We are going to taint class fields here and local variables in native.
        val affectedFields = visitor.methodVisitor?.affectedFields?.toTypedArray() ?: emptyArray()
        val affectedVars = visitor.methodVisitor?.affectedVars?.toIntArray() ?: intArrayOf()
        val sourceVars = visitor.methodVisitor?.sourceVars?.toIntArray() ?: intArrayOf()
        val a = (AffectedVarResults(affectedVars, affectedFields, sourceVars))
        println(a.affectedVars)
        println(a.affectedFields)
        println(a.sourceVars)
        return a
    }

    private val exceptionStore = mutableMapOf<Int, Any>()

    fun taintAffectedVar(obj: SetNode, idx: Int, thread: Thread, depth: Int, exception: Any): Taint<*> {
        val label = System.identityHashCode(exception)
        exceptionStore[label] = exception
        return obj.union(Taint.withLabel(label))
    }

    fun analyzeSource(obj: Taint<*>, exception: Any, location: String) {
        for (label in obj.labels) {
            if (label is Int && label in exceptionStore) {
                println(TextColors.cyan("Exception ${exception.javaClass.name} thrown at $location possible caused by: ${exceptionStore[label]}"))
            }
        }
    }
}