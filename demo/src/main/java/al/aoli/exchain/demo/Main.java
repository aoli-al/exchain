package al.aoli.exchain.demo;
//
// import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
// import edu.columbia.cs.psl.phosphor.runtime.Taint;
// import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class Main {
    //    public static int foo = 3;
    //
    @Attribute({"123"})
    public static class Test2 {
    }
    int[][] a;

    public static class DynamicInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object a = new int[3];
            ((int[]) a)[0] = 0;
            return ((int[])a).clone();
        }
    }

    static void testAnnotation() throws InvocationTargetException, IllegalAccessException {
        Annotation a = Test2.class.getAnnotations()[0];
        for (Method declaredMethod : a.annotationType().getDeclaredMethods()) {
            declaredMethod.invoke(a);
//            System.out.println(declaredMethod);
        }
    }

    public static void main(String[] args)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        testAnnotation();
//        DataFlowTest test = new DataFlowTest();
//        test.sceneLocal();
//        ArrayInterface i = (ArrayInterface) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
//                new Class[] {ArrayInterface.class},
//                new DynamicInvocationHandler());
//        System.out.println(i.get());
        //        test.scene10();
        //        TestEnableExchainDynamically test = new TestEnableExchainDynamically();
        //        test.test();
        //        test.testO();
        //        test.affectedLocalTest();
        //        test.callScene1();
        //        test.test3();
        //        test.test();
        //        test.test2();
    }

    //    public static String merged(String a, String b, String c, String d) {
    //        return a + b + "conf" + c + d;
    //    }
    //
    //    public static String get() {
    //        return "";
    //    }
    //
    //    static class Dummy<T> {
    //        T f;
    //
    //        void setF(T f) {
    //            this.f = f;
    //        }
    //
    //        void print() {
    //            System.out.println(f);
    //            test();
    //            f.getClass();
    //        }
    //
    //        static void possibleThrow() {
    //            throw new RuntimeException();
    //        }
    //
    //        static void test() {
    //            DataFlowTest test = new DataFlowTest();
    //            int a = 3;
    //            if (a < 10) {
    //                possibleThrow();
    //            }
    //            test.dummy.foo();
    //            test.scene2();
    //            a = 5;
    //        }
    //
    //    }
    //
    ////    public static void setStringCharTaints(String str, int label) {
    ////        Taint<?> tag = label == null ? Taint.emptyTaint() : Taint.withLabel(label);
    ////        Taint<?>[] tags = new Taint[str.length()];
    ////        for (int i = 0; i < tags.length; i++) {
    ////            tags[i] = tag;
    ////        }
    ////        MultiTainter.setStringCharTaints(str, tags);
    ////    }
    //
    //    void foo() {
    //
    //        if (bar()) {
    //            try {
    //                foo();
    //            } catch (Exception e) {
    //
    //            }
    //        } else {
    //            throw new RuntimeException();
    //        }
    //    }
    //
    //    boolean bar() {
    //        test();
    //        return false;
    //    }
    //
    //    private void test() {}
    //
    //
    //
    //    boolean f = true;
    //
    //    public static class Test {
    //        public int a = 123;
    //        public int[] b1 = null;
    //        public int[] b2 = new int[]{1, 2, 3};
    //        public DataFlowTest.Dummy c1 = null;
    //        public DataFlowTest.Dummy c2 = new DataFlowTest.Dummy();
    //    }
    //
    ////    public static void main(String[] args)
    ////            throws DataFormatException, InterruptedException, RemoteException,
    // NoSuchMethodException,
    ////            InvocationTargetException, IllegalAccessException, InstantiationException,
    // KeyStoreException,
    ////            NoSuchAlgorithmException, UnrecoverableKeyException, NoSuchFieldException,
    // ClassNotFoundException {
    //////        Properties p;
    //////        p.put("1", "2");
    //////        p.size();
    //////        DataFlowTest t = new DataFlowTest();
    //////        t.callScene1();
    ////    }
}
