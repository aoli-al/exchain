package al.aoli.exchain.phosphor.instrumenter

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.AnnotationVisitor
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes

class ReplayAV(val avs: List<AnnotationVisitor>): AnnotationVisitor(Opcodes.ASM9) {
    constructor(vararg avs: AnnotationVisitor): this(avs.asList())

    override fun visit(name: String?, value: Any?) {
        for (av in avs) {
            av.visit(name, value)
        }
    }

    override fun visitEnum(name: String?, descriptor: String?, value: String?) {
        for (av in avs) {
            av.visitEnum(name, descriptor, value)
        }
    }

    override fun visitAnnotation(name: String?, descriptor: String?): AnnotationVisitor? {
        return ReplayAV(avs.mapNotNull { it.visitAnnotation(name, descriptor) })
    }

    override fun visitArray(name: String?): AnnotationVisitor {
        return ReplayAV(avs.mapNotNull { it.visitArray(name) })
    }

    override fun visitEnd() {
        for (av in avs) {
            av.visitEnd()
        }
    }

}