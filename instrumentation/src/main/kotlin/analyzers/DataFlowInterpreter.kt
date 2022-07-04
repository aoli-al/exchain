package al.aoli.exception.instrumentation.analyzers

import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.AnalyzerException
import org.objectweb.asm.tree.analysis.Interpreter

class DataFlowInterpreter: Interpreter<DataFlowValue>(ASM8), Opcodes {
    val values = mutableMapOf<AbstractInsnNode, DataFlowValue>()
    val fields = mutableMapOf<String, DataFlowValue>()
    override fun newValue(type: Type?): DataFlowValue {
        return newValue(type, null)
    }

    private fun newValue(type: Type?, origin: AbstractInsnNode?, vararg inputs: DataFlowValue): DataFlowValue {
        return newValue(type, origin, inputs.toSet())
    }

    private fun newValue(type: Type?, origin: AbstractInsnNode?, inputs: Collection<DataFlowValue> = emptySet()): DataFlowValue {
        val value = DataFlowValue(type, origin, inputs, false)
        if (origin != null) {
            values[origin] = value
        }
        return value
    }


    override fun newOperation(insn: AbstractInsnNode): DataFlowValue {
        return when (insn.opcode) {
            ACONST_NULL -> newValue(Type.getObjectType("null"), insn)
            ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 -> newValue(
                Type.INT_TYPE, insn
            )
            LCONST_0, LCONST_1 -> newValue(Type.LONG_TYPE, insn)
            FCONST_0, FCONST_1, FCONST_2 -> newValue(Type.FLOAT_TYPE, insn)
            DCONST_0, DCONST_1 -> newValue(Type.DOUBLE_TYPE, insn)
            BIPUSH, SIPUSH -> newValue(Type.INT_TYPE, insn)
            LDC -> {
                val cst: Any = (insn as LdcInsnNode).cst
                if (cst is Int) {
                    newValue(Type.INT_TYPE, insn)
                } else if (cst is Float) {
                    newValue(Type.FLOAT_TYPE, insn)
                } else if (cst is Long) {
                    newValue(Type.LONG_TYPE, insn)
                } else if (cst is Double) {
                    newValue(Type.DOUBLE_TYPE, insn)
                } else if (cst is String) {
                    newValue(Type.getObjectType("java/lang/String"), insn)
                } else if (cst is Type) {
                    val sort = cst.sort
                    if (sort == Type.OBJECT || sort == Type.ARRAY) {
                        newValue(Type.getObjectType("java/lang/Class"), insn)
                    } else if (sort == Type.METHOD) {
                        newValue(
                            Type
                                .getObjectType("java/lang/invoke/MethodType"), insn
                        )
                    } else {
                        throw IllegalArgumentException(
                            "Illegal LDC constant "
                                    + cst
                        )
                    }
                } else if (cst is Handle) {
                    newValue(
                        Type
                            .getObjectType("java/lang/invoke/MethodHandle"), insn
                    )
                } else {
                    throw IllegalArgumentException(
                        ("Illegal LDC constant "
                                + cst)
                    )
                }
            }
            JSR -> newValue(Type.VOID_TYPE, insn)
            GETSTATIC -> newValue(Type.getType((insn as FieldInsnNode).desc), insn)
            NEW -> newValue(Type.getObjectType((insn as TypeInsnNode).desc), insn)
            else -> throw Error("Internal error.")
        }
    }

    override fun copyOperation(insn: AbstractInsnNode, value: DataFlowValue): DataFlowValue {
        return value
    }

    override fun binaryOperation(
        insn: AbstractInsnNode,
        value1: DataFlowValue,
        value2: DataFlowValue
    ): DataFlowValue? {
        return when (insn.opcode) {
            IALOAD, BALOAD, CALOAD, SALOAD, IADD, ISUB, IMUL, IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR, IXOR ->
                newValue(Type.INT_TYPE, insn, value1, value2)
            FALOAD, FADD, FSUB, FMUL, FDIV, FREM ->
                newValue(Type.FLOAT_TYPE, insn, value1, value2)
            LALOAD, LADD, LSUB, LMUL, LDIV, LREM, LSHL, LSHR, LUSHR, LAND, LOR, LXOR ->
                newValue(Type.LONG_TYPE, insn, value1, value2)
            DALOAD, DADD, DSUB, DMUL, DDIV, DREM ->
                newValue(Type.DOUBLE_TYPE, insn, value1, value2)
            AALOAD ->
                newValue(Type.getObjectType("java/lang/Object"), insn, value1, value2)
            LCMP, FCMPL, FCMPG, DCMPL, DCMPG ->
                newValue(Type.INT_TYPE, insn, value1, value2)
            IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE -> {
                newValue(null, insn, value1, value2)
                null
            }
            PUTFIELD -> {
                val fieldInsn = (insn as FieldInsnNode)
                val key = fieldKey(fieldInsn)
                fields.getOrPut(key) {
                    newValue(Type.getObjectType(fieldInsn.desc))
                }.merge(value1)
                null
            }
            else -> newValue(null, null)
        }
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: DataFlowValue): DataFlowValue? {
        return when (insn.opcode) {
            INEG, IINC, L2I, F2I, D2I, I2B, I2C, I2S -> newValue(Type.INT_TYPE, insn, value)
            FNEG, I2F, L2F, D2F -> newValue(Type.FLOAT_TYPE, insn, value)
            LNEG, I2L, F2L, D2L -> newValue(Type.LONG_TYPE, insn, value)
            DNEG, I2D, L2D, F2D -> newValue(Type.DOUBLE_TYPE, insn, value)
            IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, TABLESWITCH, LOOKUPSWITCH, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, PUTSTATIC -> {
                newValue(null, insn, value)
                null
            }
            GETFIELD -> {
                val fieldInsn = (insn as FieldInsnNode)
                val key = fieldKey(fieldInsn)
                fields.getOrPut(key) {
                    newValue(Type.getObjectType(fieldInsn.desc))
                }
                null
            }
            NEWARRAY -> when ((insn as IntInsnNode).operand) {
                T_BOOLEAN -> newValue(Type.getType("[Z"), insn, value)
                T_CHAR -> newValue(Type.getType("[C"), insn, value)
                T_BYTE -> newValue(Type.getType("[B"), insn, value)
                T_SHORT -> newValue(Type.getType("[S"), insn, value)
                T_INT -> newValue(Type.getType("[I"), insn, value)
                T_FLOAT -> newValue(Type.getType("[F"), insn, value)
                T_DOUBLE -> newValue(Type.getType("[D"), insn, value)
                T_LONG -> newValue(Type.getType("[J"), insn, value)
                else -> throw AnalyzerException(insn, "Invalid array type")
            }
            ANEWARRAY -> {
                val desc = (insn as TypeInsnNode).desc
                newValue(Type.getType("[" + Type.getObjectType(desc)), insn, value)
            }
            ARRAYLENGTH -> newValue(Type.INT_TYPE, insn, value)
            ATHROW -> {
                newValue(null, insn, value)
                null
            }
            CHECKCAST -> {
                val desc = (insn as TypeInsnNode).desc
                newValue(Type.getObjectType(desc), insn, value)
            }
            INSTANCEOF -> newValue(Type.INT_TYPE, insn, value)
            MONITORENTER, MONITOREXIT, IFNULL, IFNONNULL -> {
                newValue(null, insn, value)
                null
            }
            else -> throw java.lang.Error("Internal error.")
        }
    }

    override fun ternaryOperation(
        insn: AbstractInsnNode,
        value1: DataFlowValue,
        value2: DataFlowValue,
        value3: DataFlowValue
    ): DataFlowValue {
        return newValue(null, insn, value1, value2, value3)
    }

    override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out DataFlowValue>): DataFlowValue {
        val opcode = insn.opcode
        return if (opcode == MULTIANEWARRAY) {
            newValue(Type.getType((insn as MultiANewArrayInsnNode).desc), insn, values)
        } else if (opcode == INVOKEDYNAMIC) {
            newValue(
                Type.getReturnType((insn as InvokeDynamicInsnNode).desc), insn,
                values
            )
        } else {
            newValue(
                Type.getReturnType((insn as MethodInsnNode).desc), insn,
                values
            )
        }
    }


    override fun returnOperation(insn: AbstractInsnNode, value: DataFlowValue, expected: DataFlowValue) {
    }

    override fun merge(value1: DataFlowValue, value2: DataFlowValue): DataFlowValue {
        return value1.merge(value2)
    }

    private fun fieldKey(insn: FieldInsnNode): String {
        return "${insn.desc}.${insn.name}"
    }
}