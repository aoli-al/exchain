package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.AnnotationVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Attribute;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Handle;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.TypePath;

import java.util.Arrays;
import java.util.List;

public class ReplayMethodVisitor extends MethodVisitor {
    List<MethodVisitor> mvs;

    public ReplayMethodVisitor(List<MethodVisitor> mvs) {
        super(Opcodes.ASM9);
        this.mvs = mvs;
    }

    public ReplayMethodVisitor(MethodVisitor ...mvs) {
        this(Arrays.stream(mvs).toList());
    }

    @Override
    public void visitParameter(String name, int access) {
        for (MethodVisitor mv: mvs) {
            mv.visitParameter(name, access);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return new ReplayAnnotationVisitor(mvs.stream().map(MethodVisitor::visitAnnotationDefault).toList());
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return new ReplayAnnotationVisitor(mvs.stream().map(it -> it.visitAnnotation(descriptor, visible)).toList());
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        return new ReplayAnnotationVisitor(mvs.stream()
                .map(it -> it.visitTypeAnnotation(typeRef, typePath, descriptor, visible)).toList());
    }

    @Override
    public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
        for (MethodVisitor mv: mvs) {
            mv.visitAnnotableParameterCount(parameterCount, visible);
        }
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        return new ReplayAnnotationVisitor(mvs.stream().map(it -> it.visitParameterAnnotation(parameter, descriptor,
                visible)).toList());
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        for (MethodVisitor mv : mvs) {
            mv.visitAttribute(attribute);
        }
    }

    @Override
    public void visitCode() {
        for (MethodVisitor mv : mvs) {
            mv.visitCode();
        }
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        for (MethodVisitor mv : mvs) {
            mv.visitFrame(type, numLocal, local, numStack, stack);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        for (MethodVisitor mv : mvs) {
            mv.visitInsn(opcode);
        }
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        for (MethodVisitor mv : mvs) {
            mv.visitIntInsn(opcode, operand);
        }
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        for (MethodVisitor mv : mvs) {
            mv.visitVarInsn(opcode, var);
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        for (MethodVisitor mv: mvs) {
            mv.visitTypeInsn(opcode, type);
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        for (MethodVisitor mv : mvs) {
            mv.visitFieldInsn(opcode, owner, name, descriptor);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        for (MethodVisitor mv : mvs) {
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
        for (MethodVisitor mv : mvs) {
            mv.visitMethodInsn(opcode, owner, name, descriptor);
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
                                       Object... bootstrapMethodArguments) {
        for (MethodVisitor mv : mvs) {
            mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        for (MethodVisitor mv : mvs) {
            mv.visitJumpInsn(opcode, label);
        }
    }

    @Override
    public void visitLabel(Label label) {
        for (MethodVisitor mv : mvs) {
            mv.visitLabel(label);
        }
    }

    @Override
    public void visitLdcInsn(Object value) {
        for (MethodVisitor mv : mvs) {
            mv.visitLdcInsn(value);
        }
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        for (MethodVisitor mv: mvs) {
            mv.visitIincInsn(var, increment);
        }
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        for (MethodVisitor mv: mvs) {
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        for (MethodVisitor mv : mvs) {
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        for (MethodVisitor mv : mvs) {
            mv.visitMultiANewArrayInsn(descriptor, numDimensions);
        }
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        return new ReplayAnnotationVisitor(mvs.stream().map(
                it -> it.visitInsnAnnotation(typeRef, typePath, descriptor, visible)
        ).toList());
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        for (MethodVisitor mv : mvs) {
            mv.visitTryCatchBlock(start, end, handler, type);
        }
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor,
                                                     boolean visible) {
        return new ReplayAnnotationVisitor(mvs.stream().map(
                it -> it.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible)
        ).toList());
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end,
                                                          int[] index, String descriptor, boolean visible) {
        return new ReplayAnnotationVisitor(mvs.stream().map(
                it -> it.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible)
        ).toList());
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end,
                                   int index) {
        for (MethodVisitor mv : mvs) {
            mv.visitLocalVariable(name, descriptor, signature, start, end, index);
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        for (MethodVisitor mv : mvs) {
            mv.visitLineNumber(line, start);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        for (MethodVisitor mv: mvs) {
            mv.visitMaxs(maxStack, maxLocals);
        }
    }

    @Override
    public void visitEnd() {
        for (MethodVisitor mv: mvs) {
            mv.visitEnd();
        }
    }
}
