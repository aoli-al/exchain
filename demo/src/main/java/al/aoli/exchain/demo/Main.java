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
        int[] a = new int[] {1, 2, 4, 5, 6, 7, 8, 10};
        int[] b = new int[] {1, 2, 4, 5, 6, 7, 8, 10};
        for (int i = 0; i < 1000; i++) {
//            if (Arrays.equals(a, b)) {
//                System.out.println("true");
//            } else {
//                System.out.println("false");
//            }
            Method m = Arrays.class.getMethod("equals", int[].class, int[].class);
            if ((Boolean) (m.invoke(null, a, b))) {
                System.out.println("true");
            } else {
                System.out.println("false");
            }
        }
    }

    public static String concat(String a) {
        return a + "1";
    }
}
