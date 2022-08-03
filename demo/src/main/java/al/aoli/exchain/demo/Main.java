package al.aoli.exchain.demo;
//
//import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
//import edu.columbia.cs.psl.phosphor.runtime.Taint;
//import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;

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
    public static void main(String[] args) throws DataFormatException, InterruptedException, RemoteException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
//        ClassWithLargeMethod obj1 = new ClassWithLargeMethod(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39);
//        Dummy<String> dummy = new Dummy<>();
//        dummy.setF(MultiTainter.taintedReference((String) null, "123"));
//        System.out.println(dummy.f);
//        System.out.println(MultiTainter.getTaint(dummy.f));

//        Constructor<ClassWithLargeSignature> constructor = ClassWithLargeSignature.class.getDeclaredConstructor(int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class);
//        int i22 = MultiTainter.taintedInt(22, "tainted");
//        ClassWithLargeSignature obj2 = constructor.newInstance(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, i22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39);

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream in = new FileInputStream("/home/aoli/tests/certs/badssl.com-client.p12")) {
            keystore.load(in, "badssl.com".toCharArray());
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, "badssl.com".toCharArray());

    }
}