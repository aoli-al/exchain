package al.aoli.exchain.demo;

public class CustomizedThread extends Thread {
    public static Object c1;
    public static Object c2;
    public static Object c3;
    public static Object c4;
    private int i = 0;

    public CustomizedThread(int i) {
        super("CustomizedThread");
        this.i = i;
    }

    @Override
    public void run() {
        if (i == 0) {
            c1 = new Class1();
        } else if (i == 1) {
            c2 = new Class2();
        } else if (i == 2) {
            c3 = new Class3();
        } else if (i == 3) {
            c4 = new Class4();
        } else if (i == 4) {
            c4 = new Class5();
        } else if (i == 5) {
            c4 = new Class6();
        } else if (i == 6) {
            c4 = new Class7();
        }

        super.run();
    }
}
