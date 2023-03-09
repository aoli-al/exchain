package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.objects.AffectedVarResult
import al.aoli.exchain.runtime.objects.SourceType
import soot.SootMethod
import soot.jimple.AbstractJimpleValueSwitch
import soot.jimple.IfStmt
import soot.jimple.Stmt

class SourceVarAnalyzer(affectedVarResults: List<AffectedVarResult>) :
    AbstractJimpleValueSwitch<MutableSet<Int>>() {
  val sourceBranches = mutableMapOf<String, MutableMap<Int, MutableSet<Pair<Int, SourceType>>>>()

  init {
    for (affectedVarResult in affectedVarResults) {
      for (sourceBranch in affectedVarResult.sourceLines) {
        sourceBranches
            .getOrPut(affectedVarResult.getSootMethodSignature()) { mutableMapOf() }
            .getOrPut(sourceBranch.first) { mutableSetOf() }
            .add(Pair(affectedVarResult.label, sourceBranch.second))
      }
    }
  }

  override fun defaultCase(obj: Any?) {
    result = null
  }

  fun process(stmt: Stmt, method: SootMethod): Set<Int>? {
    val tags = sourceBranches[method.signature]?.get(stmt.javaSourceStartLineNumber) ?: return null
    result = mutableSetOf()
    for (tag in tags) {
      when (tag.second) {
        SourceType.INVOKE -> {
          if (stmt.containsInvokeExpr()) {
            result.add(tag.first)
          }
        }
        SourceType.FIELD -> {
          if (stmt.containsFieldRef()) {
            result.add(tag.first)
          }
        }
        SourceType.JUMP -> {
          if (stmt is IfStmt) {
            result.add(tag.first)
          }
        }
        SourceType.ARRAY -> {
          if (stmt.containsArrayRef()) {
            result.add(tag.first)
          }
        }
      }
    }
    return result.ifEmpty { null }
  }
}
