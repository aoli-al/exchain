package al.aoli.exchain.phosphor.instrumenter;

import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;

public class UninstrumentedOriginPostCV extends ClassVisitor {

    public UninstrumentedOriginPostCV(ClassVisitor cv, boolean skipFrames, byte[] bytes) {
        super(ASM9, cv);
    }

    private String owner = null;

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
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new ReflectionFixingMethodVisitor(mv, owner, name);
    }
}
