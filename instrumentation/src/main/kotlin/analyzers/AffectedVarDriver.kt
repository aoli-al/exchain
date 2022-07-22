package al.aoli.exchain.instrumentation.analyzers

import mu.KotlinLogging
import org.objectweb.asm.*

private val logger = KotlinLogging.logger {}
object AffectedVarDriver {
    fun analyzeAffectedVar(clazz: Class<out Any>, method: String, throwIndex: Long, catchIndex: Long) : IntArray {
        logger.info { "Start processing ${clazz.name}, method: $method, throwIndex: $throwIndex, catchIndex: $catchIndex" }
        val classReader = ClassReader(clazz.name)
        val visitor = AffectedVarClassVisitor(throwIndex, catchIndex, clazz.name, method)
        classReader.accept(visitor, 0)
        // We are going to taint class fields here and local variables in native.
        val affectedFields = visitor.methodVisitor?.affectedFields ?: emptyList()
        val affectedVars = visitor.methodVisitor?.affectedVars ?: emptyList()


        return affectedVars.toIntArray()
    }
}