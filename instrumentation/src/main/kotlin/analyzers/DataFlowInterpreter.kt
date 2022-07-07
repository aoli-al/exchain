package al.aoli.exception.instrumentation.analyzers

import al.aoli.exception.instrumentation.analyzers.values.*
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.AnalyzerException
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.analysis.Value
import javax.xml.crypto.Data

class DataFlowInterpreter: Interpreter<DataFlowValue>(ASM8), Opcodes {
    val values = mutableMapOf<AbstractInsnNode, DataFlowValue>()
    val fields = mutableMapOf<String, DataFlowValue>()

    override fun newValue(type: Type?): DataFlowValue? {
        if (type == Type.VOID_TYPE) {
            return null
        }
        if (type == null) {
            return DataFlowValue(Type.getObjectType("null"))
        }
        return DataFlowValue(type)
    }

    override fun newOperation(insn: AbstractInsnNode): DataFlowValue {
        return when (insn.opcode) {
            ACONST_NULL -> DataFlowValue(Type.getObjectType("null"))
            ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 ->
                DataFlowValue(Type.INT_TYPE)
            LCONST_0, LCONST_1 -> DataFlowValue(Type.LONG_TYPE)
            FCONST_0, FCONST_1, FCONST_2 -> DataFlowValue(Type.FLOAT_TYPE)
            DCONST_0, DCONST_1 -> DataFlowValue(Type.DOUBLE_TYPE)
            BIPUSH, SIPUSH -> DataFlowValue(Type.INT_TYPE)
            LDC -> {
                val cst: Any = (insn as LdcInsnNode).cst
                if (cst is Int) {
                    DataFlowValue(Type.INT_TYPE)
                } else if (cst is Float) {
                    DataFlowValue(Type.FLOAT_TYPE)
                } else if (cst is Long) {
                    DataFlowValue(Type.LONG_TYPE)
                } else if (cst is Double) {
                    DataFlowValue(Type.DOUBLE_TYPE)
                } else if (cst is String) {
                    DataFlowValue(Type.getObjectType("java/lang/String"))
                } else if (cst is Type) {
                    val sort = cst.sort
                    if (sort == Type.OBJECT || sort == Type.ARRAY) {
                        DataFlowValue(Type.getObjectType("java/lang/Class"))
                    } else if (sort == Type.METHOD) {
                        DataFlowValue(Type.getObjectType("java/lang/invoke/MethodType"))
                    } else {
                        throw IllegalArgumentException("Illegal LDC constant " + cst)
                    }
                } else if (cst is Handle) {
                    DataFlowValue(Type.getObjectType("java/lang/invoke/MethodHandle"))
                } else {
                    throw IllegalArgumentException(
                        ("Illegal LDC constant "
                                + cst)
                    )
                }
            }
            GETSTATIC -> DataFlowValue(Type.getType((insn as FieldInsnNode).desc))
            NEW -> DataFlowValue(Type.getObjectType((insn as TypeInsnNode).desc))
            else -> throw Error("Internal error.")
        }
    }

    override fun copyOperation(insn: AbstractInsnNode, value: DataFlowValue): DataFlowValue {
        return value
    }

    override fun binaryOperation(insn: AbstractInsnNode, value1: DataFlowValue, value2: DataFlowValue): DataFlowValue? {
        return when (insn.opcode) {
            IALOAD, BALOAD, CALOAD, SALOAD, IADD, ISUB, IMUL, IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR, IXOR ->
                DataFlowValue(Type.INT_TYPE, value1, value2)
            FALOAD, FADD, FSUB, FMUL, FDIV, FREM ->
                DataFlowValue(Type.FLOAT_TYPE, value1, value2)
            LALOAD, LADD, LSUB, LMUL, LDIV, LREM, LSHL, LSHR, LUSHR, LAND, LOR, LXOR ->
                DataFlowValue(Type.LONG_TYPE, value1, value2)
            DALOAD, DADD, DSUB, DMUL, DDIV, DREM ->
                DataFlowValue(Type.DOUBLE_TYPE, value1, value2)
            AALOAD ->
                DataFlowValue(Type.getObjectType("java/lang/Object"), value1, value2)
            LCMP, FCMPL, FCMPG, DCMPL, DCMPG ->
                DataFlowValue(Type.INT_TYPE, value1, value2)
            IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE -> {
//                Merge(null, insn, value1, value2)
                null
            }
            PUTFIELD -> {
//                val fieldInsn = (insn as FieldInsnNode)
//                val key = fieldKey(fieldInsn)
//                fields.getOrPut(key) {
//                    newValue(Type.getObjectType(fieldInsn.desc))!!
//                }.merge(value1)
                null
            }
            else -> null
        }
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: DataFlowValue): DataFlowValue? {
        return when (insn.opcode) {
            INEG, IINC, L2I, F2I, D2I, I2B, I2C, I2S -> DataFlowValue(Type.INT_TYPE, value)
            FNEG, I2F, L2F, D2F -> DataFlowValue(Type.FLOAT_TYPE, value)
            LNEG, I2L, F2L, D2L -> DataFlowValue(Type.LONG_TYPE, value)
            DNEG, I2D, L2D, F2D -> DataFlowValue(Type.DOUBLE_TYPE, value)
            IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, TABLESWITCH, LOOKUPSWITCH, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, PUTSTATIC -> {
//                newValue(null, insn, value)
                null
            }
            GETFIELD -> {
//                val fieldInsn = (insn as FieldInsnNode)
//                val key = fieldKey(fieldInsn)
//                fields.getOrPut(key) {
//                    newValue(Type.getObjectType(fieldInsn.desc))!!
//                }
                null
            }
            NEWARRAY -> when ((insn as IntInsnNode).operand) {
                T_BOOLEAN -> DataFlowValue(Type.getType("[Z"), value)
                T_CHAR -> DataFlowValue(Type.getType("[C"), value)
                T_BYTE -> DataFlowValue(Type.getType("[B"), value)
                T_SHORT -> DataFlowValue(Type.getType("[S"), value)
                T_INT -> DataFlowValue(Type.getType("[I"), value)
                T_FLOAT -> DataFlowValue(Type.getType("[F"), value)
                T_DOUBLE -> DataFlowValue(Type.getType("[D"), value)
                T_LONG -> DataFlowValue(Type.getType("[J"), value)
                else -> throw AnalyzerException(insn, "Invalid array type")
            }
            ANEWARRAY -> {
                val desc = (insn as TypeInsnNode).desc
                DataFlowValue(Type.getType("[" + Type.getObjectType(desc)), value)
            }
            ARRAYLENGTH -> DataFlowValue(Type.INT_TYPE, value)
            ATHROW -> {
//                newValue(null, insn, value)
                null
            }
            CHECKCAST -> {
                val desc = (insn as TypeInsnNode).desc
                DataFlowValue(Type.getObjectType(desc), value)
            }
            INSTANCEOF -> DataFlowValue(Type.INT_TYPE, value)
            MONITORENTER, MONITOREXIT, IFNULL, IFNONNULL -> {
//                newValue(null, insn, value)
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
    ): DataFlowValue? {
        return null
    }

    override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out DataFlowValue>): DataFlowValue? {
        val opcode = insn.opcode
        return if (opcode == MULTIANEWARRAY) {
            DataFlowValue(Type.getType((insn as MultiANewArrayInsnNode).desc), values)
        } else if (opcode == INVOKEDYNAMIC) {
            val returnType = Type.getReturnType((insn as InvokeDynamicInsnNode).desc)
            if (returnType != Type.VOID_TYPE) {
                DataFlowValue(returnType, values)
            } else {
                null
            }
        } else {
            val returnType = Type.getReturnType((insn as MethodInsnNode).desc)
            if (returnType != Type.VOID_TYPE) {
                DataFlowValue(returnType, values)
            } else {
                null
            }
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