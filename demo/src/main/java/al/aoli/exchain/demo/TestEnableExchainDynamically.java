package al.aoli.exchain.demo;

import al.aoli.exchain.runtime.ExceptionJavaRuntime;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTagFactory;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class TestEnableExchainDynamically {
    int[] b= new int[3];

    private static class Foo {

    }

    void test() {
        int[] a = new int[3];
        for (int i = 0; i < 3; i++) {
            a[i] = MultiTainter.taintedInt(3, "foo");
        }
        b[1] = a[0];
        System.out.println(b);
    }

    void test2() {
        System.out.println(MultiTainter.getTaint(b[1]));
    }

    static boolean update(Foo f) {
        return true;
    }

}
