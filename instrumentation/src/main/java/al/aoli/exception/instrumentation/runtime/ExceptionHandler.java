package al.aoli.exception.instrumentation.runtime;

public class ExceptionHandler {
    static public void onException(Throwable e) throws Throwable {
        e.printStackTrace();
        throw e;
    }
}
