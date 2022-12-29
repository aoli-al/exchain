package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.instrumenter.DataAndControlFlowTagFactory;

public class FieldOnlyTaintTagFactory extends DataAndControlFlowTagFactory {

    @Override
    public boolean isIgnoredMethod(String owner, String name, String desc) {
        return true;
    }
}
