package al.aoli.exchain.demo;
//
//import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
//import edu.columbia.cs.psl.phosphor.runtime.Taint;
//import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.PowerSetTree;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.CharBuffer;
import java.rmi.RemoteException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.zip.DataFormatException;

public class Main {

    static class Dummy<T> {
        T f;

        void setF(T f) {
            this.f = f;
        }

        void print() {
            System.out.println(f);
            test();
            f.getClass();
        }

        static void possibleThrow() {
            throw new RuntimeException();
        }

        static void test() {
            DataFlowTest test = new DataFlowTest();
            int a = 3;
            if (a < 10) {
                possibleThrow();
            }
            test.dummy.foo();
            test.scene2();
            a = 5;
        }

    }

//    public static void setStringCharTaints(String str, int label) {
//        Taint<?> tag = label == null ? Taint.emptyTaint() : Taint.withLabel(label);
//        Taint<?>[] tags = new Taint[str.length()];
//        for (int i = 0; i < tags.length; i++) {
//            tags[i] = tag;
//        }
//        MultiTainter.setStringCharTaints(str, tags);
//    }

    void foo() {

        if (bar()) {
            try {
                foo();
            } catch (Exception e) {

            }
        } else {
            throw new RuntimeException();
        }
    }

    boolean bar() {
        return false;
    }


    boolean f = true;
    public static void main(String[] args) throws DataFormatException, InterruptedException, RemoteException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        DataFlowTest test = new DataFlowTest();
        test.callScene1();

//        test.callScene1();
//        Object a = MultiTainter.taintedReference(new Object(), "123");
//
//        Object b = a;
//        Object c = b;
//
//        if (c == null) {
//            System.out.println(MultiTainter.getTaint(c));
//        }
    }
}