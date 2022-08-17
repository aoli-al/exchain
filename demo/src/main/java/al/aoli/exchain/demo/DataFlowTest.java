package al.aoli.exchain.demo;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;

import java.util.Random;
import java.util.zip.DataFormatException;

public class DataFlowTest {
    public Dummy dummy = new Dummy();

    public void scene7() throws DataFormatException {
        Dummy dummy = new Dummy();
        dummy.complex(new Dummy(), null, null, 0);
    }

    public static class Base {
        void bar() {
        }

        void test(Dummy d) throws DataFormatException {
            d.bar();
            testInternal();
        }

        private void testInternal() throws DataFormatException {
            throw new DataFormatException("123");
        }

    }

    public static class Dummy extends Base {
        public String sub = null;
        void foo() {}
        @Override
        void test(Dummy d) throws DataFormatException {
            super.test(d);
        }

        void complex(Dummy d, String[][] a, String[][][] b, int c) throws DataFormatException {
            test(d);
        }

        void setSub() {
            sub = ":12";
        }

        void test() {
            System.out.println(sub.length());
        }

    }



    public void scene6() {
        Dummy f = new Dummy();
        try {
            f.foo();
            functionWithParameter(a);
            functionWithException();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        System.out.println(a);
    }

    public void functionWithParameter(int a) {}


    public void scene2() {
        int count = 0;
        while (count < 50) {
            try {
                functionWithException();
                count ++;
            } catch (Exception e) {
                count ++;
            }
        }
    }
    int a = 0;
    int b = 0;

    public void scene5() {
        //...
        if (a > 0) {
            if (b < 0) {
//                foo.functionWithException(c);
            }
        }
    }

    public void scene4() {
        int a = 0;
        int b = 0;
        int c = random();
        int d = random();
        try {
            functionWithException();
            a = c;
            functionWithException();
            a += d;
            functionWithException();
        } catch (Exception e) {
            b = d;
        }
    }

    int random() {
        return 0;
    }

    int getAWithException() {
        return 0;
    }

    int getA() {
        return 0;
    }

    int getB() {
        return 0;
    }


    public void scene3() {
        try {
            Random random = new Random();
            if (random.nextBoolean()) {
                return;
            } else {
                functionWithException();
            }
        }
        catch (Exception e) {

        }
    }

    public String scene1(Dummy d) {
        Object o = null;

        try {
            o = createObjectWithException();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        String s = null;
        try {
            s = o.toString();
            d.setSub();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return s;
    }

    public void callScene1() {
        Dummy d = new Dummy();
        scene1(d);
        d.test();
    }



    public void functionWithException() {
        throw new RuntimeException("exception");
    }
    public static Object createObjectWithException() {
        throw new RuntimeException("exception");
    }

    public void methodWithException() {
        throw new RuntimeException("exception");
    }
}
