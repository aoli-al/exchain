package al.aoli.exchain.instrumentation.transformers

import al.aoli.exchain.instrumentation.analyzers.DataFlowAnalyzer
import al.aoli.exchain.instrumentation.analyzers.ExceptionFlowAnalyzer
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.BasicInterpreter
import java.io.File

class CatchBlockTransformer(private val owner: String,
                            private val mv: MethodVisitor,
                            access: Int,
                            name: String,
                            descriptor: String,
                            signature: String?,
                            exceptions: Array<String>?)
    : MethodNode(ASM8, access, name, descriptor, signature, exceptions) {
    val modifiedInstructions = InsnList()

    private fun processCatchFrames(tryReachedBlocks: Set<AbstractInsnNode>, handlerReachedBlocks: Set<AbstractInsnNode>,
                                   instructionMap: Map<AbstractInsnNode, Set<AbstractInsnNode>>, handler: LabelNode) {
        var maxLabelIndex = 0
        var endInst: AbstractInsnNode? = null
        for (inst in (handlerReachedBlocks - tryReachedBlocks)) {
            if (instructions.indexOf(inst) > maxLabelIndex) {
                maxLabelIndex = instructions.indexOf(inst)
                endInst = inst
            }
            if (instructionMap.getOrDefault(inst, emptySet()).isEmpty()) {
                if (inst.opcode != ATHROW) {
                    instructions.insertBefore(inst, MethodInsnNode(
                        INVOKESTATIC,
                        "al/aoli/exchain/instrumentation/runtime/ExceptionRuntime",
                        "onCatch",
                        "()V"
                    ))
                }
            } else {
                for (successor in instructionMap[inst]!!) {
                    if (successor in tryReachedBlocks) {
                        assert(inst is JumpInsnNode)
                        if (inst is JumpInsnNode) {
                            if (successor == inst.label) {
                                val insnList = InsnList()
                                val label = LabelNode()
                                insnList.add(JumpInsnNode(GOTO, inst.next as LabelNode))
                                insnList.add(label)
                                insnList.add(
                                    MethodInsnNode(
                                        INVOKESTATIC,
                                        "al/aoli/exchain/instrumentation/runtime/ExceptionRuntime",
                                        "onCatch",
                                        "()V"
                                    )
                                )
                                insnList.add(JumpInsnNode(GOTO, inst.label))
                                inst.label = label
                                instructions.insert(inst, insnList)
                            } else {
                                instructions.insert(
                                    inst, MethodInsnNode(
                                        INVOKESTATIC,
                                        "al/aoli/exchain/instrumentation/runtime/ExceptionRuntime",
                                        "onCatch",
                                        "()V"
                                    )
                                )
                            }
                        } else {
                            instructions.insert(
                                inst, MethodInsnNode(
                                    INVOKESTATIC,
                                    "al/aoli/exchain/instrumentation/runtime/ExceptionRuntime",
                                    "onCatch",
                                    "()V"
                                )
                            )
                        }
                    }
                }
            }
        }

        val callCatch = InsnList()
        callCatch.add(InsnNode(DUP))
        callCatch.add(MethodInsnNode(
            INVOKESTATIC,
            "al/aoli/exchain/instrumentation/runtime/ExceptionRuntime",
            "onCatchBegin",
            "(Ljava/lang/Throwable;)V",
        ))

        instructions.insert(handler.next.next, callCatch)

        val end = LabelNode(InstrumentationLabel())
        val skip = LabelNode()
        val insnList = InsnList()
        insnList.add(JumpInsnNode(GOTO, skip))
        insnList.add(end)
        insnList.add(InsnNode(DUP))
        insnList.add(LdcInsnNode("$owner:$name"))
        insnList.add(MethodInsnNode(
            INVOKESTATIC,
            "al/aoli/exchain/instrumentation/runtime/ExceptionRuntime",
            "onCatchWithException",
            "(Ljava/lang/Throwable;Ljava/lang/String;)V",
        ))
        insnList.add(InsnNode(ATHROW))

        insnList.add(skip)
        instructions.insert(endInst, insnList)

        val tryCatchBlock = TryCatchBlockNode(handler, end, end, "java/lang/Throwable")
        newTryCatchBlocks.add(tryCatchBlock)
    }

    private val newTryCatchBlocks = ArrayList<TryCatchBlockNode>()

    override fun visitEnd() {
        super.visitEnd()

        val functionName = "$owner:$name:$desc"

        if (tryCatchBlocks.isNotEmpty()) {
            val analyzer = ExceptionFlowAnalyzer(instructions, BasicInterpreter())
            // DON'T RUN analyzer twice! It will modify the successors!
            analyzer.analyze(owner, this, false)
//            val exceptionResult = analyzer.analyze(owner, this, true)
//            val affectedVars = mutableSetOf<Int>()
//            for (index in normalResult.indices) {
//                if (normalResult[index] != null) {
//                    for (localIndex in 0 until normalResult[index].locals) {
//                        val normalLocal = normalResult[index].getLocal(localIndex)
//                        val exceptionLocal = exceptionResult[index].getLocal(localIndex)
//                        if (normalLocal != exceptionLocal) {
//                            for (insn in normalLocal.insns) {
//                                if (insn is VarInsnNode) {
//                                    affectedVars.add(insn.`var`)
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            if (affectedVars.isNotEmpty()) {
//                dataFlowOutput.appendText("Method $functionName: [${affectedVars.joinToString(", ")}]\n")
//            } else {
//                dataFlowOutput.appendText("Method $functionName: EMPTY\n")
//            }


            val processedHandlers = mutableSetOf<LabelNode>()
            for (tryCatchBlock in this.tryCatchBlocks) {
                if (tryCatchBlock.handler in processedHandlers) continue
                if (tryCatchBlock.type == null) continue
                processedHandlers.add(tryCatchBlock.handler)
                val tryReachedBlocks = analyzer.reachableBlocks(tryCatchBlock.start)
                val handlerReachedBlocks = analyzer.reachableBlocks(tryCatchBlock.handler)
                processCatchFrames(tryReachedBlocks, handlerReachedBlocks, analyzer.instructionSuccessors, tryCatchBlock.handler)
            }

            tryCatchBlocks?.addAll(newTryCatchBlocks)
        }



        accept(mv)
    }

    companion object {
        var varIndex = 0
        val dataFlowOutput = File("/tmp/data-flow.txt")
        fun newLocalVar(): String {
            return "LOCAL_VAR_${varIndex++}"
        }

        init {
            dataFlowOutput.writeText("")
        }

    }
}