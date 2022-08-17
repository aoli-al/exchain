package al.aoli.exchain.instrumentation.analyzers

import mu.KotlinLogging
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.SourceValue


private val logger = KotlinLogging.logger {}
class AffectedVarMethodVisitor(val throwIndex: Long, val catchIndex: Long, val isThrowInsn: Boolean, val owner: String,
                               access: Int, name: String, descriptor: String, signature: String?,
                               exceptions: Array<out String>?):
    MethodNode(ASM8, access, name, descriptor, signature, exceptions) {
    var byteCodeOffset = 0L
    var throwInsn: AbstractInsnNode? = null
    var catchInsn: AbstractInsnNode? = null
    private val isStatic = access and ACC_STATIC != 0
    private val paramSize: Int

    init {
        paramSize = Type.getArgumentTypes(descriptor).size + if (isStatic) 0 else 1
    }



    private fun checkThrowInsn() {
        if (byteCodeOffset == throwIndex) {
            throwInsn = instructions.last
        }
        if (byteCodeOffset == catchIndex) {
            var current = instructions.last
            while (current !is LabelNode) {
                current = current.previous
            }
            catchInsn = current
        }
    }

    override fun visitInsn(opcode: Int) {
        super.visitInsn(opcode)
        checkThrowInsn()
        byteCodeOffset += 1
    }

    override fun visitVarInsn(opcode: Int, varIndex: Int) {
        super.visitVarInsn(opcode, varIndex)
        checkThrowInsn()
        byteCodeOffset += if (varIndex < 4 && opcode != RET) {
            1
        } else if (varIndex > 256) {
            4
        } else {
            2
        }
    }

    override fun visitJumpInsn(opcode: Int, label: Label?) {
        super.visitJumpInsn(opcode, label)
        checkThrowInsn()
        byteCodeOffset += 3
    }

    override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label, vararg labels: Label) {
        super.visitTableSwitchInsn(min, max, dflt, *labels)
        checkThrowInsn()
        byteCodeOffset += 4 - (byteCodeOffset and 3)
        byteCodeOffset += 12 + 4 * labels.size
    }

    override fun visitLookupSwitchInsn(dflt: Label, keys: IntArray, labels: Array<out Label>) {
        super.visitLookupSwitchInsn(dflt, keys, labels)
        checkThrowInsn()
        byteCodeOffset += 4 - (byteCodeOffset and 3)
        byteCodeOffset += 8 + 8 * labels.size
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        super.visitFieldInsn(opcode, owner, name, descriptor)
        checkThrowInsn()
        byteCodeOffset += 3
    }

    override fun visitMethodInsn(
        opcodeAndSource: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
        checkThrowInsn()
        val opcode = opcodeAndSource and SOURCE_MASK.inv()
        byteCodeOffset += if (opcode == INVOKEINTERFACE) {
            5
        } else {
            3
        }
    }

    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ) {
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
        checkThrowInsn()
        byteCodeOffset += 5
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        super.visitTypeInsn(opcode, type)
        checkThrowInsn()
        byteCodeOffset += 3
    }

    override fun visitIincInsn(varIndex: Int, increment: Int) {
        super.visitIincInsn(varIndex, increment)
        checkThrowInsn()
        byteCodeOffset += if (varIndex > 255 || increment > 127 || increment < -128) {
            6
        } else {
            3
        }
    }

    override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
        super.visitMultiANewArrayInsn(descriptor, numDimensions)
        checkThrowInsn()
        byteCodeOffset += 4
    }


    override fun visitIntInsn(opcode: Int, operand: Int) {
        super.visitIntInsn(opcode, operand)
        checkThrowInsn()
        byteCodeOffset += when (opcode) {
            BIPUSH, NEWARRAY -> 2
            else -> 3
        }
    }

    override fun visitLdcInsn(value: Any) {
        super.visitLdcInsn(value)
        checkThrowInsn()
        byteCodeOffset += if (value is Long ||
            value is Double ||
            value is ConstantDynamic && value.size == 2) {
            3
        } else {
            2
        }
    }


    val affectedVars = mutableSetOf<Int>()
    val affectedFields = mutableSetOf<String>()
    val sourceVars = mutableSetOf<Int>()
    val affectedParams = mutableSetOf<Int>()
    val sourceFields = mutableSetOf<String>()

    private fun processAffectedInsns(affectedInsns: Set<AbstractInsnNode>, frames: Array<Frame<SourceValue>>) {
        val throwInsnFrame = frames[instructions.indexOf(throwInsn)]
        for (insn in affectedInsns) {
            val frame = frames[instructions.indexOf(insn)]
            when (insn) {
                is MethodInsnNode -> {
                    if (insn.opcode != INVOKESTATIC && insn.opcode != INVOKEDYNAMIC) {
                        val value = try {
                            for (type in Type.getArgumentTypes(insn.desc)) {
                                frame.pop()
                            }
                            frame.pop()
                        } catch (e: IndexOutOfBoundsException) {
                            logger.error { "Processing instruction $insn failed." }
                            continue
                        }
                        for (src in value.insns) {
                            val srcFrame = frames[instructions.indexOf(src)]
                            // If a method call is declared in a field the field must be from `this`.
                            if (src is FieldInsnNode && src.opcode == GETFIELD && !isStatic) {
                                val fieldValue = srcFrame.pop()
                                if (fieldValue.insns.size != 1) continue
                                val fieldSrc = fieldValue.insns.first()
                                if (fieldSrc is VarInsnNode && fieldSrc.opcode == ALOAD && fieldSrc.`var` == 0) {
                                    affectedFields.add(src.name)
                                }
                            }
                            // If a method call is from a local object, check if the object is on the stack.
                            if (src is VarInsnNode && src.opcode == ALOAD) {
                                try {
                                    if (throwInsnFrame.getLocal(src.`var`) == srcFrame.getLocal(src.`var`)) {
                                        if (insn == throwInsn) {
                                            sourceVars.add(src.`var`)
                                        }
                                        affectedVars.add(src.`var`)
                                    }
                                } catch (e: IndexOutOfBoundsException) {
                                    logger.info { "Local object is not on the stack!" }
                                }
                            }
                        }
                    }
                }
                is FieldInsnNode -> {
                    if (insn.opcode == PUTFIELD) {
                        val objRef = try {
                            frame.pop()
                            frame.pop()
                        } catch (e: IndexOutOfBoundsException) {
                            logger.error { "Processing instruction $insn failed." }
                            continue
                        }
                        for (src in objRef.insns) {
                            if (src is VarInsnNode) {
                                if (src.`var` == 0 && !isStatic) {
                                    affectedFields.add(insn.name)
                                }
                                else {
                                    affectedVars.add(src.`var`)
                                }
                            }
                        }
                    }
                }
                is VarInsnNode -> {
                    if (catchInsn == null) continue
                    when (insn.opcode) {
                        ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> {
                            if (insn.`var` < throwInsnFrame.locals) {
                                affectedVars.add(insn.`var`)
                            }
                        }
                    }
                }
            }
        }
        println(affectedFields)
        println(affectedVars)
    }

    override fun visitEnd() {
        super.visitEnd()

        val tryCatchLocations = mutableListOf<Pair<Int, Int>>()
        for (tryCatchBlock in tryCatchBlocks) {
            if (tryCatchBlock.handler == catchInsn) {
                tryCatchLocations.add(Pair(instructions.indexOf(tryCatchBlock.start), instructions.indexOf(tryCatchBlock.end)))
            }
        }
        if (throwInsn != null && (catchIndex == -1L || catchInsn != null)) {
            val throwInsnIndex = instructions.indexOf(throwInsn)
            val interpreter = AffectedVarInterpreter(instructions, throwInsnIndex, tryCatchLocations)
            val analyzer = AffectedVarAnalyser(instructions, interpreter, throwInsn!!, catchInsn)
            analyzer.analyze(owner, this)
            val affectedInsns = interpreter.affectedInsns
            if (catchIndex != -1L) {
                val normalRunInterpreter = AffectedVarInterpreter(instructions, throwInsnIndex, emptyList())
                val normalRunAnalyzer = AffectedVarAnalyser(instructions, normalRunInterpreter, throwInsn!!, null)
                normalRunAnalyzer.analyze(owner, this)
                for (insn in analyzer.reachableBlocks(catchInsn!!)) {
                    if (insn in normalRunInterpreter.affectedInsns && insn !in interpreter.affectedInsnInTry) {
                        affectedInsns.remove(insn)
                    }
                }
            }
            affectedInsns.add(throwInsn!!)
            processAffectedInsns(affectedInsns, analyzer.frames)
        } else {
            logger.error { "Error finding throwInsn/catchInsn at index $throwIndex in class $owner:$name" }
        }
    }
}

