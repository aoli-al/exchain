package al.aoli.exception.instrumentation.analyzers.values

import org.objectweb.asm.Type
import org.objectweb.asm.Type.DOUBLE_TYPE
import org.objectweb.asm.Type.LONG_TYPE
import org.objectweb.asm.tree.analysis.Value


class DataFlowValue(val type: Type, val children: Set<DataFlowValue>): Value {
    val id: Int = uniqueId++

    constructor(type: Type, vararg children: DataFlowValue): this(type, children.toSet())
    constructor(type: Type, children: List<DataFlowValue>): this(type, children.toSet())


    fun merge(other: DataFlowValue): DataFlowValue {
        if (other == this) return this

        return DataFlowValue(type, children + other.children)
    }

    fun variables(): Set<DataFlowValue> {
        return setOf(this) + children.flatMap { it.variables() }.toSet()
    }


    override fun getSize(): Int {
        return if (type == LONG_TYPE || type == DOUBLE_TYPE) 2 else 1
    }

    override fun equals(other: Any?): Boolean {
        if (other is DataFlowValue) {
            return id == other.id && children == other.children && type == other.type
        }
        return super.equals(other)
    }


    override fun toString(): String {
        val b = StringBuilder()
//        b.append("<SimpleFlowValue id=${id} type=${type}")
//            b.append(" merge [${inputs.joinToString(",")}]")
//        } else if (origin != null) {
//            b.append(" origin=${origin}")
//        }
//        b.append(">")
        return b.toString()
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + children.hashCode()
        result = 31 * result + id
        return result
    }

    companion object {
        var uniqueId = 0
    }


}