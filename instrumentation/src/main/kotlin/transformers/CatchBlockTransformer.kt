package al.aoli.exception.instrumentation.transformers

import al.aoli.exception.instrumentation.dataflow.InstrumentationLabel
import com.sun.jdi.LocalVariable
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.*

class DataFlowAnalyzer<V: Value>(interpreter: Interpreter<V>): Analyzer<V>(interpreter) {
    val successors = mutableMapOf<Int, MutableSet<Int>>()
    val predecessors = mutableMapOf<Int, MutableSet<Int>>()

    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        successors.getOrPut(insnIndex, { mutableSetOf() }).add(successorIndex)
        super.newControlFlowEdge(insnIndex, successorIndex)
    }

}

class CatchBlockTransformer(private val owner: String,
                            private val mv: MethodVisitor,
                            access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<String>?)
    : MethodNode(ASM8, access, name, descriptor, signature, exceptions) {
    val modifiedInstructions = InsnList()

    fun processCatchFrames(tryReachedBlocks: Set<AbstractInsnNode>, handlerReachedBlocks: Set<AbstractInsnNode>,
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
                        "al/aoli/exception/instrumentation/runtime/ExceptionRuntime",
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
                                        "al/aoli/exception/instrumentation/runtime/ExceptionRuntime",
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
                                        "al/aoli/exception/instrumentation/runtime/ExceptionRuntime",
                                        "onCatch",
                                        "()V"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        val end = LabelNode(InstrumentationLabel())
        val skip = LabelNode()
        val insnList = InsnList()
        insnList.add(JumpInsnNode(GOTO, skip))
        insnList.add(end)

        insnList.add(MethodInsnNode(
            INVOKESTATIC,
            "al/aoli/exception/instrumentation/runtime/ExceptionRuntime",
            "onCatchWithException",
            "(Ljava/lang/Throwable;)V",
        ))

        insnList.add(skip)
        instructions.insert(endInst, insnList)

        val tryCatchBlock = TryCatchBlockNode(handler, end, end, "java/lang/Throwable")
        newTryCatchBlocks.add(tryCatchBlock)
    }

    private val newTryCatchBlocks = ArrayList<TryCatchBlockNode>()

    private fun computeReachBlocks(inst: AbstractInsnNode, instructionMap: Map<AbstractInsnNode, Set<AbstractInsnNode>>): Set<AbstractInsnNode> {
        val workItems = mutableListOf(inst)
        val reachedBlocks = mutableSetOf<AbstractInsnNode>()
        while (workItems.isNotEmpty()) {
            val currentInstruction = workItems.removeFirst()
            if (currentInstruction in reachedBlocks) continue
            reachedBlocks.add(currentInstruction)
            workItems.addAll(instructionMap.getOrDefault(currentInstruction, emptySet()))
        }
        return reachedBlocks
    }

    override fun visitEnd() {
        super.visitEnd()

//        modifiedInstructions.add(instructions)

        val analyzer = DataFlowAnalyzer(BasicInterpreter())
        analyzer.analyze(owner, this)

        val instructionMap = analyzer.successors.map { entry ->
            Pair(instructions[entry.key], entry.value.map { instructions[it] }.toSet())
        }.toMap()


        for (tryCatchBlock in this.tryCatchBlocks) {
            val tryReachedBlocks = computeReachBlocks(tryCatchBlock.start, instructionMap)
            val handlerReachedBlocks = computeReachBlocks(tryCatchBlock.handler, instructionMap)
            processCatchFrames(tryReachedBlocks, handlerReachedBlocks, instructionMap, tryCatchBlock.handler)
        }

        tryCatchBlocks?.addAll(0, newTryCatchBlocks)

        accept(mv)
    }
}