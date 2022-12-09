package al.aoli.exchain.demo;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;

public class TestEnableExchainDynamically {
    Object a = new int[3];
    int[] b = new int[3];
    Foo c = new Foo();
    int o = 0;

    private static class Foo {}

    void test() {
        int a = MultiTainter.taintedInt(1, "bar");
        o = a + 24;
        System.out.println(o);
    }

    void testO() {
        System.out.println(MultiTainter.getTaint(o));
    }

    void test3() {
        c = MultiTainter.taintedReference(null, "bar");
        System.out.println(MultiTainter.getTaint(c));
        a = MultiTainter.taintedReference(b, "123");
        System.out.println(MultiTainter.getTaint(((int[]) a)[1]));
    }

    void test2() {
        System.out.println(MultiTainter.getTaint(b[1]));
    }

    void test4() {
        assert (update(new Foo()));
    }

    static boolean update(Foo f) {
        return true;
    }
}
