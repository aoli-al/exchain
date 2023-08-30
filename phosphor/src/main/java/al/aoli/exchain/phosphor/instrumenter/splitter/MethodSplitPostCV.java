package al.aoli.exchain.phosphor.instrumenter.splitter;

import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.FieldVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.AbstractInsnNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodNode;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;

public class MethodSplitPostCV extends ClassVisitor {

    public Set<String> aggressivelyReduceMethodSize = new HashSet<>();
    private Set<MethodNode> methodNodes = new HashSet<>();
    private String owner;

    public MethodSplitPostCV(ClassVisitor cv, boolean skipFrames, byte[] bytes) {
        super(ASM9, cv);
    }

    @Override
    public FieldVisitor visitField(
            int access, String name, String descriptor, String signature, Object value) {
        if (aggressivelyReduceMethodSize != null && !aggressivelyReduceMethodSize.isEmpty()) {
            access &= (~Opcodes.ACC_FINAL);
        }
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public void visit(
            int version,
            int access,
            String name,
            String signature,
            String superName,
            String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        owner = name;
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        if (aggressivelyReduceMethodSize != null
                && aggressivelyReduceMethodSize.contains(name + descriptor)) {
            MethodNode node = new MethodNode(access, name, descriptor, signature, exceptions);
            methodNodes.add(node);
            return node;
        } else {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    public void splitMethodRecursive(MethodNode node) {
        if (node.instructions.size() > 15000) {
            int size = node.instructions.size();
            int minSize = (int) (size * 0.2) + 1;
            int maxSize = (int) (size * 0.6) + 1;
            SplitMethod.Result result =
                    new SplitMethod(ASM9).split(owner, node, minSize, maxSize, maxSize);
            if (result == null) {
                node.accept(this.cv);
                return;
            }
            splitMethodRecursive(result.trimmedMethod);
            int i = 0;
            AbstractInsnNode start = result.splitOffMethod.instructions.getFirst();
            while (start != null) {
                start = start.getNext();
                i += 1;
            }

            System.out.println(i);
            splitMethodRecursive(result.splitOffMethod);
        } else {
            node.accept(this.cv);
        }
    }

    @Override
    public void visitEnd() {
        for (MethodNode node : methodNodes) {
            splitMethodRecursive(node);
        }
        super.visitEnd();
    }
}
