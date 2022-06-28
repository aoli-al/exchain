package al.aoli.exception.instrumentation.transformers

import al.aoli.exception.instrumentation.dataflow.Node
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM8
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.*

class DataFlowAnalyzer<V: Value>(interpreter: Interpreter<V>): Analyzer<V>(interpreter) {

    override fun newFrame(frame: Frame<out V>): Frame<V> {
        return Node(frame)
    }

    override fun newFrame(numLocals: Int, numStack: Int): Frame<V> {
        return Node(numLocals, numStack)
    }
    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        (frames[insnIndex] as Node).successors.add(frames[successorIndex] as Node)
        (frames[successorIndex] as Node).predecessors.add(frames[insnIndex] as Node)
        super.newControlFlowEdge(insnIndex, successorIndex)
    }

}

class CatchBlockTransformer(private val owner: String,
                            private val mv: MethodVisitor,
                            access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<String>?)
    : MethodNode(ASM8, access, name, descriptor, signature, exceptions) {
    override fun visitEnd() {
        super.visitEnd()
        val analyzer = DataFlowAnalyzer(BasicInterpreter())
        analyzer.analyze(owner, this)
        for (tryCatchBlock in this.tryCatchBlocks) {
            val startLoc = instructions.indexOf(tryCatchBlock.start)
            val startFrame = analyzer.frames[startLoc]
            val workItems = mutableListOf(startFrame)
            val tryReachedBlocks = mutableSetOf<Frame<BasicValue>>()
            while (workItems.isNotEmpty()) {
                val currentFrame = workItems.removeFirst()
                if (currentFrame in tryReachedBlocks) continue
                tryReachedBlocks.add(currentFrame)
                workItems.addAll((currentFrame as Node<BasicValue>).successors)
            }

            val handlerLoc = instructions.indexOf(tryCatchBlock.handler)
            val handlerFrame = analyzer.frames[handlerLoc]
            workItems.clear()
            workItems.add(handlerFrame)
            val handlerReachedBlocks = mutableSetOf<Frame<BasicValue>>()
            while (workItems.isNotEmpty()) {
                val currentFrame = workItems.removeFirst()
                if (currentFrame in handlerReachedBlocks) continue
                handlerReachedBlocks.add(currentFrame)
                workItems.addAll((currentFrame as Node<BasicValue>).successors)
            }

            val insts = mutableListOf<Int>()
            val endLocs = mutableListOf<Int>()
            for (frame in (handlerReachedBlocks - tryReachedBlocks)) {
                val node = frame as Node<BasicValue>
                val frameLoc = analyzer.frames.indexOf(frame)
                insts.add(frameLoc)
                val inst = instructions[frameLoc]
                if (node.successors.size != 1 || node.predecessors.size != 1) {
                    println(inst)
                }
            }
            insts.sort()
        }
        accept(mv)
    }
}