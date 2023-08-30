package al.aoli.exchain.phosphor.instrumenter;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.*;

import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import java.lang.reflect.Method;

public class ReflectionFixingMethodVisitor extends MethodVisitor {
    private final boolean patchAnonymousClasses;
    private String className;
    private String methodName;

    public ReflectionFixingMethodVisitor(
            MethodVisitor methodVisitor, String owner, String methodName) {
        super(ASM9, methodVisitor);
        className = owner;
        this.methodName = methodName;
        this.patchAnonymousClasses = owner.equals("java/lang/invoke/InnerClassLambdaMetafactory");
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        if (this.className.equals("java/lang/invoke/MethodHandles$Lookup")
                && this.methodName.startsWith("defineHiddenClass")) {
            super.visitVarInsn(ALOAD, 1);
            INSTRUMENT_CLASS_BYTES.delegateVisit(mv);
            super.visitVarInsn(ASTORE, 1);
        }
    }

    @Override
    public void visitMethodInsn(
            int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        if (patchAnonymousClasses
                && name.equals("defineAnonymousClass")
                && Instrumenter.isUnsafeClass(owner)
                && descriptor.equals("(Ljava/lang/Class;[B[Ljava/lang/Object;)Ljava/lang/Class;")) {
            super.visitInsn(SWAP);
            INSTRUMENT_CLASS_BYTES.delegateVisit(mv);
            super.visitInsn(SWAP);
        }
        if (owner.equals("java/lang/Class")
                && name.endsWith("Methods")
                && !className.equals(owner)
                && descriptor.equals(
                        StringHelper.concat("()", Type.getDescriptor(Method[].class)))) {
            visit(REMOVE_TAINTED_METHODS);
        } else if (owner.equals("java/lang/Class") && name.equals("getInterfaces")) {
            visit(REMOVE_TAINTED_INTERFACES);
        } else if (owner.equals("java/lang/Class")
                && name.endsWith("Fields")
                && !className.equals("java/lang/Class")) {
            visit(REMOVE_TAINTED_FIELDS);
        }
    }

    private void visit(TaintMethodRecord method) {
        super.visitMethodInsn(
                method.getOpcode(),
                method.getOwner(),
                method.getName(),
                method.getDescriptor(),
                method.isInterface());
    }
}
