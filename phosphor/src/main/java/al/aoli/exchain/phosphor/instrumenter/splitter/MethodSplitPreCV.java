package al.aoli.exchain.phosphor.instrumenter.splitter;

import al.aoli.exchain.phosphor.instrumenter.Constants;
import al.aoli.exchain.phosphor.instrumenter.DynamicSwitchPostCV;
import al.aoli.exchain.phosphor.instrumenter.StringHelper;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;

import java.lang.reflect.Field;

import static al.aoli.exchain.phosphor.instrumenter.Constants.methodNameReMapping;
import static al.aoli.exchain.phosphor.instrumenter.DynamicSwitchPostCV.defaultInline;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;

public class MethodSplitPreCV extends ClassVisitor {
    private Set<String> aggresivelyReduceMethodSize = new HashSet();
    public MethodSplitPreCV(ClassVisitor cv, boolean skipFrames) {
        super(ASM9, cv);


        ClassVisitor subCV = cv;
        while (subCV != null && !(subCV instanceof TaintTrackingClassVisitor)) {
            try {
                Field field = ClassVisitor.class.getDeclaredField("cv");
                field.setAccessible(true);
                subCV = (ClassVisitor) field.get(subCV);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                subCV = null;
            }
        }

        if (subCV != null) {
            try {
                Field field = subCV.getClass().getDeclaredField("aggressivelyReduceMethodSize");
                field.setAccessible(true);
                HashSet<String> methodList = (HashSet<String>) field.get(subCV);

                while (subCV != null && !(subCV instanceof MethodSplitPostCV)) {
                    try {
                        field = ClassVisitor.class.getDeclaredField("cv");
                        field.setAccessible(true);
                        subCV = (ClassVisitor) field.get(subCV);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        subCV = null;
                    }
                }

                if (subCV != null) {
                    ((MethodSplitPostCV) subCV).aggressivelyReduceMethodSize = methodList;
                }

            } catch (NoSuchFieldException | IllegalAccessException e) {
            }
        }

    }
}
