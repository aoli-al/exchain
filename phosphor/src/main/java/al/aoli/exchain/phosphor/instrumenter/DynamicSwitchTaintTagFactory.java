package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.instrumenter.DataAndControlFlowTagFactory;

public class DynamicSwitchTaintTagFactory extends DataAndControlFlowTagFactory {
    @Override
    public boolean isIgnoredMethod(String owner, String name, String desc) {
        if (name.contains(Constants.originMethodSuffix)) {
            return true;
        }
        return super.isIgnoredMethod(owner, name, desc);
    }

    @Override
    public boolean isIgnoredClass(String className) {
        if (className.startsWith("al/aoli/exchain/instrumentation") ||
                className.startsWith("al/aoli/exchain/runtime") ||
                className.startsWith("al/aoli/exchain/phosphor") ||
                className.startsWith("net/bytebuddy")) {
            return true;
        }
        return super.isIgnoredClass(className);
    }
}
