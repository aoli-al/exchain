package al.aoli.exchain.demo;
//
// import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
// import edu.columbia.cs.psl.phosphor.runtime.Taint;
// import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.apache.commons.math3.util.FastMath;

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
    private static final double[][] LN_MANT = new double[][] {
            {1, 2},
            {2, 3}
    };
    private static final double[] LN_FOO = new double[] {
            1, 2, 3
    };



    public static void main(String[] args) throws Throwable {
        FastMath.log(2.3);
    }

    public static String concat(String a) {
        return a + "1";
    }
}
