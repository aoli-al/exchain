package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;

import java.lang.reflect.Method;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.REMOVE_TAINTED_FIELDS;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.REMOVE_TAINTED_INTERFACES;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.REMOVE_TAINTED_METHODS;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;

public class ReflectionFixingMethodVisitor extends MethodVisitor {
    String className;
    public ReflectionFixingMethodVisitor(MethodVisitor methodVisitor, String owner) {
        super(ASM9, methodVisitor);
        className = owner;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                                boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        if (owner.equals("java/lang/Class") && name.endsWith("Methods") && !className.equals(owner) &&
                descriptor.equals(StringHelper.concat("()", Type.getDescriptor(Method[].class)))) {
            visit(REMOVE_TAINTED_METHODS);
        } else if (owner.equals("java/lang/Class") && name.equals("getInterfaces")) {
            visit(REMOVE_TAINTED_INTERFACES);
        } else if (owner.equals("java/lang/Class") && name.endsWith("Fields") && !className.equals("java/lang/Class")) {
            visit(REMOVE_TAINTED_FIELDS);
        }
    }

    private void visit(TaintMethodRecord method) {
        super.visitMethodInsn(method.getOpcode(), method.getOwner(), method.getName(), method.getDescriptor(), method.isInterface());
    }
}
