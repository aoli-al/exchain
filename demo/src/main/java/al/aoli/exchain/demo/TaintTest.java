package al.aoli.exchain.demo;

public class TaintTest {

    static class Foo extends TaintTest {
        public int a = 23;
    }

    public void empty(byte[] a) {
    }

    public void call(byte[] a) {
        System.out.println(a);
    }

    /*
    Exception in thread "main" java.lang.NoSuchMethodError: 'long edu.columbia.cs.psl.phosphor.runtime.RuntimeBoxUnboxPropagator.parseUnsignedLong(java.lang.CharSequence, int, int, int, edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame)'
        at al.aoli.exchain.demo.TaintTest.crash(TaintTest.java:15)
        at al.aoli.exchain.demo.TaintTest.test(TaintTest.java:29)
        at al.aoli.exchain.demo.Main.main(Main.java:35)
     */
    public void crash() {
        Long l = Long.parseUnsignedLong("123456", 0, 5, 10);
    }

    /*
    output: edu.columbia.cs.psl.phosphor.struct.TaggedReferenceArray@467aecef
     */
    public synchronized void wrongOutput() {
        byte[] obj = new byte[] {46};
        call(obj);
        call(null);
    }

//    public void leak() {
//        String[] obj = new String[] {"123", "456"};
//        call(obj, obj);
//        call(null, (String[]) null);
//    }

    public synchronized void test() {
//        called2( null, null);
//        crash();
//        leak();
    }

    static {
        foo();
    }

    static void foo() {
    }


}
