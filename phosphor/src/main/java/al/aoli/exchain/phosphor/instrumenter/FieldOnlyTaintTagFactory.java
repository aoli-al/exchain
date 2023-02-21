package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.instrumenter.DataAndControlFlowTagFactory;

public class FieldOnlyTaintTagFactory extends DynamicSwitchTaintTagFactory {

    @Override
    public boolean isIgnoredMethod(String owner, String name, String desc) {
        return true;
    }

    @Override
    public boolean makeFieldTransient() {
        return false;
    }

    @Override
    public boolean disableClinitRetransform() {
        return true;
    }
}
