package al.aoli.exchain.runtime.analyzers

import al.aoli.exchain.runtime.objects.SourceType
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException
import java.lang.RuntimeException

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
        assert(visitor.methodVisitor!!.sourceLines.contains(Pair(28, SourceType.INVOKE)))
    }

    @Test
    fun testSourceVarMethodCall() {
        val resource = AffectedVarClassReader::class.java
            .getResourceAsStream("/bytecode/SimpleClass.class")!!.readAllBytes()
        val cr = AffectedVarClassReader(resource)
        val e = NullPointerException("dummy exception")
        val visitor = AffectedVarClassVisitor(e, 3, -1, true, true,
            "LSimpleClass;",
            "testSourceVarMethodCall()V",
            cr)
        cr.accept(visitor, 0)
        assert(visitor.methodVisitor!!.sourceFields.isEmpty())
        assert(visitor.methodVisitor!!.sourceVars.contains(1))
        assert(visitor.methodVisitor!!.sourceLines.contains(Pair(14, SourceType.INVOKE)))
    }

    @Test
    fun testSourceFieldMethodCall() {
        val resource = AffectedVarClassReader::class.java
            .getResourceAsStream("/bytecode/SimpleClass.class")!!.readAllBytes()
        val cr = AffectedVarClassReader(resource)
        val e = NullPointerException("dummy exception")
        val visitor = AffectedVarClassVisitor(e, 4, -1, true, true,
            "LSimpleClass;",
            "testSourceFieldMethodCall()V",
            cr)
        cr.accept(visitor, 0)
        assert(visitor.methodVisitor!!.sourceFields.contains("foo"))
        assert(visitor.methodVisitor!!.sourceVars.isEmpty())
        assert(visitor.methodVisitor!!.sourceLines.contains(Pair(9, SourceType.INVOKE)))
    }

    @Test
    fun testSourceFieldFieldAccess() {
        val resource = AffectedVarClassReader::class.java
            .getResourceAsStream("/bytecode/SimpleClass.class")!!.readAllBytes()
        val cr = AffectedVarClassReader(resource)
        val e = NullPointerException("dummy exception")
        val visitor = AffectedVarClassVisitor(e, 4, -1, true, true,
            "LSimpleClass;",
            "testSourceFieldFieldAccess()V",
            cr)
        cr.accept(visitor, 0)
        assert(visitor.methodVisitor!!.sourceFields.contains("foo"))
        assert(visitor.methodVisitor!!.sourceVars.isEmpty())
        assert(visitor.methodVisitor!!.sourceLines.contains(Pair(18, SourceType.FIELD)))
    }

    @Test
    fun testSourceVarFieldAccess() {
        val resource = AffectedVarClassReader::class.java
            .getResourceAsStream("/bytecode/SimpleClass.class")!!.readAllBytes()
        val cr = AffectedVarClassReader(resource)
        val e = NullPointerException("dummy exception")
        val visitor = AffectedVarClassVisitor(e, 3, -1, true, true,
            "LSimpleClass;",
            "testSourceVarFieldAccess()V",
            cr)
        cr.accept(visitor, 0)
        assert(visitor.methodVisitor!!.sourceFields.isEmpty())
        assert(visitor.methodVisitor!!.sourceVars.contains(1))
        assert(visitor.methodVisitor!!.sourceLines.contains(Pair(23, SourceType.FIELD)))
    }

    @Test
    fun testSourceVarBranch() {
        val resource = AffectedVarClassReader::class.java
            .getResourceAsStream("/bytecode/SimpleClass.class")!!.readAllBytes()
        val cr = AffectedVarClassReader(resource)
        val e = RuntimeException("dummy exception")
        val visitor = AffectedVarClassVisitor(e, 35, -1, true, true,
            "LSimpleClass;",
            "testSourceVarBranch()V",
            cr)
        cr.accept(visitor, 0)
        assert(visitor.methodVisitor!!.sourceFields.contains("a"))
        assert(visitor.methodVisitor!!.sourceFields.contains("foo"))
        assert(visitor.methodVisitor!!.sourceVars.contains(1))
        assert(visitor.methodVisitor!!.sourceLines.contains(Pair(29, SourceType.JUMP)))
    }
}