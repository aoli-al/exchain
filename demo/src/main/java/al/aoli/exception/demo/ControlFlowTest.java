package al.aoli.exception.demo;

public class ControlFlowTest {
    public int scene1() {
        try {
            foo();
        } catch (Exception e1) {
            if (e1 instanceof NullPointerException) {
                System.out.println("caught");
                return 2;
            }
        }
        return 1;
    }

    static class Exception1 extends RuntimeException {}
    static class Exception2 extends RuntimeException {}

    public void scene15() {
        try {
            throw new RuntimeException();
        } catch (RuntimeException e1) {
            try {
                throw new RuntimeException(e1);
            } catch (RuntimeException e2) {
                try {
                    throw e2.getCause();
                } catch (Throwable e3) {
                    throw new RuntimeException(e3);
                }
            }
        }
    }

    public void scene2() {
        try {
            throw new Exception1();
        } catch (Exception e1) {
            try {
                throw new Exception2();
            } catch (Exception e2) {
            }
            throw e1;
        }
    }

    public void scene9() {
        try {
            throw new Exception1();
        } catch (Exception e1) {
            try {
                throw new Exception2();
            } catch (Exception e2) {
                throw e2;
            }
        }
    }

    public void scene3() {
        try {
            foo();
        } catch (Exception e) {
            foo();
        }
    }

    public void scene4() {
        try {
            foo();
        }
        catch (Exception e) {
            throw e;
        }
    }

    public void scene6() {
        try {
            foo();
        }
        catch (Exception e) {
            throw new RuntimeException("2");
        }
    }

    public void scene5() {
        try {
            foo();
        }
        catch (Exception e) {
            ex();
        }
    }

    public void scene7() {
        try {
            foo();
        }
        catch (Exception e) {
            foo();
        }
    }

    public void scene8() {
        synchronized (this) {
            foo();
        }
    }

    public void iden(Throwable e) {
        throw (RuntimeException) e;
    }

    public void scene10() {
        try {
            foo();
        }
        catch (RuntimeException e) {
            if (bar()) {
                return;
            }
            else if (bar()) {
                throw new RuntimeException();
            }
        }
    }


    public void foo() {
        throw new RuntimeException("1");
    }

    public void ex() {
        throw new RuntimeException("3");
    }
    public boolean bar() {
        return true;
    }
}
