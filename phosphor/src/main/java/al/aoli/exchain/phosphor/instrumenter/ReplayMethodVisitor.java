package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.AnnotationVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Attribute;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Handle;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.TypePath;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.List;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;

import java.util.stream.Stream;


public class ReplayMethodVisitor extends MethodVisitor {

    List<MethodVisitor> mvs;
    List<MethodVisitor> annotationOnlyMvs;

    List<MethodVisitor> codeOnlyMvs;

    int access;
    String name;
    String descriptor;

    static Map<Label, Label> labelMapping = new HashMap<>();

    public ReplayMethodVisitor(
            int access, String name, String descriptor,
            List<MethodVisitor> mvs, List<MethodVisitor> annotationOnlyMvs,
            List<MethodVisitor> codeOnlyMvs) {
        super(Opcodes.ASM9);
        this.access = access;
        this.name = name;
        this.descriptor = descriptor;
        this.mvs = mvs;
        this.annotationOnlyMvs = annotationOnlyMvs;
        this.codeOnlyMvs = codeOnlyMvs;
    }

    @Override
    public void visitParameter(String name, int access) {
        for (MethodVisitor mv: mvs) {
            mv.visitParameter(name, access);
        }
        for (MethodVisitor mv : annotationOnlyMvs) {
            mv.visitParameter(name, access);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitParameter(name, access);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        List<AnnotationVisitor> result = new ArrayList<>();
        for (MethodVisitor mv: mvs) {
            result.add(mv.visitAnnotationDefault());
        }
        for (MethodVisitor annotationOnlyMv : annotationOnlyMvs) {
            result.add(annotationOnlyMv.visitAnnotationDefault());
        }
        return new ReplayAnnotationVisitor(result);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        List<AnnotationVisitor> result = new ArrayList<>();
        for (MethodVisitor mv: mvs) {
            result.add(mv.visitAnnotation(descriptor, visible));
        }
        for (MethodVisitor annotationOnlyMv : annotationOnlyMvs) {
            result.add(annotationOnlyMv.visitAnnotation(descriptor, visible));
        }
        return new ReplayAnnotationVisitor(result);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        List<AnnotationVisitor> result = new ArrayList<>();
        for (MethodVisitor mv: mvs) {
            result.add(mv.visitTypeAnnotation(typeRef, typePath, descriptor, visible));
        }
        for (MethodVisitor annotationOnlyMv : annotationOnlyMvs) {
            result.add(annotationOnlyMv.visitTypeAnnotation(typeRef, typePath, descriptor, visible));
        }
        return new ReplayAnnotationVisitor(result);
    }

    @Override
    public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
        for (MethodVisitor mv: mvs) {
            mv.visitAnnotableParameterCount(parameterCount, visible);
        }
        for (MethodVisitor mv: annotationOnlyMvs) {
            mv.visitAnnotableParameterCount(parameterCount, visible);
        }
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        List<AnnotationVisitor> result = new ArrayList<>();
        for (MethodVisitor mv: mvs) {
            result.add(mv.visitParameterAnnotation(parameter, descriptor, visible));
        }
        for (MethodVisitor annotationOnlyMv : annotationOnlyMvs) {
            result.add(annotationOnlyMv.visitParameterAnnotation(parameter, descriptor, visible));
        }
        return new ReplayAnnotationVisitor(result);
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        for (MethodVisitor mv : mvs) {
            mv.visitAttribute(attribute);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitAttribute(attribute);
        }
    }

    @Override
    public void visitCode() {
        for (MethodVisitor mv : mvs) {
            mv.visitCode();
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitCode();
        }
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        for (MethodVisitor mv : mvs) {
            mv.visitFrame(type, numLocal, local, numStack, stack);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitFrame(type, numLocal, local, numStack, stack);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        for (MethodVisitor mv : mvs) {
            mv.visitInsn(opcode);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitInsn(opcode);
        }
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        for (MethodVisitor mv : mvs) {
            mv.visitIntInsn(opcode, operand);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitIntInsn(opcode, operand);
        }
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        for (MethodVisitor mv : mvs) {
            mv.visitVarInsn(opcode, var);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitVarInsn(opcode, var);
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        for (MethodVisitor mv: mvs) {
            mv.visitTypeInsn(opcode, type);
        }
        for (MethodVisitor mv: codeOnlyMvs) {
            mv.visitTypeInsn(opcode, type);
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        for (MethodVisitor mv : mvs) {
            mv.visitFieldInsn(opcode, owner, name, descriptor);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitFieldInsn(opcode, owner, name, descriptor);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        for (MethodVisitor mv : mvs) {
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
        for (MethodVisitor mv : mvs) {
            mv.visitMethodInsn(opcode, owner, name, descriptor);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitMethodInsn(opcode, owner, name, descriptor);
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
                                       Object... bootstrapMethodArguments) {
        for (MethodVisitor mv : mvs) {
            mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }
    }

    private Label[] checkLabel(Label... labels) {
        if (mvs.isEmpty() || codeOnlyMvs.isEmpty()) return labels;
        List<Label> result = new ArrayList<>();
        for (Label label : labels) {
            result.add(checkLabel(label));
        }
        return result.toArray(new Label[labels.length]);
    }

    private Label checkLabel(Label label) {
        if (mvs.isEmpty() || codeOnlyMvs.isEmpty()) return label;
        return label;
//        if (!labelMapping.containsKey(label)) {
//            labelMapping.put(label, new Label());
//        }
//        return labelMapping.get(label);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        for (MethodVisitor mv : mvs) {
            mv.visitJumpInsn(opcode, label);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitJumpInsn(opcode, checkLabel(label));
        }
    }

    @Override
    public void visitLabel(Label label) {
        for (MethodVisitor mv : mvs) {
            mv.visitLabel(label);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitLabel(checkLabel(label));
        }
    }

    @Override
    public void visitLdcInsn(Object value) {
        for (MethodVisitor mv : mvs) {
            mv.visitLdcInsn(value);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitLdcInsn(value);
        }
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        for (MethodVisitor mv: mvs) {
            mv.visitIincInsn(var, increment);
        }
        for (MethodVisitor mv: codeOnlyMvs) {
            mv.visitIincInsn(var, increment);
        }
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        for (MethodVisitor mv: mvs) {
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }
        for (MethodVisitor mv: codeOnlyMvs) {
            mv.visitTableSwitchInsn(min, max, checkLabel(dflt), checkLabel(labels));
        }
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        for (MethodVisitor mv : mvs) {
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitLookupSwitchInsn(checkLabel(dflt), keys, checkLabel(labels));
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        for (MethodVisitor mv : mvs) {
            mv.visitMultiANewArrayInsn(descriptor, numDimensions);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitMultiANewArrayInsn(descriptor, numDimensions);
        }
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        List<AnnotationVisitor> result = new ArrayList<>();
        for (MethodVisitor mv: mvs) {
            result.add(mv.visitInsnAnnotation(typeRef, typePath, descriptor, visible));
        }
        for (MethodVisitor annotationOnlyMv : codeOnlyMvs) {
            result.add(annotationOnlyMv.visitInsnAnnotation(typeRef, typePath, descriptor, visible));
        }
        return new ReplayAnnotationVisitor(result);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        for (MethodVisitor mv : mvs) {
            mv.visitTryCatchBlock(start, end, handler, type);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitTryCatchBlock(checkLabel(start), checkLabel(end), checkLabel(handler), type);
        }
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor,
                                                     boolean visible) {
        List<AnnotationVisitor> result = new ArrayList<>();
        for (MethodVisitor mv: mvs) {
            result.add(mv.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible));
        }
        for (MethodVisitor annotationOnlyMv : codeOnlyMvs) {
            result.add(annotationOnlyMv.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible));
        }
        return new ReplayAnnotationVisitor(result);
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end,
                                                          int[] index, String descriptor, boolean visible) {
        List<AnnotationVisitor> result = new ArrayList<>();
        for (MethodVisitor mv: mvs) {
            result.add(mv.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible));
        }
        for (MethodVisitor annotationOnlyMv : codeOnlyMvs) {
            result.add(annotationOnlyMv.visitLocalVariableAnnotation(typeRef, typePath, checkLabel(start),
                    checkLabel(end), index, descriptor, visible));
        }
        return new ReplayAnnotationVisitor(result);
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end,
                                   int index) {
        for (MethodVisitor mv : mvs) {
            mv.visitLocalVariable(name, descriptor, signature, start, end, index);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitLocalVariable(name, descriptor, signature, checkLabel(start), checkLabel(end), index);
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        for (MethodVisitor mv : mvs) {
            mv.visitLineNumber(line, start);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitLineNumber(line, checkLabel(start));
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        for (MethodVisitor mv: mvs) {
            mv.visitMaxs(maxStack, maxLocals);
        }
        for (MethodVisitor mv: codeOnlyMvs) {
            mv.visitMaxs(maxStack, maxLocals);
        }
        for (MethodVisitor mv: annotationOnlyMvs) {
            mv.visitMaxs(maxStack, maxLocals);
        }
    }

    @Override
    public void visitEnd() {
        for (MethodVisitor mv: mvs) {
            mv.visitEnd();
        }
        for (MethodVisitor mv: codeOnlyMvs) {
            mv.visitEnd();
        }
    }
}
