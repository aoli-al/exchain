package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodNode;

import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;

public class InlineSwitchMethodNode extends MethodNode {
    public boolean finished = false;
    public InlineSwitchMethodNode(int access, String name, String descriptor,
                                  String signature, String[] exceptions) {
        super(ASM9, access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        finished = true;
        super.visitEnd();
    }
}
