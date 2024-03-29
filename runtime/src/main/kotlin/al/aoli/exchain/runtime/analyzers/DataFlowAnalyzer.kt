package al.aoli.exchain.runtime.analyzers

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.analysis.Value

open class DataFlowAnalyzer<V : Value>(val instructions: InsnList, interpreter: Interpreter<V>) :
    Analyzer<V>(interpreter) {
    val indexSuccessors = mutableMapOf<Int, MutableSet<Int>>()
    val instructionSuccessors = mutableMapOf<AbstractInsnNode, MutableSet<AbstractInsnNode>>()
    val instructionPredecessors = mutableMapOf<AbstractInsnNode, MutableSet<AbstractInsnNode>>()

    fun reachableBlocks(from: AbstractInsnNode): MutableSet<AbstractInsnNode> {
        val workItems = mutableListOf(from)
        val reachedBlocks = mutableSetOf<AbstractInsnNode>()
        while (workItems.isNotEmpty()) {
            val currentInstruction = workItems.removeFirst()
            if (currentInstruction in reachedBlocks) continue
            reachedBlocks.add(currentInstruction)
            workItems.addAll(instructionSuccessors.getOrDefault(currentInstruction, emptySet()))
        }
        return reachedBlocks
    }

    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        indexSuccessors.getOrPut(insnIndex) { mutableSetOf() }.add(successorIndex)
        instructionSuccessors
            .getOrPut(instructions[insnIndex]) { mutableSetOf() }
            .add(instructions[successorIndex])
        instructionPredecessors
            .getOrPut(instructions[successorIndex]) { mutableSetOf() }
            .add(instructions[insnIndex])
        super.newControlFlowEdge(insnIndex, successorIndex)
    }
}
