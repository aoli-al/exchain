package al.aoli.exchain.demo;

public class SimpleTest {
    private String s = "123";

    public class FooImpl extends Foo {

        @Override
        void setS(String v) {
            s = s;
        }
    }

    public abstract class Foo {
        private String s2;
        Foo() {
            if (s != null) {
                s2 = s;
            } else {
                s2 = "456";
            }
        }

        String getS2() {
            return s2;
        }

        String getS() {
            return s;
        }

        abstract void setS(String v);
    }

    public Foo getFoo() {
        return new FooImpl();
    }
}
