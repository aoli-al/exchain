package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static al.aoli.exchain.phosphor.instrumenter.Constants.methodNameMapping;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_NATIVE;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_STATIC;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ALOAD;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ARETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.DLOAD;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.DRETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.FLOAD;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.FRETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.IFEQ;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ILOAD;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.IRETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.LLOAD;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.LRETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.RETURN;

public class DynamicSwitchPostCV extends ClassVisitor {
    private String owner = null;
    public DynamicSwitchPostCV(ClassVisitor cv, boolean skipFrames, byte[] bytes) {
        super(ASM9, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        owner = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                     String[] exceptions) {
        if (name.endsWith(Constants.originMethodSuffix)
                || name.endsWith("PHOSPHOR_TAG")
                || (access & ACC_ABSTRACT) != 0
                || (access & ACC_NATIVE) != 0) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        String newName = methodNameMapping(name);
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        addSwitch(mv, newName, descriptor, (access & ACC_STATIC) != 0);
        return new ReplayMethodVisitor(
                access, name, descriptor,
                Collections.emptyList(),
                List.of(mv),
                List.of(super.visitMethod(access, newName + Constants.instrumentedMethodSuffix, descriptor, signature
                        , exceptions)));
    }

    void addBranch(MethodVisitor mv, Label label, String name, String descriptor, boolean isStatic) {
        mv.visitLabel(label);
        int offset;
        int insn;
        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 0);
            offset = 1;
            insn = INVOKEINTERFACE;
        } else {
            offset = 0;
            insn = INVOKESTATIC;
        }
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        for (int i = 0; i < argumentTypes.length; i++) {
            switch (argumentTypes[i].getSort()) {
                case Type.BOOLEAN, Type.BYTE, Type.CHAR,
                        Type.INT, Type.SHORT ->
                        mv.visitVarInsn(ILOAD, i + offset);
                case Type.LONG -> {
                    mv.visitVarInsn(LLOAD, i + offset);
                    offset += 1;
                }
                case Type.FLOAT ->
                    mv.visitVarInsn(FLOAD, i + offset);
                case Type.DOUBLE -> {
                    mv.visitVarInsn(DLOAD, i + offset);
                    offset += 1;
                }
                default -> mv.visitVarInsn(ALOAD, i + offset);

            }
        }
        mv.visitMethodInsn(insn, owner, name, descriptor, true);

        Type returnType = Type.getReturnType(descriptor);

        switch (returnType.getSort()) {
            case Type.BOOLEAN, Type.BYTE, Type.CHAR,
                    Type.INT, Type.SHORT ->
                    mv.visitInsn(IRETURN);
            case Type.LONG ->
                    mv.visitInsn(LRETURN);
            case Type.FLOAT ->
                    mv.visitInsn(FRETURN);
            case Type.DOUBLE ->
                    mv.visitInsn(DRETURN);
            case Type.VOID ->
                    mv.visitInsn(RETURN);
            default -> mv.visitInsn(ARETURN);

        }
    }

    void addSwitch(MethodVisitor mv, String name, String descriptor, boolean isStatic) {
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "al/aoli/exchain/instrumentation/runtime/ExceptionRuntime",
                "taintEnabled",
                "()Z",
                false
        );

        Label trueLabel = new Label();
        Label falseLabel = new Label();

        mv.visitJumpInsn(IFEQ, falseLabel);
        addBranch(mv, trueLabel, name + Constants.instrumentedMethodSuffix, descriptor, isStatic);
        addBranch(mv, falseLabel, name + Constants.originMethodSuffix, descriptor, isStatic);
    }
}
