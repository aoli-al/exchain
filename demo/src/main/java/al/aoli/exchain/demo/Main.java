package al.aoli.exchain.demo;
//
// import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
// import edu.columbia.cs.psl.phosphor.runtime.Taint;
// import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class Main {
    //    public static int foo = 3;
    //
    @Attribute({"123"})
    public static class Test2 {}

    int[][] a;

    public static class DynamicInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object a = new int[3];
            ((int[]) a)[0] = 0;
            return ((int[]) a).clone();
        }
    }

    static void testAnnotation() throws InvocationTargetException, IllegalAccessException {
        Annotation a = Test2.class.getAnnotations()[0];
        for (Method declaredMethod : a.annotationType().getDeclaredMethods()) {
            declaredMethod.invoke(a);
            //            System.out.println(declaredMethod);
        }
    }

    private static final MethodType LOAD_CLASS_CLASSLOADER =
            MethodType.methodType(ServiceLoader.class, Class.class, ClassLoader.class);

    public static class O1 {}

    public static class V1 {
        private O1 o1;

        public V1(O1 o1) {
            this.o1 = o1;
        }

        public void process() throws Throwable {
            Field f = this.getClass().getField("o1PHOSPHOR_TAG");
            f.set(this, Taint.withLabel("?>??"));
        }

        public void check() throws Throwable {
            Field f = this.getClass().getField("o1PHOSPHOR_TAG");
            System.out.println(f.get(this));
        }
    }

    public static void main(String[] args) throws Throwable {
//        O1 obj = new O1();
//
//        V1 v1 = new V1(obj);
//        v1.process();
//        V1 v2 = new V1(obj);
//        v2.check();

        final ObjectDemo demo = new ObjectDemo();

        Runnable runA = new Runnable() {

            public void run() {
                try {
                    String item = demo.removeElement();
                    System.out.println("" + item);
                } catch (InterruptedException ix) {
                    System.out.println("Interrupted Exception!");
                } catch (Exception x) {
                    System.out.println("Exception thrown.");
                }
            }
        };

        try {
            Thread threadA1 = new Thread(runA, "A");
            threadA1.start();

            Thread.sleep(500);


            threadA1.interrupt();
        } catch (InterruptedException x) {
        }
        //        MethodHandles.Lookup publicLookup = MethodHandles.lookup();
        //        final MethodHandle handle = publicLookup.findStatic(ServiceLoader.class, "load",
        // LOAD_CLASS_CLASSLOADER);
        //        final ServiceLoader serviceLoader = (ServiceLoader) handle.invokeExact(Main.class,
        // Main.class.getClassLoader());
        //        System.out.println(serviceLoader);
    }

    public static String concat(String a) {
        return a + "1";
    }
}
