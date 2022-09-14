package al.aoli.exchain.demo;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;

public class TestDebug {
    public static void print(String a, String b, String c, String d) {
        if (check()) {
            String s0 = MultiTainter.taintedReference(null, "123");
            System.out.println(MultiTainter.getTaint(s0));
        }

        if (check()) {
            String s1 = null;
            System.out.println(MultiTainter.getTaint(s1));
        }
    }

    public static void assertEquals(double expected, double actual, double delta) {
        System.out.println(expected + actual + delta);
    }

    private static boolean check() {
        return true;
    }

}
