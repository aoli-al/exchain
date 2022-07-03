package al.aoli.exception.demo;

public class DataFlowTest {
    public void scene1() {
        Object o = null;

        try {
            o = createObjectWithException();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        String s = null;

        try {
            s = o.toString();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        System.out.println(s.length());
    }

    public void scene2() {

    }

    public Object createObjectWithException() {
        throw new RuntimeException("exception");
    }

    public void methodWithException() {
        throw new RuntimeException("exception");
    }
}
