package al.aoli.exchain.demo;
//
// import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
// import edu.columbia.cs.psl.phosphor.runtime.Taint;
// import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

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

    private static final MethodType LOAD_CLASS_CLASSLOADER = MethodType.methodType(ServiceLoader.class, Class.class,
            ClassLoader.class);

    public static void main(String[] args)
            throws Throwable {
        MethodHandles.Lookup publicLookup = MethodHandles.lookup();
        final MethodHandle handle = publicLookup.findStatic(ServiceLoader.class, "load", LOAD_CLASS_CLASSLOADER);
        final ServiceLoader serviceLoader = (ServiceLoader) handle.invokeExact(Main.class, Main.class.getClassLoader());
        System.out.println(serviceLoader);
    }

    public static String concat(String a) {
        return a + "1";
    }

}
