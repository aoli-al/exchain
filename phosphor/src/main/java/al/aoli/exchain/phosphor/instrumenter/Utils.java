package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.DOUBLE;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.FLOAT;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.INTEGER;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.LONG;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.UNINITIALIZED_THIS;


public class Utils {
    static List<Object> descriptorToLocals(String descriptor) {
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        List<Object> locals = new ArrayList<>(
                Arrays.stream(argumentTypes).map(it -> {
                    switch (it.getSort()) {
                        case Type.INT, Type.BYTE, Type.CHAR, Type.BOOLEAN -> {
                            return INTEGER;
                        }
                        case Type.LONG -> {
                            return LONG;
                        }
                        case Type.DOUBLE -> {
                            return DOUBLE;
                        }
                        case Type.FLOAT -> {
                            return FLOAT;
                        }
                        default -> {
                            return it.getInternalName();
                        }
                    }
                }).toList());
        return locals;
    }
}
