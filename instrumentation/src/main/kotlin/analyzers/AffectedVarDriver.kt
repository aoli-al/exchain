package al.aoli.exchain.instrumentation.analyzers

import al.aoli.exchain.instrumentation.store.TransformedCodeStore
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
    fun analyzeAffectedVar(clazz: String, method: String, throwIndex: Long, catchIndex: Long) : AffectedVarResults {
        val className = clazz.replace("/", ".").substring(1 until clazz.length-1)
        logger.info { "Start processing ${className}, method: $method, throwIndex: $throwIndex, catchIndex: $catchIndex" }
        val classReader = if (className in TransformedCodeStore.store) {
            ClassReader(TransformedCodeStore.store[className])
        } else {
            ClassReader(className)
        }
        val visitor = AffectedVarClassVisitor(throwIndex, catchIndex, className, method)
        classReader.accept(visitor, 0)
        // We are going to taint class fields here and local variables in native.
        val affectedFields = visitor.methodVisitor?.affectedFields?.toTypedArray() ?: emptyArray()
        val affectedVars = visitor.methodVisitor?.affectedVars?.toIntArray() ?: intArrayOf()
        return AffectedVarResults(affectedVars, affectedFields)
    }

    fun taintAffectedVar(obj: SetNode, idx: Int, thread: Thread, depth: Int, label: Int): Taint<*> {
        return obj.union(Taint.withLabel(label))
//        if (obj is Taint<*> || obj is PhosphorStackFrame) {
//            return
//        }
//        if (obj is TaintedWithObjTag) {
//            val tag = Taint.withLabel(label)
//            if (obj.phosphoR_TAG == null) {
//                obj.phosphoR_TAG = tag
//            } else {
//                obj.phosphoR_TAG = (obj.phosphoR_TAG as Taint<Int>).union(tag)
//            }
//        }
    }
}