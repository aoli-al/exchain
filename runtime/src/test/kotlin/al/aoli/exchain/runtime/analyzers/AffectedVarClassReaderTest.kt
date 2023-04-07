package al.aoli.exchain.runtime.analyzers

import al.aoli.exchain.runtime.objects.SourceType
import java.lang.RuntimeException
import org.junit.jupiter.api.Test

internal class AffectedVarClassReaderTest {

  @Test
  fun testSourceLine() {
    val resource =
        AffectedVarClassReader::class
            .java
            .getResourceAsStream("/bytecode/GraalCompat.class")!!
            .readAllBytes()
    val cr = AffectedVarClassReader(resource)
    val e = ClassNotFoundException("dummy exception")
    val visitor =
        AffectedVarClassVisitor(
            e, 4, 36, true, true, "Lorg/apache/tomcat/util/compat/GraalCompat;", "<clinit>()V", cr)
    cr.accept(visitor, 0)
    assert(visitor.methodVisitor!!.sourceLines.contains(Pair(28, SourceType.INVOKE)))
  }

  @Test
  fun testSourceVarMethodCall() {
    val resource =
        AffectedVarClassReader::class
            .java
            .getResourceAsStream("/bytecode/SimpleClass.class")!!
            .readAllBytes()
    val cr = AffectedVarClassReader(resource)
    val e = NullPointerException("dummy exception")
    val visitor =
        AffectedVarClassVisitor(
            e, 3, -1, true, true, "LSimpleClass;", "testSourceVarMethodCall()V", cr)
    cr.accept(visitor, 0)
    assert(visitor.methodVisitor!!.sourceField.isEmpty())
    assert(visitor.methodVisitor!!.sourceLocalVariable.contains(1))
    assert(visitor.methodVisitor!!.sourceLines.contains(Pair(14, SourceType.INVOKE)))
  }

  @Test
  fun testSourceFieldMethodCall() {
    val resource =
        AffectedVarClassReader::class
            .java
            .getResourceAsStream("/bytecode/SimpleClass.class")!!
            .readAllBytes()
    val cr = AffectedVarClassReader(resource)
    val e = NullPointerException("dummy exception")
    val visitor =
        AffectedVarClassVisitor(
            e, 4, -1, true, true, "LSimpleClass;", "testSourceFieldMethodCall()V", cr)
    cr.accept(visitor, 0)
    assert(visitor.methodVisitor!!.sourceField.contains("foo"))
    assert(visitor.methodVisitor!!.sourceLocalVariable.isEmpty())
    assert(visitor.methodVisitor!!.sourceLines.contains(Pair(9, SourceType.INVOKE)))
  }

  @Test
  fun testSourceFieldFieldAccess() {
    val resource =
        AffectedVarClassReader::class
            .java
            .getResourceAsStream("/bytecode/SimpleClass.class")!!
            .readAllBytes()
    val cr = AffectedVarClassReader(resource)
    val e = NullPointerException("dummy exception")
    val visitor =
        AffectedVarClassVisitor(
            e, 4, -1, true, true, "LSimpleClass;", "testSourceFieldFieldAccess()V", cr)
    cr.accept(visitor, 0)
    assert(visitor.methodVisitor!!.sourceField.contains("foo"))
    assert(visitor.methodVisitor!!.sourceLocalVariable.isEmpty())
    assert(visitor.methodVisitor!!.sourceLines.contains(Pair(18, SourceType.FIELD)))
  }

  @Test
  fun testSourceVarFieldAccess() {
    val resource =
        AffectedVarClassReader::class
            .java
            .getResourceAsStream("/bytecode/SimpleClass.class")!!
            .readAllBytes()
    val cr = AffectedVarClassReader(resource)
    val e = NullPointerException("dummy exception")
    val visitor =
        AffectedVarClassVisitor(
            e, 3, -1, true, true, "LSimpleClass;", "testSourceVarFieldAccess()V", cr)
    cr.accept(visitor, 0)
    assert(visitor.methodVisitor!!.sourceField.isEmpty())
    assert(visitor.methodVisitor!!.sourceLocalVariable.contains(1))
    assert(visitor.methodVisitor!!.sourceLines.contains(Pair(23, SourceType.FIELD)))
  }

  @Test
  fun testSourceVarBranch() {
    val resource =
        AffectedVarClassReader::class
            .java
            .getResourceAsStream("/bytecode/SimpleClass.class")!!
            .readAllBytes()
    val cr = AffectedVarClassReader(resource)
    val e = RuntimeException("dummy exception")
    val visitor =
        AffectedVarClassVisitor(
            e, 35, -1, true, true, "LSimpleClass;", "testSourceVarBranch()V", cr)
    cr.accept(visitor, 0)
    assert(visitor.methodVisitor!!.sourceField.contains("a"))
    assert(visitor.methodVisitor!!.sourceField.contains("foo"))
    assert(visitor.methodVisitor!!.sourceLocalVariable.contains(1))
    assert(visitor.methodVisitor!!.sourceLines.contains(Pair(29, SourceType.JUMP)))
  }

  @Test
  fun testSourceVarFSEditLogLoader() {
    val resource =
        AffectedVarClassReader::class
            .java
            .getResourceAsStream("/bytecode/FSEditLogLoader.class")!!
            .readAllBytes()
    val cr = AffectedVarClassReader(resource)
    val e = RuntimeException("dummy exception")
    val visitor =
        AffectedVarClassVisitor(
            e,
            2645,
            6223,
            false,
            true,
            "Lorg/apache/hadoop/hdfs/server/namenode/FSEditLogLoader;",
            "loadEditRecords(Lorg/apache/hadoop/hdfs/server/namenode/EditLogInputStream;" +
                "ZJJLorg/apache/hadoop/hdfs/server/common/HdfsServerConstants\$StartupOption;Lorg/apache/hadoop/hdfs/server/namenode/MetaRecoveryContext;)J",
            cr)
    cr.accept(visitor, 0)
    assert(visitor.methodVisitor!!.sourceField.isEmpty())
    assert(visitor.methodVisitor!!.sourceLocalVariable.contains(51))
    assert(visitor.methodVisitor!!.sourceLines.contains(Pair(296, SourceType.JUMP)))
  }

  @Test
  fun testRedundantLocalVar() {
    val resource =
        AffectedVarClassReader::class
            .java
            .getResourceAsStream("/bytecode/DefaultPageFactory.class")!!
            .readAllBytes()
    val cr = AffectedVarClassReader(resource)
    val e = RuntimeException("dummy exception")
    val visitor =
        AffectedVarClassVisitor(
            e,
            112,
            244,
            false,
            true,
            "Lorg/apache/wicket/session/DefaultPageFactory;",
            "newPage(Ljava/lang/Class;)Lorg/apache/wicket/request/component/IRequestablePage;",
            cr)
    cr.accept(visitor, 0)
    println(visitor.methodVisitor!!.affectedVars)
    //        assert(visitor.methodVisitor!!.sourceField.isEmpty())
    //        assert(visitor.methodVisitor!!.sourceLocalVariable.contains(51))
    //        assert(visitor.methodVisitor!!.sourceLines.contains(Pair(296, SourceType.JUMP)))
  }

  @Test
  fun testAffectedInsns() {
    val resource =
        AffectedVarClassReader::class
            .java
            .getResourceAsStream("/bytecode/RawLocalFileSystem.class")!!
            .readAllBytes()
    val cr = AffectedVarClassReader(resource)
    val e = RuntimeException("dummy exception")
    val visitor =
        AffectedVarClassVisitor(
            e,
            186,
            -1,
            false,
            true,
            "Lorg/apache/hadoop/fs/RawLocalFileSystem;",
            "getFileLinkStatusInternal(Lorg/apache/hadoop/fs/Path;Z)Lorg/apache/hadoop/fs/FileStatus;",
            cr)
    cr.accept(visitor, 0)
    println(visitor.methodVisitor!!.affectedVars)
    //        assert(visitor.methodVisitor!!.sourceField.isEmpty())
    //        assert(visitor.methodVisitor!!.sourceLocalVariable.contains(51))
    //        assert(visitor.methodVisitor!!.sourceLines.contains(Pair(296, SourceType.JUMP)))
  }
}
