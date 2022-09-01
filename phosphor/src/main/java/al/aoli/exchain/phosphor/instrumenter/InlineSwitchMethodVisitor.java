package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.AnnotationVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Attribute;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.TypePath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static al.aoli.exchain.phosphor.instrumenter.Constants.methodNameMapping;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_STATIC;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ALOAD;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ARETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.DLOAD;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.DRETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.FLOAD;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.FRETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.F_FULL;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.F_NEW;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.IFEQ;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.IFNE;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ILOAD;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.IRETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.LLOAD;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.LRETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.RETURN;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.UNINITIALIZED_THIS;

public class InlineSwitchMethodVisitor extends MethodVisitor {

    public int finished;
    public InlineSwitchMethodNode originNode;
    public InlineSwitchMethodNode instrumentedNode;
    private Label instrumentedCodeSection = new Label();
    private Label originCodeSection = new Label();
    public boolean isInstrumentedCode = false;
    public boolean isSecondPass = false;
    int access;
    String owner;
    String descriptor;
    String signature;
    String[] exceptions;
    String name;

    ClassVisitor cv;

    boolean isInterface;

    boolean shouldInline;

    public MethodVisitor getMv() {
        return super.mv;
    }


    public InlineSwitchMethodVisitor(MethodVisitor mv, String owner,
                                     boolean isInterface,
                                     int access, String name,
                                     String descriptor, String signature,
                                     String[] exceptions) {
        super(ASM9, mv);
        this.isInterface = isInterface;
        this.name = name;
        this.access = access;
        this.owner = owner;
        this.descriptor = descriptor;
        this.signature = signature;
        this.exceptions = exceptions;
        shouldInline = name.equals("<init>") || name.equals("<clinit>");
        String newName = methodNameMapping(name);
        originNode = new InlineSwitchMethodNode(access,
                newName + Constants.originMethodSuffix,
                descriptor, signature, exceptions);
        instrumentedNode = new InlineSwitchMethodNode(
                access, newName + Constants.instrumentedMethodSuffix,
                descriptor, signature, exceptions);
    }

    public InlineSwitchMethodNode getOriginNode() {
        return originNode;
    }

    public InlineSwitchMethodNode getInstrumentedNode() {
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
                super.visitJumpInsn(IFNE, instrumentedCodeSection);
            } else {
                super.visitJumpInsn(IFEQ, originCodeSection);
            }
        }

        if (isInstrumentedCode) {
            super.visitLabel(instrumentedCodeSection);
            List<Object> locals = Utils.descriptorToLocals(descriptor);
            if ((access & ACC_STATIC) == 0) {
                if (name.equals("<init>")) {
                    locals.add(0, UNINITIALIZED_THIS);
                } else {
                    locals.add(0, owner);
                }
            }
            super.visitFrame(F_NEW, locals.size(), locals.toArray(), 0, null);
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
    public void visitMaxs(int maxStack, int maxLocals) {
        if (isSecondPass) {
            super.visitMaxs(maxStack, maxLocals);
        }
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        super.visitAttribute(attribute);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (descriptor.contains("CallerSensitive")) {
            shouldInline = true;
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (isInstrumentedCode) {
            super.visitLineNumber(line + 50000, start);
        } else {
            super.visitLineNumber(line, start);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return super.visitAnnotationDefault();
    }

}
