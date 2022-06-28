package al.aoli.exception.demo;

import jdk.jshell.spi.ExecutionControlProvider;

import java.util.Random;

class Test {
    public int scene1() {
        try {
            foo();
        } catch (Exception e1) {
            if (e1 instanceof NullPointerException) {
                System.out.println("caught");
                return 2;
            }
        }
        return 1;
    }

    public void scene2() {
        try {
            foo();
        } catch (Exception e) {
            foo();
        }
    }

    public void scene3() {
        try {
            foo();
        } catch (Exception e) {
            foo();
        }
    }
//
//
//    /**
//     * Root -> RuntimeException:foo
//     *      |> RuntimeException:bar
//     */
//    public void scene5() {
//        try {
//            foo();
//            return;
//        } catch (Exception e) {
//        }
//        bar();
////        throw new RuntimeException("2");
//    }
//
//    /**
//     * Root -> RuntimeException:foo -> RuntimeException:bar
//     */
//    public void scene6() {
//        try {
//            foo();
//            return;
//        } catch (Exception e) {
//            bar();
//        }
////        throw new RuntimeException("2");
//    }
//
//    public void scene3() {
//        try {
//            foo();
//            return;
//        } catch (Exception e) {
//        }
//        bar();
//        //
//    }
//
//    public void scene4() {
//        try {
//            foo();
//        } catch (Exception e) {
//            bar();
////            throw new RuntimeException("2");
//        }
//    }
//
//    public void bar() {
//        throw new RuntimeException("2");
//    }
//
    public void foo() {
        throw new RuntimeException("1");
    }

}

public class Main {
    public static void main(String[] args) {
        Test t = new Test();
//        System.out.println(t.scene1());
        t.scene2();
    }
}