package al.aoli.exchain.runtime.analyzers

import al.aoli.exchain.runtime.logger.Logger
import al.aoli.exchain.runtime.objects.SourceType
import org.objectweb.asm.ConstantDynamic
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.SourceValue

private val logger = Logger()

class AffectedVarMethodVisitor(
    val exception: Throwable,
    val throwIndex: Long,
    val catchIndex: Long,
    val isThrowInsn: Boolean,
    val findSource: Boolean,
    val owner: String,
    access: Int,
    name: String,
    descriptor: String,
    signature: String?,
    exceptions: Array<out String>?,
    val classReader: AffectedVarClassReader
) : MethodNode(Opcodes.ASM8, access, name, descriptor, signature, exceptions) {
    var byteCodeOffset = 0L
    var throwInsn: AbstractInsnNode? = null
    var catchInsn: AbstractInsnNode? = null
    private val isStatic = access and Opcodes.ACC_STATIC != 0
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
        byteCodeOffset +=
            if (varIndex < 4 && opcode != Opcodes.RET) {
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
        val opcode = opcodeAndSource and Opcodes.SOURCE_MASK.inv()
        byteCodeOffset +=
            if (opcode == Opcodes.INVOKEINTERFACE) {
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
        byteCodeOffset +=
            if (varIndex > 255 || increment > 127 || increment < -128) {
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
        byteCodeOffset +=
            when (opcode) {
                Opcodes.BIPUSH,
                Opcodes.NEWARRAY -> 2
                else -> 3
            }
    }

    override fun visitLdcInsn(value: Any) {
        super.visitLdcInsn(value)
        checkThrowInsn()
        byteCodeOffset +=
            if (value is Long ||
                value is Double ||
                value is ConstantDynamic && value.size == 2 ||
                classReader.lastReadConstIndex > 255
            ) {
                3
            } else {
                2
            }
    }

    val affectedVars = mutableSetOf<Triple<Int, Int, String>>()
    val affectedFields = mutableSetOf<Pair<Int, String>>()
    val affectedStaticField = mutableSetOf<Pair<Int, String>>()
    val sourceLines = mutableSetOf<Pair<Int, SourceType>>()
    val sourceField = mutableSetOf<String>()
    val sourceStaticField = mutableSetOf<String>()
    val sourceLocalVariable = mutableSetOf<Int>()

    // This is not the right way to get local variable names.
    // However, this is the implementation of Soot and we
    // need to make it consistent.
    // https://github.com/soot-oss/soot/blob/ea2f0c2956fda48698416dd342233f7df15b5d76/src/main/java/soot/asm/AsmMethodSource.java#L403
    fun getLocalName(idx: Int): String {
        for (lvn in localVariables) {
            if (lvn.index == idx && lvn.start != lvn.end) {
                return lvn.name
            }
        }
        return "l" + idx
    }

    fun getLineNumber(insn: AbstractInsnNode): Int {
        var cur: AbstractInsnNode? = insn
        while (cur != null && cur !is LineNumberNode) {
            cur = cur.previous
        }
        if (cur == null) return -1
        return (cur as LineNumberNode).line
    }

    fun appendAffectedFields(insn: FieldInsnNode) {
        affectedFields.add(Pair(getLineNumber(insn), insn.name))
    }

    fun appendAffectedStaticField(insn: FieldInsnNode) {
        affectedStaticField.add(Pair(getLineNumber(insn), insn.owner + "#" + insn.name))
    }

    fun appendAffectedVars(insn: VarInsnNode) {
        val name = getLocalName(insn.`var`)
        if (name.startsWith("phosphor")) return
        for (affectedVar in affectedVars) {
            if (affectedVar.second == insn.`var`) {
                return
            }
        }
        affectedVars.add(Triple(getLineNumber(insn), insn.`var`, name))
    }

    private fun processAffectedInsns(
        affectedInsns: Set<AbstractInsnNode>,
        frames: Array<Frame<SourceValue>?>
    ) {
        val throwInsnIndex = instructions.indexOf(throwInsn)
        val throwInsnFrame = frames[throwInsnIndex]
        for (insn in affectedInsns) {
            val frame = frames[instructions.indexOf(insn)]
            when (insn) {
                is MethodInsnNode -> {
                    if (insn.opcode != Opcodes.INVOKESTATIC && insn.opcode != Opcodes.INVOKEDYNAMIC) {
                        val value =
                            try {
                                frame?.getStack(frame.stackSize - Type.getArgumentTypes(insn.desc).size - 1)
                            } catch (e: IndexOutOfBoundsException) {
                                logger.error { "Processing instruction $insn failed." }
                                continue
                            } ?: continue
                        for (src in value.insns) {
                            val srcFrame = frames[instructions.indexOf(src)]
                            // If a method call is called to a field the field must be from `this`.
                            if (src is FieldInsnNode && src.opcode == Opcodes.GETFIELD && !isStatic) {
                                val fieldValue = srcFrame?.getStack(srcFrame.stackSize - 1) ?: continue
                                if (fieldValue.insns.size != 1) continue
                                val fieldSrc = fieldValue.insns.first()
                                if (fieldSrc is VarInsnNode &&
                                    fieldSrc.opcode == Opcodes.ALOAD &&
                                    fieldSrc.`var` == 0
                                ) {
                                    appendAffectedFields(src)
                                }
                            }
                            if (src is FieldInsnNode && src.opcode == Opcodes.GETSTATIC) {
                                appendAffectedStaticField(src)
                            }
                            // If a method call is from a local object, check if the object is on the stack.
                            if (src is VarInsnNode && src.opcode == Opcodes.ALOAD) {
                                try {
                                    if (throwInsnFrame?.getLocal(src.`var`) == srcFrame?.getLocal(src.`var`)) {
                                        appendAffectedVars(src)
                                    }
                                } catch (e: IndexOutOfBoundsException) {
                                    logger.info { "Local object is not on the stack!" }
                                }
                            }
                        }
                    }
                }
                is FieldInsnNode -> {
                    when (insn.opcode) {
                        Opcodes.PUTFIELD -> {
                            val objRef =
                                try {
                                    frame?.getStack(frame.stackSize - 2)
                                } catch (e: IndexOutOfBoundsException) {
                                    logger.error { "Processing instruction $insn failed. Error: $e" }
                                    continue
                                } ?: continue
                            for (src in objRef.insns) {
                                if (src is VarInsnNode) {
                                    if (src.`var` == 0 && !isStatic) {
                                        appendAffectedFields(insn)
                                    } else {
                                        appendAffectedVars(src)
                                    }
                                }
                            }
                        }
                    }
                }
                is VarInsnNode -> {
                    if (catchInsn == null || throwInsnFrame == null) continue
                    when (insn.opcode) {
                        Opcodes.ISTORE,
                        Opcodes.LSTORE,
                        Opcodes.FSTORE,
                        Opcodes.DSTORE,
                        Opcodes.ASTORE -> {
                            if (insn.`var` < throwInsnFrame.locals) {
                                appendAffectedVars(insn)
                            }
                        }
                    }
                }
            }
        }
    }

    // We may want to check if the source var is from
    // a map or an array.
    fun findVariableSource(varInsn: VarInsnNode,
                           frames: Array<Frame<SourceValue>?>,
                           throwInsnFrame: Frame<SourceValue>,
                           localVariableMap: MutableMap<Int, MutableList<Pair<Int, SourceValue>>>,
                           ) {
        val records = localVariableMap[varInsn.`var`] ?: return
        val index = instructions.indexOf(varInsn)
        var selectedRecord: Pair<Int, SourceValue>? = null
        for (record in records) {
            if (record.first > index) break
            selectedRecord = record
        }
        if (selectedRecord == null) {
            return
        }
        processSourceValue(
            instructions.get(selectedRecord.first),
            selectedRecord.second,
            frames,
            throwInsnFrame,
            localVariableMap
        )
    }

    fun processSourceValue(
        insn: AbstractInsnNode,
        objRef: SourceValue,
        frames: Array<Frame<SourceValue>?>,
        throwInsnFrame: Frame<SourceValue>,
        localVariableMap: MutableMap<Int, MutableList<Pair<Int, SourceValue>>>
    ) {
        for (src in objRef.insns) {
            val srcFrame = frames[instructions.indexOf(src)] ?: return
            when (src) {
                is MethodInsnNode -> {
                    if (src.opcode != Opcodes.INVOKESTATIC && src.opcode != Opcodes.INVOKEDYNAMIC) {
                        val srcRef = srcFrame.getStack(srcFrame.stackSize - Type.getArgumentTypes(src.desc).size - 1)
                        processSourceValue(src, srcRef , frames, throwInsnFrame, localVariableMap)
                    }
                }
                is VarInsnNode -> {
                    if (src.`var` == 0 && !isStatic) {
                        if (insn is FieldInsnNode) {
                            sourceField.add(insn.name)
                        } else {
                            sourceLocalVariable.add(src.`var`)
                            findVariableSource(src, frames, throwInsnFrame, localVariableMap)
                        }
                    } else {
                        if (throwInsnFrame.getLocal(src.`var`) == srcFrame.getLocal(src.`var`)) {
                            sourceLocalVariable.add(src.`var`)
                            findVariableSource(src, frames, throwInsnFrame, localVariableMap)
                        }
                    }
                }
                is FieldInsnNode -> {
                    when (src.opcode) {
                        Opcodes.GETFIELD -> {
                            val srcRef = srcFrame.getStack(srcFrame.stackSize - 1)
                            processSourceValue(src, srcRef, frames, throwInsnFrame, localVariableMap)
                        }
                        Opcodes.GETSTATIC -> {
                            sourceStaticField.add(src.owner + "#" + src.name)
                        }
                    }
                }
                is TypeInsnNode -> {
                    when (src.opcode) {
                        Opcodes.CHECKCAST,
                        Opcodes.INSTANCEOF -> {
                            processSourceValue(
                                src,
                                srcFrame.getStack(srcFrame.stackSize - 1),
                                frames,
                                throwInsnFrame,
                                localVariableMap
                            )
                        }
                    }
                }
                is InsnNode -> {
                    when (src.opcode) {
                        Opcodes.IADD,
                        Opcodes.ISUB,
                        Opcodes.IMUL,
                        Opcodes.IDIV,
                        Opcodes.LADD,
                        Opcodes.LSUB,
                        Opcodes.LMUL,
                        Opcodes.LDIV,
                        Opcodes.LCMP,
                        Opcodes.FADD,
                        Opcodes.FSUB,
                        Opcodes.FMUL,
                        Opcodes.FDIV,
                        Opcodes.FCMPG,
                        Opcodes.FCMPL,
                        Opcodes.DADD,
                        Opcodes.DSUB,
                        Opcodes.DMUL,
                        Opcodes.DDIV,
                        Opcodes.DCMPL,
                        Opcodes.DCMPG -> {
                            processSourceValue(
                                src,
                                srcFrame.getStack(srcFrame.stackSize - 1),
                                frames,
                                throwInsnFrame,
                                localVariableMap
                            )
                            processSourceValue(
                                src,
                                srcFrame.getStack(srcFrame.stackSize - 2),
                                frames,
                                throwInsnFrame,
                                localVariableMap
                            )
                        }
                        Opcodes.L2D,
                        Opcodes.L2F,
                        Opcodes.L2I,
                        Opcodes.D2F,
                        Opcodes.D2I,
                        Opcodes.D2L,
                        Opcodes.INEG,
                        Opcodes.I2B,
                        Opcodes.I2C,
                        Opcodes.I2D,
                        Opcodes.I2F,
                        Opcodes.I2L,
                        Opcodes.I2S,
                        Opcodes.F2D,
                        Opcodes.F2I,
                        Opcodes.F2L,
                        Opcodes.FNEG -> {
                            processSourceValue(
                                src,
                                srcFrame.getStack(srcFrame.stackSize - 1),
                                frames,
                                throwInsnFrame,
                                localVariableMap
                            )
                        }
                    }
                }
            }
        }
    }

    fun findSourceInsn(
        analyzer: AffectedVarAnalyser<SourceValue>,
        frames: Array<Frame<SourceValue>?>,
        localVariableMap: MutableMap<Int, MutableList<Pair<Int, SourceValue>>>
    ) {
        val throwInsnFrame = frames[instructions.indexOf(throwInsn)] ?: return
        val throwInsnLocal = throwInsn ?: return
        try {
            if (exception is ReflectiveOperationException) {
                return
            }
            if (isThrowInsn &&
                (exception is NullPointerException || exception is IndexOutOfBoundsException)
            ) {
                val objRef =
                    if (throwInsnLocal is FieldInsnNode) {
                        sourceLines.add(Pair(getLineNumber(throwInsnLocal), SourceType.FIELD))
                        when (throwInsnLocal.opcode) {
                            Opcodes.PUTFIELD -> {
                                throwInsnFrame.getStack(throwInsnFrame.stackSize - 2)
                            }
                            Opcodes.GETFIELD -> {
                                throwInsnFrame.getStack(throwInsnFrame.stackSize - 1)
                            }
                            else -> return
                        }
                    } else if (throwInsnLocal is MethodInsnNode) {
                        sourceLines.add(Pair(getLineNumber(throwInsnLocal), SourceType.INVOKE))
                        if (throwInsnLocal.opcode != Opcodes.INVOKEDYNAMIC &&
                            throwInsnLocal.opcode != Opcodes.INVOKESTATIC
                        ) {
                            throwInsnFrame.getStack(
                                throwInsnFrame.stackSize - Type.getArgumentTypes(throwInsnLocal.desc).size - 1
                            )
                        } else {
                            null
                        }
                    } else {
                        // TODO: handle array here
                        null
                    }
                if (objRef != null) {
                    processSourceValue(throwInsnLocal, objRef, frames, throwInsnFrame, localVariableMap)
                }
                return
            }

            if (exception is ClassNotFoundException &&
                throwInsnLocal is MethodInsnNode &&
                throwInsnLocal.name.contains("forName")
            ) {
                sourceLines.add(Pair(getLineNumber(throwInsnLocal), SourceType.INVOKE))
                return
            }

            // this is default behavior
            var currentInsn: AbstractInsnNode? = throwInsnLocal

            // We want to find the jump instruction that leads to the throw instruction.
            while (analyzer.instructionPredecessors[currentInsn]?.size == 1 &&
                currentInsn !is JumpInsnNode
            ) {
                currentInsn = analyzer.instructionPredecessors[currentInsn]?.first()
            }

            // If the instruction has two predecessors, we want to find the corresponding jump INSN.
            if (currentInsn !is JumpInsnNode &&
                analyzer.instructionPredecessors[currentInsn]?.size == 2
            ) {
                for (insn in analyzer.instructionPredecessors[currentInsn]!!) {
                    if (insn is JumpInsnNode) {
                        currentInsn = insn
                    }
                }
            }
            if (currentInsn is JumpInsnNode) {
                sourceLines.add(Pair(getLineNumber(currentInsn), SourceType.JUMP))
                val branchFrame = frames[instructions.indexOf(currentInsn)] ?: return
                val objRefs =
                    when (currentInsn.opcode) {
                        Opcodes.IFEQ,
                        Opcodes.IFNE,
                        Opcodes.IFLT,
                        Opcodes.IFGE,
                        Opcodes.IFGT,
                        Opcodes.IFLE -> {
                            listOf(branchFrame.getStack(branchFrame.stackSize - 1))
                        }
                        Opcodes.IF_ICMPEQ,
                        Opcodes.IF_ICMPNE,
                        Opcodes.IF_ICMPLT,
                        Opcodes.IF_ICMPGE,
                        Opcodes.IF_ICMPGT,
                        Opcodes.IF_ICMPLE -> {
                            listOf(
                                branchFrame.getStack(branchFrame.stackSize - 1),
                                branchFrame.getStack(branchFrame.stackSize - 2)
                            )
                        }
                        else -> emptyList()
                    }
                for (objRef in objRefs) {
                    processSourceValue(currentInsn, objRef, frames, throwInsnFrame, localVariableMap)
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            logger.error { "Failed to process throwInsn: $throwInsnLocal, $e" }
            return
        }
    }

    override fun visitEnd() {
        super.visitEnd()

        val tryCatchLocations = mutableListOf<Pair<Int, Int>>()
        for (tryCatchBlock in tryCatchBlocks) {
            if (tryCatchBlock.handler == catchInsn) {
                tryCatchLocations.add(
                    Pair(
                        instructions.indexOf(tryCatchBlock.start),
                        instructions.indexOf(tryCatchBlock.end)
                    )
                )
            }
        }
        if (throwInsn != null && (catchIndex == -1L || catchInsn != null)) {
            logger.info { "Identified thrown instruction: ${throwInsn?.opcode}" }
            val throwInsnIndex = instructions.indexOf(throwInsn)
            val interpreter = AffectedVarInterpreter(instructions, throwInsnIndex, tryCatchLocations)
            val analyzer = AffectedVarAnalyser(instructions, interpreter, throwInsn!!, catchInsn)
            analyzer.analyze(owner, this)
            val affectedInsns = interpreter.affectedInsns
            if (catchIndex != -1L) {
                val normalRunInterpreter = AffectedVarInterpreter(instructions, throwInsnIndex, emptyList())
                val normalRunAnalyzer =
                    AffectedVarAnalyser(instructions, normalRunInterpreter, throwInsn!!, null)
                normalRunAnalyzer.analyze(owner, this)
                for (insn in analyzer.reachableBlocks(catchInsn!!)) {
                    if (insn in normalRunInterpreter.affectedInsns &&
                        insn !in interpreter.affectedInsnInTry
                    ) {
                        affectedInsns.remove(insn)
                    }
                }
            }
            if (isThrowInsn) {
                affectedInsns.add(throwInsn!!)
            }
            processAffectedInsns(affectedInsns, analyzer.frames)
            if (findSource && !Constants.exceptionHelpers.contains(name + desc)) {
                findSourceInsn(analyzer, analyzer.frames, interpreter.localValueMap)
            }
        } else {
            logger.error {
                "Error finding throwInsn/catchInsn at index $throwIndex in class $owner:$name"
            }
        }
    }
}
