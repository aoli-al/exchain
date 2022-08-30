package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodNode;

import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;

public class ConstructorMethodNode extends MethodNode {
    private final ConstructorMethodVisitor visitor;
    public boolean finished = false;
    public ConstructorMethodNode(ConstructorMethodVisitor visitor) {
        super(ASM9);
        this.visitor = visitor;
    }

    @Override
    public void visitEnd() {
        finished = true;
        visitor.visitorFinished();
        super.visitEnd();
    }
}
