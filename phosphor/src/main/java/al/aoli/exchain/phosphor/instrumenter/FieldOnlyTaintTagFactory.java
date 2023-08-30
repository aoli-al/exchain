package al.aoli.exchain.phosphor.instrumenter;

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
