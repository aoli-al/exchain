package al.aoli.exchain.phosphor.instrumenter;

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
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.F_NEW;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.IFEQ;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ILOAD;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.IRETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.LLOAD;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.LRETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.RETURN;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;

public class DynamicSwitchPostCV extends ClassVisitor {
    public static boolean defaultInline = true;

    private int index;
    private String owner = null;
    private boolean isInterface;
    private Map<String, InlineSwitchMethodVisitor> constructorVisitors = new HashMap<>();
    private Set<String> aggressivelyReduceMethodSize = new HashSet<>();

    public DynamicSwitchPostCV(ClassVisitor cv, boolean skipFrames, byte[] bytes) {
        super(ASM9, cv);
        ClassVisitor subCV = cv;
    }

    public void setAggressivelyReduceMethodSize(Set<String> aggressivelyReduceMethodSize) {
        this.aggressivelyReduceMethodSize = aggressivelyReduceMethodSize;
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
        isInterface = (access & ACC_INTERFACE) != 0;
        index = (name.hashCode() % 100_000 + 100_000) % 100_000;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        for (InlineSwitchMethodVisitor visitor : constructorVisitors.values()) {
            visitor.annotationOnly = false;
            if (!visitor.instrumentedNode.finished || !visitor.originNode.finished) {
                if (visitor.instrumentedNode.finished) {
                    visitor.instrumentedNode.accept(visitor.getMv());
                } else {
                    visitor.originNode.accept(visitor.getMv());
                }
            } else if (visitor.shouldInline
                    || (defaultInline
                            && !aggressivelyReduceMethodSize.contains(
                                    StringHelper.concat(visitor.name, visitor.descriptor)))) {
                visitor.isSecondPass = false;
                visitor.isInstrumentedCode = false;
                visitor.originNode.accept(
                        new ReflectionFixingMethodVisitor(visitor, owner, visitor.name));
                visitor.isInstrumentedCode = true;
                visitor.isSecondPass = true;
                visitor.instrumentedNode.accept(visitor);
            } else {
                String instrumentedMethodName =
                        StringHelper.concat(visitor.instrumentedNode.name, Integer.toString(index));
                MethodVisitor instrumentedMv =
                        super.visitMethod(
                                visitor.access,
                                instrumentedMethodName,
                                visitor.descriptor,
                                visitor.signature,
                                visitor.exceptions);
                visitor.instrumentedNode.accept(
                        new ReplayMethodVisitor(
                                visitor.access,
                                instrumentedMethodName,
                                visitor.descriptor,
                                List.of(),
                                List.of(),
                                List.of(instrumentedMv)));

                String originMethodName =
                        StringHelper.concat(visitor.originNode.name, Integer.toString(index));
                MethodVisitor originMv =
                        new ReflectionFixingMethodVisitor(
                                super.visitMethod(
                                        visitor.access,
                                        originMethodName,
                                        visitor.descriptor,
                                        visitor.signature,
                                        visitor.exceptions),
                                owner,
                                originMethodName);
                visitor.originNode.accept(
                        new ReplayMethodVisitor(
                                visitor.access,
                                originMethodName,
                                visitor.descriptor,
                                List.of(),
                                List.of(),
                                List.of(originMv)));
                visitor.getMv().visitCode();
                addSwitch(
                        visitor.getMv(),
                        instrumentedMethodName,
                        originMethodName,
                        visitor.descriptor,
                        (visitor.access & ACC_STATIC) != 0);
                visitor.getMv().visitEnd();
            }
        }
        constructorVisitors.clear();
        super.visitEnd();
    }

    void addBranch(
            MethodVisitor mv, Label label, String name, String descriptor, boolean isStatic) {
        mv.visitLabel(label);
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        edu.columbia.cs.psl.phosphor.struct.harmony.util.List<Object> locals =
                Utils.descriptorToLocals(descriptor);
        if (!isStatic) {
            locals.add(0, owner);
        }
        mv.visitFrame(F_NEW, locals.size(), locals.toArray(), 0, new String[] {});
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
        for (int i = 0; i < argumentTypes.length; i++) {
            switch (argumentTypes[i].getSort()) {
                case Type.BOOLEAN:
                case Type.BYTE:
                case Type.CHAR:
                case Type.INT:
                case Type.SHORT:
                    mv.visitVarInsn(ILOAD, i + offset);
                    break;
                case Type.LONG:
                    {
                        mv.visitVarInsn(LLOAD, i + offset);
                        offset += 1;
                        break;
                    }
                case Type.FLOAT:
                    mv.visitVarInsn(FLOAD, i + offset);
                    break;
                case Type.DOUBLE:
                    {
                        mv.visitVarInsn(DLOAD, i + offset);
                        offset += 1;
                        break;
                    }
                default:
                    mv.visitVarInsn(ALOAD, i + offset);
                    break;
            }
        }
        mv.visitMethodInsn(insn, owner, name, descriptor, isInterface);

        Type returnType = Type.getReturnType(descriptor);

        switch (returnType.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.INT:
            case Type.SHORT:
                mv.visitInsn(IRETURN);
                break;
            case Type.LONG:
                mv.visitInsn(LRETURN);
                break;
            case Type.FLOAT:
                mv.visitInsn(FRETURN);
                break;
            case Type.DOUBLE:
                mv.visitInsn(DRETURN);
                break;
            case Type.VOID:
                mv.visitInsn(RETURN);
                break;
            default:
                mv.visitInsn(ARETURN);
                break;
        }
    }

    void addSwitch(
            MethodVisitor mv,
            String instrumentedMethodName,
            String originMethodName,
            String descriptor,
            boolean isStatic) {
        Label enter = new Label();
        mv.visitLabel(enter);
        mv.visitLineNumber(50111, enter);
        mv.visitFieldInsn(
                Opcodes.GETSTATIC, "al/aoli/exchain/runtime/ExceptionJavaRuntime", "enabled", "Z");
        Label falseLabel = new Label();
        Label trueLabel = new Label();

        mv.visitJumpInsn(IFEQ, trueLabel);
        addBranch(mv, falseLabel, instrumentedMethodName, descriptor, isStatic);
        mv.visitLineNumber(50222, falseLabel);
        addBranch(mv, trueLabel, originMethodName, descriptor, isStatic);
        mv.visitLineNumber(50333, trueLabel);
        int locals = 0;
        if (isStatic) {
            locals += 1;
        }
        for (Type type : Type.getArgumentTypes(descriptor)) {
            switch (type.getSort()) {
                case Type.DOUBLE:
                case Type.LONG:
                    locals += 2;
                    break;
                default:
                    locals += 1;
                    break;
            }
        }
        mv.visitMaxs(Integer.max(locals, 1), locals);
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        if (name.endsWith("PHOSPHOR_TAG")
                || (access & ACC_ABSTRACT) != 0
                || (access & ACC_NATIVE) != 0) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        String newName = methodNameMapping(name);

        String key = StringHelper.concat(methodNameReMapping(newName), descriptor);
        if (!constructorVisitors.containsKey(key)) {
            constructorVisitors.put(
                    key,
                    new InlineSwitchMethodVisitor(
                            super.visitMethod(
                                    access,
                                    methodNameReMapping(newName),
                                    descriptor,
                                    signature,
                                    exceptions),
                            owner,
                            isInterface,
                            access,
                            methodNameReMapping(newName),
                            descriptor,
                            signature,
                            exceptions));
        }
        InlineSwitchMethodVisitor mv = constructorVisitors.get(key);
        if (name.contains(Constants.originMethodSuffix)) {
            return new ReplayMethodVisitor(
                    access, name, descriptor, List.of(), List.of(), List.of(mv.originNode));
        } else {
            return new ReplayMethodVisitor(
                    access, name, descriptor, List.of(), List.of(mv), List.of(mv.instrumentedNode));
        }
    }
}
