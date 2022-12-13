package al.aoli.exchain.phosphor.instrumenter;

import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_NATIVE;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.MethodNode;

public class UninstrumentedOriginPostCV extends ClassVisitor {

    public UninstrumentedOriginPostCV(ClassVisitor cv, boolean skipFrames, byte[] bytes) {
        super(ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        if ((access & ACC_ABSTRACT) != 0
                || (access & ACC_NATIVE) != 0
                || descriptor.contains(
                        "Ledu/columbia/cs/psl/phosphor/runtime/PhosphorStackFrame;")) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
        if (name.contains(Constants.originMethodSuffix)) {
            String originName = Constants.methodNameReMapping(name);
            return super.visitMethod(access, originName, descriptor, signature, exceptions);
        }
        return new MethodNode(access, name, descriptor, signature, exceptions);
    }
}
