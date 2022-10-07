package al.aoli.exchain.demo;


import java.util.Random;
import java.util.zip.DataFormatException;

public class DataFlowTest {
    public Dummy dummy = new Dummy();
    public boolean state = false;

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
        public Base b = new Base();
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
            try {
                if (sub == null) {
                    logError("sub is null");
                }
            }
            catch (RuntimeException e) {
                System.out.println(e.getMessage());
            }
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

    Object o = null;

    public String scene1(Dummy d) {
//        try {
//            if (d.sub == null) {
//                throw new RuntimeException("sub is null");
//            }
//        }
//        catch (RuntimeException e) {
//            System.out.println(e.getMessage());
//        }
//        return "";
        try {
            o = createObjectWithException();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        test2(d, (String) o);

        String s = null;
        try {
            d.test();
//            s = o.toString();
//            d.setSub();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return s;
    }

    public static void logError(String s) {
        if (s != null) {
            throw new RuntimeException(s);
        }
    }

    public void test2(Dummy d, String o) {
        d.sub = o;

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
