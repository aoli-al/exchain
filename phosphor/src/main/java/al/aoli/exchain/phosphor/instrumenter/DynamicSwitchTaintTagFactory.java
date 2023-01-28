package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.instrumenter.DataAndControlFlowTagFactory;

public class DynamicSwitchTaintTagFactory extends DataAndControlFlowTagFactory {

    @Override
    public boolean isIgnoredMethod(String owner, String name, String desc) {
        if (name.contains(Constants.originMethodSuffix)) {
            return true;
        }
        if (owner.contains("java/lang/invoke/MethodHandleImpl") && name.contains("checkSpreadArgument")) {
            return true;
        }
        return super.isIgnoredMethod(owner, name, desc);
    }

    @Override
    public boolean isIgnoredClass(String className) {
        if (className.startsWith("al/aoli/exchain/instrumentation")
                || className.startsWith("al/aoli/exchain/runtime")
                || className.startsWith("al/aoli/exchain/phosphor")
                || className.startsWith("mu/")
                || className.startsWith("kotlin")
                || className.startsWith("org.mockito")
                || className.startsWith("net/bytebuddy")) {
//                || className.startsWith("org/apache/derby/exe")) {
            return true;
        }
        return super.isIgnoredClass(className);
    }
}
