package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.AnnotationVisitor;

import java.util.List;
import java.util.Objects;

import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;

public class ReplayAnnotationVisitor extends AnnotationVisitor {
    public List<AnnotationVisitor> avs;

    public ReplayAnnotationVisitor(List<AnnotationVisitor> avs) {
        super(ASM9);
        this.avs = avs;
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
        for (AnnotationVisitor av: avs) {
            av.visitEnum(name, descriptor, value);
        }
    }

    @Override
    public void visit(String name, Object value) {
        for (AnnotationVisitor av: avs) {
            av.visit(name, value);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        return new ReplayAnnotationVisitor(avs.stream().map(
                it -> it.visitAnnotation(name, descriptor)
        ).filter(Objects::nonNull).toList());
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        return new ReplayAnnotationVisitor(avs.stream().map(
                it -> it.visitArray(name)
        ).filter(Objects::nonNull).toList());
    }

    @Override
    public void visitEnd() {
        for (AnnotationVisitor av: avs) {
            av.visitEnd();
        }
    }
}
