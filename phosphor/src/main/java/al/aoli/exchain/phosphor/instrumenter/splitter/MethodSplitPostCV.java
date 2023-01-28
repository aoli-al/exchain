package al.aoli.exchain.phosphor.instrumenter.splitter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodNode;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;

import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;

public class MethodSplitPostCV extends ClassVisitor {

    private Set<String> aggressivelyReduceMethodSize = new HashSet<>();
    private Set<MethodNode> methodNodes = new HashSet<>();
    private String owner;

    public MethodSplitPostCV(ClassVisitor cv, boolean skipFrames, byte[] bytes) {
        super(ASM9, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        owner = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (aggressivelyReduceMethodSize != null && aggressivelyReduceMethodSize.contains(name + descriptor)) {
            MethodNode node = new MethodNode(access, name, descriptor, signature, exceptions);
            methodNodes.add(node);
            return node;
        } else {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    @Override
    public void visitEnd() {
        for (MethodNode node: methodNodes) {
            SplitMethod.Result result = new SplitMethod(ASM9).split(owner, node);
            result.trimmedMethod.accept(this.cv);
            result.splitOffMethod.accept(this.cv);
        }
        super.visitEnd();
    }
}
