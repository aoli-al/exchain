import al.aoli.exchain.instrumentation.analyzers.AffectedVarDriver;
import org.junit.jupiter.api.Test;

public class AffectedVariableAnalyzerTest {

    static class Dummy {
        int a;
        int b;
        int c;
        int d;

        // Affected vars are a, b
        void scene() {
            for (int i = 0; i < 100; i++) {
                d = 0;
                try {
                    possibleThrown();
                    a = 1;

                    if (a == 0) {

                    }
                } catch (Throwable e) {
                    System.out.println("?");
                    b = 2;
                }
                c = 3;
            }
        }

        void possibleThrown() {
            throw new RuntimeException();
        }
    }

    @Test
    void testLoadClass() {
        AffectedVarDriver.INSTANCE.analyzeAffectedVar(
                Dummy.class, "scene()V",
                14, 32);
    }
}
