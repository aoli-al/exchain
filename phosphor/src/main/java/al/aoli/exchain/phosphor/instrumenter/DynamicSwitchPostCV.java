package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;

import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;

public class DynamicSwitchPostCV extends ClassVisitor {
    private String owner = null;
    public DynamicSwitchPostCV(ClassVisitor cv, boolean skipFrames, byte[] bytes) {
        super(ASM9);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        owner = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                     String[] exceptions) {
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
