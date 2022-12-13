package al.aoli.exchain.phosphor.instrumenter;

import static al.aoli.exchain.phosphor.instrumenter.Constants.methodNameMapping;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_NATIVE;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;

public class UninstrumentedOriginPreCV extends ClassVisitor {
    private String owner;

    public UninstrumentedOriginPreCV(ClassVisitor cv, boolean skipFrames) {
        super(ASM9, cv);
    }

    @Override
    public void visit(
            int version,
            int access,
            String name,
            String signature,
            String superName,
            String[] interfaces) {
        owner = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        if (name.contains("Exchain")) {
            return null;
        }
        String newName = methodNameMapping(name);
        MethodVisitor mv2 = super.visitMethod(access, name, descriptor, signature, exceptions);
        if ((access & ACC_ABSTRACT) != 0 || (access & ACC_NATIVE) != 0) {
            return mv2;
        }
        MethodVisitor mv1 =
                super.visitMethod(
                        access,
                        StringHelper.concat(newName, Constants.originMethodSuffix),
                        descriptor,
                        signature,
                        exceptions);
        return new ReplayMethodVisitor(
                access, name, descriptor, List.of(mv2, mv1), List.of(), List.of());
    }
}
