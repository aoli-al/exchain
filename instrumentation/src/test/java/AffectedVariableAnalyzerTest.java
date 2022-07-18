import al.aoli.exchain.instrumentation.analyzers.AffectedVarDriver;
import org.junit.jupiter.api.Test;

public class AffectedVariableAnalyzerTest {

    static class Dummy {
        int foo;
        void test() {
            try {
                possibleThrown();
                foo = 4;
            } catch (Throwable e) {
                System.out.println("?");
                foo = 185;
            }
        }

        void possibleThrown() {
            throw new RuntimeException();
        }
    }

    @Test
    void testLoadClass() {
        AffectedVarDriver.INSTANCE.analyzeAffectedVar(
                Dummy.class, "test()V",
                );
    }
}
