package al.aoli.exchain.phosphor.instrumenter;

import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.DOUBLE;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.FLOAT;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.INTEGER;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.LONG;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.List;

public class Utils {
    static List<Object> descriptorToLocals(String descriptor) {
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        List<Object> locals = new ArrayList<>();
        for (Type argumentType : argumentTypes) {
            switch (argumentType.getSort()) {
                case Type.INT:
                case Type.BYTE:
                case Type.CHAR:
                case Type.BOOLEAN:
                case Type.SHORT:
                    {
                        locals.add(INTEGER);
                        break;
                    }
                case Type.LONG:
                    {
                        locals.add(LONG);
                        break;
                    }
                case Type.DOUBLE:
                    {
                        locals.add(DOUBLE);
                        break;
                    }
                case Type.FLOAT:
                    {
                        locals.add(FLOAT);
                        break;
                    }
                default:
                    {
                        locals.add(argumentType.getInternalName());
                        break;
                    }
            }
        }
        return locals;
    }
}
