package al.aoli.exchain.demo;

import al.aoli.exchain.runtime.ExceptionJavaRuntime;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTagFactory;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class TestEnableExchainDynamically {
    Object a = new int[3];
    int[] b= new int[3];
    Foo c = new Foo();

    private static class Foo {

    }

    void test() {
        for (int i = 0; i < 3; i++) {
            ((int[]) a)[i] = MultiTainter.taintedInt(3, "foo");
        }
        b[1] = ((int[]) a)[0];
        System.out.println(b);
    }

    void test3() {
        c = MultiTainter.taintedReference(null, "bar");
        System.out.println(MultiTainter.getTaint(c));
        a = MultiTainter.taintedReference(b, "123");
        System.out.println(MultiTainter.getTaint(((int[])a)[1]));

    }

    void test2() {
        System.out.println(MultiTainter.getTaint(b[1]));
    }

    void test4() {
        assert(update(new Foo()));
    }

    static boolean update(Foo f) {
        return true;
    }

}
