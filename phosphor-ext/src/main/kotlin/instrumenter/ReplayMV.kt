package instrumenter

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes

class ReplayMV: MethodVisitor(Opcodes.ASM9) {
}