package al.aoli.exchain.demo;

import java.rmi.RemoteException;
import java.util.zip.DataFormatException;

public class Main {

    static class Dummy<T> {
        T f;

        void print() {
            System.out.println(f);
            test();
        }

        static void possibleThrow() {
            throw new RuntimeException();
        }

        static void test() {
            DataFlowTest test = new DataFlowTest();
            int a = 3;
            possibleThrow();
            test.dummy.foo();
            test.scene2();
            a = 5;
        }

    }
    public static void main(String[] args) throws DataFormatException, InterruptedException, RemoteException {
        Dummy<Integer> i = new Dummy<>();
        i.f = 123;
        i.print();

//        DataFlowTest test = new DataFlowTest();
//        test.scene1();
    }
}