package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.AnnotationVisitor;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.List;


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
        List<AnnotationVisitor> result = new ArrayList<>();
        for (AnnotationVisitor av: avs) {
            AnnotationVisitor visitor = av.visitAnnotation(name, descriptor);
            if (visitor != null) {
                result.add(visitor);
            }
        }
        return new ReplayAnnotationVisitor(result);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        List<AnnotationVisitor> result = new ArrayList<>();
        for (AnnotationVisitor av: avs) {
            AnnotationVisitor visitor = av.visitArray(name);
            if (visitor != null) {
                result.add(visitor);
            }
        }
        return new ReplayAnnotationVisitor(result);
    }

    @Override
    public void visitEnd() {
        for (AnnotationVisitor av: avs) {
            av.visitEnd();
        }
    }
}
