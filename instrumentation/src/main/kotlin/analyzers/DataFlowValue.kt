package al.aoli.exception.instrumentation.analyzers

import org.objectweb.asm.Type
import org.objectweb.asm.Type.DOUBLE_TYPE
import org.objectweb.asm.Type.LONG_TYPE
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.Value


class DataFlowValue(val type: Type?, val origin: AbstractInsnNode?,
                    val inputs: Collection<DataFlowValue>, val isMerge: Boolean): Value {
    val id: Int = uniqueId++


    val isReference: Boolean by lazy {
        (type != null
                && (type.sort == Type.OBJECT || type.sort == Type.ARRAY))
    }

    fun merge(other: DataFlowValue): DataFlowValue {
        if (other == this) return this
        var newType: Type? = type
        if (newType == null || other.type == null || newType != other.type) {
            newType = null
        }

        val newInputs = mutableSetOf<DataFlowValue>()
        if (isMerge) {
            newInputs.addAll(inputs)
        } else {
            newInputs.add(this)
        }

        if (other.isMerge) {
            newInputs.addAll(other.inputs)
        } else {
            newInputs.add(other)
        }

        return DataFlowValue(newType, null, newInputs, true)
    }


    override fun getSize(): Int {
        return if (type == LONG_TYPE || type == DOUBLE_TYPE) 2 else 1
    }

    override fun equals(other: Any?): Boolean {
        if (other is DataFlowValue) {
            if (origin != null) {
                return origin == other.origin
            }
            if (isMerge) {
                if (!other.isMerge) {
                    return false
                }
                return inputs == other.inputs
            }
            return id == other.id
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return origin?.hashCode()
            ?: if (isMerge) {
                inputs.hashCode()
            } else {
                id
            }
    }

    override fun toString(): String {
        val b = StringBuilder()
        b.append("<SimpleFlowValue id=${id} type=${type}")
        if (isMerge) {
            b.append(" merge [${inputs.joinToString(",")}]")
        } else if (origin != null) {
            b.append(" origin=${origin}")
        }
        b.append(">")
        return b.toString()
    }
    companion object {
        var uniqueId = 0
    }


}