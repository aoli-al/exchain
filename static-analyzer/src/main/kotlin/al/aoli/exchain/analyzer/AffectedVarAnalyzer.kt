package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.objects.AffectedVarResult
import soot.Local
import soot.SootMethod
import soot.jimple.*

class AffectedVarAnalyzer(
    affectedVarResults: List<AffectedVarResult>,
) : AbstractJimpleValueSwitch<Boolean>() {
  val affectedVars = mutableMapOf<String, MutableSet<AffectedVarResult>>()
  var currentAffectedVar: AffectedVarResult? = null
  var currentStmt: Stmt? = null

  init {
    for (result in affectedVarResults) {

      affectedVars.getOrPut(result.getSootMethodSignature()) { mutableSetOf() }.add(result)
    }
  }

  fun process(method: SootMethod, stmt: Stmt): Set<Int> {
    val exceptions = mutableSetOf<Int>()
    if (stmt is DefinitionStmt) {
      val methodAffectedVars = affectedVars[method.signature] ?: return exceptions
      currentStmt = stmt
      for (methodAffectedVar in methodAffectedVars) {
        currentAffectedVar = methodAffectedVar
        result = false
        stmt.leftOp.apply(this)
        if (result) {
          exceptions.add(methodAffectedVar.label)
        }
      }
    }
    return exceptions
  }

  override fun caseInstanceFieldRef(v: InstanceFieldRef) {
    result = false
    if (v.field.declaringClass?.name == currentAffectedVar?.getSootClassName()) {
      val index = currentAffectedVar?.affectedFieldName?.indexOf(v.field.name) ?: -1
      if (index != -1 &&
          currentAffectedVar?.affectedFieldLine?.get(index) ==
              currentStmt?.javaSourceStartLineNumber) {
        result = true
      }
    }
  }

  override fun caseStaticFieldRef(v: StaticFieldRef) {
    result = false
    if (v.field.declaringClass?.name == currentAffectedVar?.getSootClassName()) {
      val index = currentAffectedVar?.affectedStaticFieldName?.indexOf(v.field.name) ?: -1
      if (index != -1 &&
          currentAffectedVar?.affectedStaticFieldLine?.get(index) ==
              currentStmt?.javaSourceStartLineNumber) {
        result = true
      }
    }
  }

  fun getLocalName(v: Local): String {
    var name = v.name
    if ("#" in name) {
      name = name.substring(0, name.indexOf("#"))
    }
    return name
  }

  override fun caseLocal(v: Local) {
    result = false
    val name = getLocalName(v)
    val index = currentAffectedVar?.affectedLocalName?.indexOf(name) ?: -1
    if (index != -1 &&
        currentAffectedVar?.affectedLocalLine?.get(index) ==
            currentStmt?.javaSourceStartLineNumber) {
      result = true
    }
  }

  override fun defaultCase(obj: Any) {
    result = false
  }
}
