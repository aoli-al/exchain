package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;

import java.lang.reflect.Field;

import static al.aoli.exchain.phosphor.instrumenter.Constants.methodNameMapping;
import static al.aoli.exchain.phosphor.instrumenter.Constants.methodNameReMapping;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ACC_NATIVE;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.ASM9;

public class DynamicSwitchPreCV extends ClassVisitor {
    private String owner;
    private Set<String> aggressivelyReduceMethodSize = new HashSet<>();
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
                    aggressivelyReduceMethodSize = methodList;
                    for (String s : methodList) {
                        String[] results = s.split("\\(");
                        if (s.contains(Constants.instrumentedMethodSuffix)) {
                            newMethodList.add(methodNameReMapping(results[0]) + "(" + results[1]);
                        }
                    }
                }
                field.set(subCV, newMethodList);
            }
            catch (NoSuchFieldException | IllegalAccessException e) {
            }
        }

        while (subCV != null && !(subCV instanceof DynamicSwitchPostCV)) {
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
            ((DynamicSwitchPostCV) subCV).setAggressivelyReduceMethodSize(aggressivelyReduceMethodSize);
        }
    }

    public Set<String> getAggressivelyReduceMethodSize() {
        return aggressivelyReduceMethodSize;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        owner = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                     String[] exceptions) {
        if (name.contains("Exchain")) {
            return null;
        }
        String newName = methodNameMapping(name);
        MethodVisitor mv2 = super.visitMethod(access, name, descriptor, signature, exceptions);
        if ((access & ACC_ABSTRACT) != 0 || (access & ACC_NATIVE) != 0) {
            return mv2;
        }
        MethodVisitor mv1 = super.visitMethod(access, newName + Constants.originMethodSuffix, descriptor, signature, exceptions);
        return new ReplayMethodVisitor(access, "pre" + name, descriptor,
                List.of(mv2), List.of(), List.of(mv1));
    }
}
