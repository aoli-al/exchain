package al.aoli.exchain.phosphor.instrumenter;

public final class Constants {
    public static final String originMethodSuffix = "ExchainOrigin";
    public static final String instrumentedMethodSuffix = "ExchainInst";
    public static String methodNameMapping(String name) {
        return switch (name) {
            case "<clinit>" -> "exchainStaticConstructor";
            case "<init>" -> "exchainConstructor";
            default -> name;
        };
    }

    public static String methodNameReMapping(String name) {
        if (name.contains("exchainStaticConstructor")) {
            return "<clinit>";
        }
        if (name.contains("exchainConstructor")) {
            return "<init>";
        }
        if (name.contains(originMethodSuffix)) {
            return name.substring(0, name.indexOf(originMethodSuffix));
        }
        if (name.contains(instrumentedMethodSuffix)) {
            return name.substring(0, name.indexOf(instrumentedMethodSuffix));
        }
        return name;
    }
}
