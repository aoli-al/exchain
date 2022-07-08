package al.aoli.exception.demo;

import java.util.Random;
import java.util.zip.DataFormatException;

public class DataFlowTest {

    public void scene7() throws DataFormatException {
        throw new DataFormatException("123");
    }

    public static class Dummy {
        void foo() {}
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

    public void scene1() {
        Object o = null;

        try {
            o = createObjectWithException();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        String s = null;
        try {
            s = o.toString();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        System.out.println(s.length());
    }


    public void functionWithException() {
        throw new RuntimeException("exception");
    }
    public Object createObjectWithException() {
        throw new RuntimeException("exception");
    }

    public void methodWithException() {
        throw new RuntimeException("exception");
    }
}
