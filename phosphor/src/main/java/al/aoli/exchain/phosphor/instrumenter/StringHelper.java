package al.aoli.exchain.phosphor.instrumenter;

public class StringHelper {
    public static String concat(String... args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg);
        }
        return sb.toString();
    }
}
