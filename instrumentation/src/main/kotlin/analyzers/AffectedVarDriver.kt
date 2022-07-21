package al.aoli.exchain.instrumentation.analyzers

import org.objectweb.asm.*
object AffectedVarDriver {
    fun analyzeAffectedVar(clazz: Class<out Any>, method: String, throwIndex: Int, catchIndex: Int) {
        val classReader = ClassReader(clazz.name)
        val visitor = AffectedVarClassVisitor(throwIndex, catchIndex, clazz.name, method)
        classReader.accept(visitor, 0)
    }
}