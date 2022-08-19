package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;

import java.lang.reflect.Field;

import static al.aoli.exchain.phosphor.instrumenter.Constants.methodNameMapping;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;

public class DynamicSwitchPreCV extends ClassVisitor {
    public DynamicSwitchPreCV(ClassVisitor cv, boolean skipFrames) {
        super(ASM9, cv);
        ClassVisitor subCV = cv;
        while (subCV != null && !(subCV instanceof TaintTrackingClassVisitor)) {
            try {
                Field field = ClassVisitor.class.getDeclaredField("cv");
                field.setAccessible(true);
                subCV = (ClassVisitor) field.get(subCV);
            }
            catch (NoSuchFieldException | IllegalAccessException e) {
                subCV = null;
            }
        }

        if (subCV != null) {
            try {
                Field field = subCV.getClass().getDeclaredField("aggressivelyReduceMethodSize");
                field.setAccessible(true);
                HashSet<String> methodList = (HashSet<String>) field.get(subCV);
                HashSet<String> newMethodList = new HashSet<>();

                if (methodList != null) {
                    for (String s : methodList) {
                        newMethodList.add(s.replace(Constants.instrumentedMethodSuffix, "")
                                .replace(Constants.originMethodSuffix, "")
                                .replace("exchainConstructor", "<init>")
                                .replace("exchainStaticConstructor", "<clinit>"));
                    }
                }

                field.set(subCV, newMethodList);
            }
            catch (NoSuchFieldException | IllegalAccessException e) {
            }
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                     String[] exceptions) {
        String newName = methodNameMapping(name);
        MethodVisitor mv1 = super.visitMethod(access, newName + Constants.originMethodSuffix, descriptor, signature,
                exceptions);
        MethodVisitor mv2 = super.visitMethod(access, name, descriptor, signature, exceptions);

        return new ReplayMethodVisitor(mv1, mv2);
    }
}
