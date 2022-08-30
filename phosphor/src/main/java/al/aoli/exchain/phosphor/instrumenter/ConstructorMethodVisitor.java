package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.AnnotationVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Attribute;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.TypePath;

import java.util.Collections;

import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.IFEQ;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.IFNE;

public class ConstructorMethodVisitor extends MethodVisitor {

    public int finished;
    public ConstructorMethodNode originNode;
    public ConstructorMethodNode instrumentedNode;
    private Label instrumentedCodeSection = new Label();
    private Label originCodeSection = new Label();
    private boolean isInstrumentedCode = false;
    private boolean isSecondPass = false;
    private String owner;

    public void setIsSecondPass(boolean isSecondPass) {
        this.isSecondPass = isSecondPass;
    }

    public void setIsInstrumentedCode(boolean isInstrumentedCode) {
        this.isInstrumentedCode = isInstrumentedCode;
    }

    public void visitorFinished() {
        if (instrumentedNode.finished && originNode.finished) {
            originNode.accept(this);
            isInstrumentedCode = true;
            isSecondPass = true;
            instrumentedNode.accept(this);
        }
    }

    public void checkFinished() {
        if (!instrumentedNode.finished || !originNode.finished) {
            super.visitCode();
            super.visitInsn(Opcodes.RETURN);
            super.visitMaxs(0, 0);
            super.visitEnd();
        }
    }


    public ConstructorMethodVisitor(MethodVisitor mv, String owner) {
        super(ASM9, mv);
        this.owner = owner;
        originNode = new ConstructorMethodNode(this);
        instrumentedNode = new ConstructorMethodNode(this);
    }

    public ConstructorMethodNode getOriginNode() {
        return originNode;
    }

    public ConstructorMethodNode getInstrumentedNode() {
        return instrumentedNode;
    }

    @Override
    public void visitCode() {
        if (!isSecondPass) {
            super.visitFieldInsn(
                    Opcodes.GETSTATIC,
                    "al/aoli/exchain/runtime/ExceptionJavaRuntime",
                    "enabled",
                    "Z"
            );
            if (isInstrumentedCode) {
                super.visitJumpInsn(IFNE, originCodeSection);
            } else {
                super.visitJumpInsn(IFEQ, instrumentedCodeSection);
            }
        }

        if (isInstrumentedCode) {
            super.visitLabel(instrumentedCodeSection);
        } else {
            super.visitLabel(originCodeSection);
        }
        super.visitCode();
    }

    @Override
    public void visitEnd() {
        if (isSecondPass) {
            super.visitEnd();
        }
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        if (!isSecondPass) {
            return super.visitParameterAnnotation(parameter, descriptor, visible);
        } else {
            return new ReplayAnnotationVisitor(Collections.emptyList());
        }
    }

    @Override
    public void visitParameter(String name, int access) {
        if (!isSecondPass) {
            super.visitParameter(name, access);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        if (!isSecondPass) {
            return super.visitAnnotationDefault();
        } else {
            return new ReplayAnnotationVisitor(Collections.emptyList());
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (!isSecondPass) {
            return super.visitAnnotation(descriptor, visible);
        } else {
            return new ReplayAnnotationVisitor(Collections.emptyList());
        }
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        if (!isSecondPass) {
            return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        } else {
            return new ReplayAnnotationVisitor(Collections.emptyList());
        }
    }

    @Override
    public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
        if (!isSecondPass) {
            super.visitAnnotableParameterCount(parameterCount, visible);
        }
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        if (!isSecondPass) {
            super.visitAttribute(attribute);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (isInstrumentedCode) {
            super.visitMaxs(maxStack, maxLocals);
        }
    }
}
