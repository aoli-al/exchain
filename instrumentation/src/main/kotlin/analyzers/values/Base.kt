package al.aoli.exception.instrumentation.analyzers.values

import org.objectweb.asm.Type
import org.objectweb.asm.tree.analysis.Value

abstract class Base(val type: Type): Value {
    override fun getSize(): Int {
        return if (type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE) 2 else 1
    }
    abstract fun variables(): Set<Base>
}