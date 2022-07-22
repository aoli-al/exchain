package al.aoli.exchain.instrumentation.analyzers

import al.aoli.exchain.instrumentation.store.TransformedCodeStore
import mu.KotlinLogging
import org.objectweb.asm.*

private val logger = KotlinLogging.logger {}
object AffectedVarDriver {
    fun analyzeAffectedVar(clazz: String, method: String, throwIndex: Long, catchIndex: Long) : IntArray {
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
        val affectedFields = visitor.methodVisitor?.affectedFields ?: emptyList()
        val affectedVars = visitor.methodVisitor?.affectedVars ?: emptyList()


        return affectedVars.toIntArray()
    }
}