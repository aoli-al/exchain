package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;

import java.util.Collections;
import java.util.List;

import static al.aoli.exchain.phosphor.instrumenter.Constants.instrumentedMethodSuffix;
import static al.aoli.exchain.phosphor.instrumenter.Constants.methodNameMapping;
import static al.aoli.exchain.phosphor.instrumenter.Constants.methodNameReMapping;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_INTERFACE;
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
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.IRETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.LLOAD;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.LRETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.RETURN;

public class DynamicSwitchPostCV extends ClassVisitor {

    private static int index;
    private String owner = null;
    private boolean isInterface;
    private Map<String, InlineSwitchMethodVisitor> constructorVisitors = new HashMap<>();
    public DynamicSwitchPostCV(ClassVisitor cv, boolean skipFrames, byte[] bytes) {
        super(ASM9, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        owner = name;
        isInterface = (access & ACC_INTERFACE) != 0;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        for (InlineSwitchMethodVisitor visitor: constructorVisitors.values()) {
            if (!visitor.instrumentedNode.finished || !visitor.originNode.finished) {
                if (visitor.instrumentedNode.finished) {
                    visitor.instrumentedNode.accept(visitor.getMv());
                } else {
                    visitor.originNode.accept(visitor.getMv());
                }
            } else if (visitor.shouldInline) {
                visitor.originNode.accept(visitor);
                visitor.isInstrumentedCode = true;
                visitor.isSecondPass = true;
                visitor.instrumentedNode.accept(visitor);
            } else {
                String instrumentedMethodName = visitor.instrumentedNode.name + index++;
                MethodVisitor instrumentedMv = super.visitMethod(visitor.access,
                        instrumentedMethodName, visitor.descriptor, visitor.signature, visitor.exceptions);
                visitor.instrumentedNode.accept(new ReplayMethodVisitor(
                        visitor.access, visitor.instrumentedNode.name, visitor.descriptor,
                        Collections.emptyList(), Collections.emptyList(),
                        List.of(instrumentedMv)));
                MethodVisitor originMv = super.visitMethod(visitor.access, visitor.originNode.name,
                        visitor.descriptor, visitor.signature, visitor.exceptions);

                String originMethodName = visitor.originNode.name + index++;
                visitor.originNode.accept(new ReplayMethodVisitor(
                        visitor.access, originMethodName, visitor.descriptor,
                        Collections.emptyList(), List.of(),
                        List.of(originMv)));
                visitor.getMv().visitCode();
                addSwitch(visitor.getMv(), instrumentedMethodName, originMethodName,
                        visitor.descriptor, (visitor.access & ACC_STATIC) != 0);
                visitor.getMv().visitEnd();
            }
        }
        constructorVisitors.clear();
        super.visitEnd();
    }

    void addBranch(MethodVisitor mv, Label label, String name, String descriptor, boolean isStatic) {
        mv.visitLabel(label);
        int offset;
        int insn;
        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 0);
            offset = 1;
            if (isInterface) {
                insn = INVOKEINTERFACE;
            } else {
                insn = INVOKEVIRTUAL;
            }
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
        mv.visitMethodInsn(insn, owner, name, descriptor, isInterface);

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

    void addSwitch(MethodVisitor mv, String instrumentedMethodName, String originMethodName,
                   String descriptor, boolean isStatic) {
        Label enter = new Label();
        mv.visitLabel(enter);
        mv.visitLineNumber(111, enter);
        mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                "al/aoli/exchain/runtime/ExceptionJavaRuntime",
                "enabled",
                "Z"
        );
        Label trueLabel = new Label();
        Label falseLabel = new Label();

        mv.visitJumpInsn(IFEQ, falseLabel);
        addBranch(mv, trueLabel, instrumentedMethodName, descriptor, isStatic);
        mv.visitLineNumber(222, trueLabel);
        addBranch(mv, falseLabel, originMethodName, descriptor, isStatic);
        mv.visitLineNumber(333, falseLabel);
        int locals = 0;
        if (isStatic) {
            locals += 1;
        }
        for (Type type: Type.getArgumentTypes(descriptor)) {
            switch (type.getSort()) {
                case Type.DOUBLE, Type.LONG ->
                        locals += 2;
                default -> locals += 1;
            }
        }
        mv.visitMaxs(Integer.max(locals, 1), locals);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                     String[] exceptions) {
        if (name.endsWith("PHOSPHOR_TAG")
                || (access & ACC_ABSTRACT) != 0
                || (access & ACC_NATIVE) != 0) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        String newName = methodNameMapping(name);

        String key = owner + methodNameReMapping(newName) + descriptor;
        if (!constructorVisitors.containsKey(key)) {
            constructorVisitors.put(key,
                    new InlineSwitchMethodVisitor(
                            super.visitMethod(access, methodNameReMapping(newName), descriptor, signature, exceptions),
                            owner, isInterface,
                            access, methodNameReMapping(newName), descriptor, signature, exceptions));
        }
        InlineSwitchMethodVisitor mv = constructorVisitors.get(key);
        if (name.contains(Constants.originMethodSuffix)) {
            return new ReplayMethodVisitor(access, name, descriptor, Collections.emptyList(),
                    List.of(),
                    List.of(mv.originNode));
        } else {
            return new ReplayMethodVisitor(access, name, descriptor, Collections.emptyList(),
                    List.of(mv),
                    List.of(mv.instrumentedNode));
        }
    }

}
