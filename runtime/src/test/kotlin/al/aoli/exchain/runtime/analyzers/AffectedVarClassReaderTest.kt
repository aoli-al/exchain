package al.aoli.exchain.runtime.analyzers

import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException

internal class AffectedVarClassReaderTest {

    @Test
    fun testSourceLine() {
        val resource = AffectedVarClassReader::class.java
            .getResourceAsStream("/bytecode/GraalCompat.class")!!.readAllBytes()
        val cr = AffectedVarClassReader(resource)
        val e = ClassNotFoundException("dummy exception")
        val visitor = AffectedVarClassVisitor(e, 4, 36, true, true,
            "Lorg/apache/tomcat/util/compat/GraalCompat;",
            "<clinit>()V",
            cr)
        cr.accept(visitor, 0)
        println(visitor.methodVisitor?.sourceLines)
    }

    @Test
    fun testSourceLine2() {
        val cr = AffectedVarClassReader("com.sun.jmx.mbeanserver.Introspector")
        val e = ClassNotFoundException("dummy exception")
        val visitor = AffectedVarClassVisitor(e, 56, -1, true, true,
            "Lcom/sun/jmx/mbeanserver/Introspector",
            "getStandardMBeanInterface(Ljava/lang/Class;)Ljava/lang/Class;",
            cr)
        cr.accept(visitor, 0)
        println(visitor.methodVisitor?.sourceLines)
    }

}