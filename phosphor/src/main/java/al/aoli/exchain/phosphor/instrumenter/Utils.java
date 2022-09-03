package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.List;


import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.DOUBLE;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.FLOAT;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.INTEGER;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.LONG;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.UNINITIALIZED_THIS;


public class Utils {
    static List<Object> descriptorToLocals(String descriptor) {
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        List<Object> locals = new ArrayList<>();
        for (Type argumentType : argumentTypes) {
            switch (argumentType.getSort()) {
                case Type.INT, Type.BYTE, Type.CHAR, Type.BOOLEAN -> {
                    locals.add(INTEGER);
                }
                case Type.LONG -> {
                    locals.add(LONG);
                }
                case Type.DOUBLE -> {
                    locals.add(DOUBLE);
                }
                case Type.FLOAT -> {
                    locals.add(FLOAT);
                }
                default -> {
                    locals.add(argumentType.getInternalName());
                }
            }
        }
        return locals;
    }
}
