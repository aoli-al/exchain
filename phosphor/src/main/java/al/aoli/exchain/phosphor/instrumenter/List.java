package al.aoli.exchain.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.ArrayList;

public class List {
    public static <V> edu.columbia.cs.psl.phosphor.struct.harmony.util.List<V> of(V... args) {
        var list = new ArrayList<V>();
        for (V arg : args) {
            list.add(arg);
        }
        return list;
    }
}
