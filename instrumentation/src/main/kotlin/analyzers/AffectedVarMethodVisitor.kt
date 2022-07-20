package al.aoli.exchain.instrumentation.analyzers

import mu.KotlinLogging
import org.objectweb.asm.ConstantDynamic
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.SourceValue


private val logger = KotlinLogging.logger {}
class AffectedVarMethodVisitor(val throwIndex: Int, val catchIndex: Int, val owner: String, access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?):
    MethodNode(Opcodes.ASM8, access, name, descriptor, signature, exceptions) {
    var byteCodeOffset = 0;
    var throwInsn: AbstractInsnNode? = null
    var catchInsn: AbstractInsnNode? = null

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
        byteCodeOffset += if (varIndex < 4 && opcode != Opcodes.RET) {
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
        byteCodeOffset += if (opcode == Opcodes.INVOKEINTERFACE) {
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
            Opcodes.BIPUSH, Opcodes.NEWARRAY -> 2
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

    private fun processAffectedInsns(affectedInsns: Set<AbstractInsnNode>, frames: Array<Frame<SourceValue>>) {
        for (affectedInsn in affectedInsns) {
            val frame = frames[instructions.indexOf(affectedInsn)]
            when (affectedInsn) {
                is MethodInsnNode -> {
                    when (affectedInsn.opcode) {
                        INVOKESTATIC -> {
                            println("Invoke static ${affectedInsn.owner}:${affectedInsn.name}${affectedInsn.desc}")
                        }
                        else -> {
                            println("Invoke ${affectedInsn.owner}:${affectedInsn.name}${affectedInsn.desc}")
                        }
                    }
                }
            }
        }
    }

    override fun visitEnd() {
        super.visitEnd()
        val tryCatchLocations = mutableListOf<Pair<Int, Int>>()
        for (tryCatchBlock in tryCatchBlocks) {
            if (tryCatchBlock.handler == catchInsn) {
                tryCatchLocations.add(Pair(instructions.indexOf(tryCatchBlock.start), instructions.indexOf(tryCatchBlock.end)))
            }
        }
        if (throwInsn != null && (catchIndex == -1 || catchInsn != null)) {
            val interpreter = AffectedVarInterpreter(instructions, throwInsn!!, tryCatchLocations)
            val analyzer = AffectedVarAnalyser(instructions, interpreter, throwInsn!!, catchInsn)
            analyzer.analyze(owner, this)
            val affectedInsns = interpreter.affectedInsns
            if (catchIndex != -1) {
                val normalRunInterpreter = AffectedVarInterpreter(instructions, throwInsn!!, emptyList())
                val normalRunAnalyzer = AffectedVarAnalyser(instructions, normalRunInterpreter, throwInsn!!, null)
                normalRunAnalyzer.analyze(owner, this)
                for (insn in analyzer.reachableBlocks(catchInsn!!)) {
                    if (insn in normalRunInterpreter.affectedInsns && insn !in interpreter.affectedInsnInTry) {
                        affectedInsns.remove(insn)
                    }
                }
            }
            processAffectedInsns(affectedInsns, analyzer.frames)
        } else {
            logger.error { "Error finding throwInsn/catchInsn at index $throwIndex in class $owner:$name" }
        }
    }
}

