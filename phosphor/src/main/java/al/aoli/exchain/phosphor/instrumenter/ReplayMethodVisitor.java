package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.AnnotationVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Attribute;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Handle;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.TypePath;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_STATIC;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.DOUBLE;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.LONG;

public class ReplayMethodVisitor extends MethodVisitor {

    List<MethodVisitor> mvs;
    List<MethodVisitor> annotationOnlyMvs;

    List<MethodVisitor> codeOnlyMvs;

    int access;
    String name;
    String descriptor;

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
        return new ReplayAnnotationVisitor(
                Stream.concat(
                        mvs.stream().map(MethodVisitor::visitAnnotationDefault),
                        annotationOnlyMvs.stream().map(MethodVisitor::visitAnnotationDefault)
                ).toList());
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return new ReplayAnnotationVisitor(
                Stream.concat(
                        mvs.stream().map(it -> it.visitAnnotation(descriptor, visible)),
                        annotationOnlyMvs.stream().map(it -> it.visitAnnotation(descriptor, visible))
                ).toList());
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        return new ReplayAnnotationVisitor(
                Stream.concat(
                        mvs.stream().map(it -> it.visitTypeAnnotation(typeRef, typePath, descriptor, visible)),
                        annotationOnlyMvs.stream().map(it -> it.visitTypeAnnotation(typeRef, typePath, descriptor, visible))
                ).toList());
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
        return new ReplayAnnotationVisitor(
                Stream.concat(
                        mvs.stream().map(it -> it.visitParameterAnnotation(parameter, descriptor, visible)),
                        annotationOnlyMvs.stream().map(it -> it.visitParameterAnnotation(parameter, descriptor, visible))
                ).toList());
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

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        for (MethodVisitor mv : mvs) {
            mv.visitJumpInsn(opcode, label);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitJumpInsn(opcode, label);
        }
    }

    @Override
    public void visitLabel(Label label) {
        for (MethodVisitor mv : mvs) {
            mv.visitLabel(label);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitLabel(label);
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
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        for (MethodVisitor mv : mvs) {
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitLookupSwitchInsn(dflt, keys, labels);
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
        return new ReplayAnnotationVisitor(
                Stream.concat(
                        mvs.stream().map(it -> it.visitInsnAnnotation(typeRef, typePath, descriptor, visible)),
                        codeOnlyMvs.stream().map(it -> it.visitInsnAnnotation(typeRef, typePath, descriptor, visible))
                ).toList());
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        for (MethodVisitor mv : mvs) {
            mv.visitTryCatchBlock(start, end, handler, type);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitTryCatchBlock(start, end, handler, type);
        }
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor,
                                                     boolean visible) {
        return new ReplayAnnotationVisitor(
                Stream.concat(
                        mvs.stream().map(it -> it.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible)),
                        codeOnlyMvs.stream().map(it -> it.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible))
                ).toList());
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end,
                                                          int[] index, String descriptor, boolean visible) {
        return new ReplayAnnotationVisitor(
                Stream.concat(
                        mvs.stream().map(it -> it.visitLocalVariableAnnotation(typeRef, typePath, start, end, index,
                                descriptor, visible)),
                        codeOnlyMvs.stream().map(it -> it.visitLocalVariableAnnotation(typeRef, typePath, start, end, index,
                                descriptor, visible))
                ).toList());
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end,
                                   int index) {
        for (MethodVisitor mv : mvs) {
            mv.visitLocalVariable(name, descriptor, signature, start, end, index);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitLocalVariable(name, descriptor, signature, start, end, index);
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        for (MethodVisitor mv : mvs) {
            mv.visitLineNumber(line, start);
        }
        for (MethodVisitor mv : codeOnlyMvs) {
            mv.visitLineNumber(line, start);
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
            int locals = 0;
            if ((this.access & ACC_STATIC) != 0) {
                locals += 1;
            }
            for (Type type: Type.getArgumentTypes(descriptor)) {
                switch (type.getSort()) {
                    case Type.DOUBLE, Type.LONG ->
                        locals += 2;
                    default -> locals += 1;
                }
            }
            mv.visitMaxs(locals, locals);
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
