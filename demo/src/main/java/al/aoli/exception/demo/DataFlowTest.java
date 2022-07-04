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
        int count = 0;
        while (count < 50) {
            try {
                functionWithException();
            } catch (Exception e) {
                count ++;
            }
        }
    }


    public void functionWithException() {
        throw new RuntimeException("exception");
    }
    public Object createObjectWithException() {
        throw new RuntimeException("exception");
    }

    public void methodWithException() {
        throw new RuntimeException("exception");
    }
}
