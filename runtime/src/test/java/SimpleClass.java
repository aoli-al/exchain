public class SimpleClass {
    public void dummyMethod() {
        System.out.println("dummy");
    }
    public SimpleClass foo = null;
    public int a;

    public void testSourceFieldMethodCall() {
        foo.dummyMethod();
    }

    public void testSourceVarMethodCall() {
        SimpleClass bar = null;
        bar.dummyMethod();
    }

    public void testSourceFieldFieldAccess() {
        int b = foo.a;
    }

    public void testSourceVarFieldAccess() {
        SimpleClass bar = null;
        int b = bar.a;
    }

    public void testSourceVarBranch() {
        int b = 5;

        if (a * foo.a + 7 != b * 2.5) {
            throw new RuntimeException();
        }
    }
}