package al.aoli.exception.demo;

class Test {
    public void foo() {
        try {
            throw new RuntimeException("123");
        }
        catch (Exception e) {
        }
    }

}

public class Main {
    public static void main(String[] args) {
        Test t = new Test();
        t.foo();
    }
}