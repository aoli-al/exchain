package al.aoli.exception.instrumentation.analyzers.values

import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.Value

class BaseValue(type: Type, val origin: AbstractInsnNode): Base(type) {
    override fun equals(other: Any?): Boolean {
        if (other is BaseValue) {
            return origin == other.origin && type == other.type
        }
        return super.equals(other)
    }

    override fun variables(): Set<Base> {
        return setOf(this)
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + origin.hashCode()
        return result
    }
}