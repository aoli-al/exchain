package al.aoli.exchain.phosphor.instrumenter;

import static al.aoli.exchain.phosphor.instrumenter.Constants.methodNameMapping;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_STATIC;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.F_NEW;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.IFEQ;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.IFNE;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.UNINITIALIZED_THIS;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.AnnotationVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Attribute;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.List;

public class InlineSwitchMethodVisitor extends MethodVisitor {

    public int finished;
    public InlineSwitchMethodNode originNode;
    public InlineSwitchMethodNode instrumentedNode;
    private Label instrumentedCodeSection = new Label();
    private Label originCodeSection = new Label();
    public boolean isInstrumentedCode = false;
    public boolean isSecondPass = false;
    public boolean annotationOnly = true;
    int access;
    String owner;
    String descriptor;
    String signature;
    String[] exceptions;
    String name;
    boolean shouldInline;

    boolean isInterface;

    public MethodVisitor getMv() {
        return super.mv;
    }

    public InlineSwitchMethodVisitor(
            MethodVisitor mv,
            String owner,
            boolean isInterface,
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions) {
        super(ASM9, mv);
        this.isInterface = isInterface;
        this.name = name;
        this.access = access;
        this.owner = owner;
        this.descriptor = descriptor;
        this.signature = signature;
        this.exceptions = exceptions;
        String newName = methodNameMapping(name);
        shouldInline =
                name.equals("<init>")
                        || name.equals("fillInStackTrace")
                        || owner.startsWith("jdk/internal/reflect/Generated");
        originNode =
                new InlineSwitchMethodNode(
                        access,
                        StringHelper.concat(newName, Constants.originMethodSuffix),
                        descriptor,
                        signature,
                        exceptions);
        instrumentedNode =
                new InlineSwitchMethodNode(
                        access,
                        StringHelper.concat(newName, Constants.instrumentedMethodSuffix),
                        descriptor,
                        signature,
                        exceptions);
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
                    "Z");
            if (isInstrumentedCode) {
                super.visitJumpInsn(IFEQ, originCodeSection);
            } else {
                super.visitJumpInsn(IFNE, instrumentedCodeSection);
            }
        }

        if (isInstrumentedCode) {
            visitLabelAndFrame(instrumentedCodeSection);
        } else {
            //            visitLabelAndFrame(originCodeSection);
        }
        super.visitCode();
    }

    public void visitLabelAndFrame(Label label) {
        super.visitLabel(label);
        List<Object> locals = Utils.descriptorToLocals(descriptor);
        if ((access & ACC_STATIC) == 0) {
            if (name.equals("<init>")) {
                locals.add(0, UNINITIALIZED_THIS);
            } else {
                locals.add(0, owner);
            }
        }
        super.visitFrame(F_NEW, locals.size(), locals.toArray(), 0, null);
    }

    @Override
    public void visitParameter(String name, int access) {
        if (annotationOnly) {
            super.visitParameter(name, access);
        }
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
        if (attribute.isCodeAttribute()) {
            super.visitAttribute(attribute);
        } else if (!isSecondPass) {
            super.visitAttribute(attribute);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (descriptor.contains("CallerSensitive") || descriptor.contains("ForceInline")) {
            shouldInline = true;
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return super.visitAnnotationDefault();
    }
}
