package al.aoli.exchain.demo;

import al.aoli.exchain.runtime.ExceptionJavaRuntime;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTagFactory;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class TestEnableExchainDynamically {

    private static class Foo {

    }

    static void test() {
        Foo f = null;
        f = MultiTainter.taintedReference(f, "123");
        Taint t = MultiTainter.getTaint(f);
        System.out.println(t);

        if (update(f)) {
            f = new Foo();
        } else {
            f = MultiTainter.taintedReference(new Foo(), "456");
        }

        t = MultiTainter.getTaint(f);
        System.out.println(t);


    }

    static boolean update(Foo f) {
        return false;
    }

}
