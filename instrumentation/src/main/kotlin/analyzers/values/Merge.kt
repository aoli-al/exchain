package al.aoli.exception.instrumentation.analyzers.values

import org.objectweb.asm.Type

class Merge(type: Type, private val children: List<Base>): Base(type) {

    constructor(type: Type, vararg v: Base): this(type, v.toList())
    override fun variables(): Set<Base> {
        return setOf(this) + children.flatMap { it.variables() }.toSet()
    }

    override fun getSize(): Int {
        return 1
    }

    override fun equals(other: Any?): Boolean {
        if (other is Merge) {
            return variables() == other.variables()
        }
        return super.equals(other)
    }
}