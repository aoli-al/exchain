package al.aoli.exception.instrumentation.analyzers.values

import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode

class Holder(type: Type, val insn: AbstractInsnNode, val base: Base): Base(type) {
    override fun variables(): Set<Base> {
        return setOf(this) + base.variables()
    }

    override fun equals(other: Any?): Boolean {
        if (other is Holder) {
            return other.base == base && other.insn == insn
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = insn.hashCode()
        result = 31 * result + base.hashCode()
        return result
    }
}