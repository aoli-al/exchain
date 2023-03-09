package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.objects.AffectedVarResult
import soot.Local
import soot.SootMethod
import soot.Value
import soot.jimple.AbstractJimpleValueSwitch
import soot.jimple.ArrayRef
import soot.jimple.AssignStmt
import soot.jimple.BinopExpr
import soot.jimple.BreakpointStmt
import soot.jimple.CastExpr
import soot.jimple.EnterMonitorStmt
import soot.jimple.ExitMonitorStmt
import soot.jimple.Expr
import soot.jimple.FieldRef
import soot.jimple.GotoStmt
import soot.jimple.IdentityStmt
import soot.jimple.IfStmt
import soot.jimple.InstanceFieldRef
import soot.jimple.InvokeExpr
import soot.jimple.InvokeStmt
import soot.jimple.LookupSwitchStmt
import soot.jimple.NopStmt
import soot.jimple.ParameterRef
import soot.jimple.RetStmt
import soot.jimple.ReturnStmt
import soot.jimple.ReturnVoidStmt
import soot.jimple.StmtSwitch
import soot.jimple.TableSwitchStmt
import soot.jimple.ThrowStmt
import soot.tagkit.Tag

class PropagationStmtSwitch(
    val analyzer: Analyzer,
    val affectedVarResult: AffectedVarResult?,
    val method: SootMethod
) : StmtSwitch, AbstractJimpleValueSwitch<Set<Tag>>() {

  override fun caseBreakpointStmt(stmt: BreakpointStmt) {}

  override fun caseInvokeStmt(stmt: InvokeStmt) {
    stmt.invokeExpr.apply(this)
  }

  override fun caseAssignStmt(stmt: AssignStmt) {
    stmt.rightOp.apply(this)
    stmt.rightOpBox.addAll(result)
    updateValue(stmt.leftOp, result)
  }

  fun updateValue(value: Value, result: Set<Tag>?) {
    if (result == null) return
    if (value is FieldRef) {
      val field = value.field
      if (field.declaringClass?.name != null) {
        val key = field.declaringClass.name + "." + field.name
        for (labelTag in result.filterIsInstance<LabelTag>()) {
          analyzer.fieldTaints.getOrPut(key) { mutableSetOf() }.add(labelTag.label)
        }
      }
    } else if (value is Local) {
      analyzer.localMap.getOrPut(value) { mutableSetOf() }.addAll(result)
    } else if (value is ArrayRef) {
      updateValue(value.base, result)
    }
  }

  override fun caseIdentityStmt(stmt: IdentityStmt) {
    stmt.rightOp.apply(this)
    stmt.rightOpBox.addAll(result)
    updateValue(stmt.leftOp, result)
  }

  override fun caseEnterMonitorStmt(stmt: EnterMonitorStmt) {}

  override fun caseExitMonitorStmt(stmt: ExitMonitorStmt) {}

  override fun caseGotoStmt(stmt: GotoStmt) {}

  override fun caseIfStmt(stmt: IfStmt) {
    //        if (affectedVarResult != null && stmt.javaSourceStartLineNumber in
    // affectedVarResult.sourceLines) {
    //            stmt.condition.apply(this)
    //            stmt.conditionBox.addAll(result)
    //            for (labelTag in stmt.conditionBox.tags.filterIsInstance<LabelTag>()) {
    //                analyzer.addEdge(affectedVarResult.label, labelTag.label)
    //            }
    //        }
  }

  override fun caseLookupSwitchStmt(stmt: LookupSwitchStmt) {}

  override fun caseNopStmt(stmt: NopStmt) {}

  override fun caseRetStmt(stmt: RetStmt) {}

  override fun caseReturnStmt(stmt: ReturnStmt) {
    var updated = false
    stmt.op.apply(this)
    for (tag in result) {
      if (tag !in method.tags) {
        method.addTag(tag)
        updated = true
      }
    }

    if (updated) {
      val dependencies = analyzer.methodDependency.getOrPut(method) { mutableSetOf() }
      for (dependency in dependencies) {
        if (dependency !in analyzer.workList) {
          analyzer.workList.add(dependency)
        }
      }
    }
  }

  override fun caseReturnVoidStmt(stmt: ReturnVoidStmt) {}

  override fun caseTableSwitchStmt(stmt: TableSwitchStmt) {}

  override fun caseThrowStmt(stmt: ThrowStmt) {}

  fun processBinopExpr(binopExpr: BinopExpr) {
    binopExpr.op1.apply(this)
    binopExpr.op1Box.addAll(result)
    binopExpr.op2.apply(this)
    binopExpr.op2Box.addAll(result)
    result = (binopExpr.op1Box.tags + binopExpr.op2Box.tags).filterIsInstance<LabelTag>().toSet()
  }

  fun processInvokeExpr(invokeExpr: InvokeExpr) {
    if (invokeExpr.method == null) {
      return
    }
    analyzer.methodDependency.getOrPut(invokeExpr.method) { mutableSetOf() }.add(method)
    val paramList =
        analyzer.methodParameterTaintMap.getOrPut(invokeExpr.method) {
          MutableList(invokeExpr.useBoxes.size) { mutableSetOf() }
        }

    var updated = false
    for (index in invokeExpr.useBoxes.indices) {
      val useBox = invokeExpr.useBoxes[index]
      useBox.value.apply(this)
      useBox.addAll(result)
      for (labelTag in useBox.tags.filterIsInstance<LabelTag>()) {
        if (labelTag !in paramList[index]) {
          paramList[index].add(labelTag)
          updated = true
        }
      }
    }
    if (updated && invokeExpr.method !in analyzer.workList) {
      analyzer.workList.add(invokeExpr.method)
    }
    result = invokeExpr.method.tags.filterIsInstance<LabelTag>().toSet()
  }

  fun processExpr(expr: Expr) {
    val out = mutableSetOf<Tag>()
    for (useBox in expr.useBoxes) {
      useBox.value.apply(this)
      useBox.addAll(result)
      out.addAll(useBox.tags.filterIsInstance<LabelTag>())
    }
    result = out
  }

  override fun caseParameterRef(v: ParameterRef) {
    val idx = if (method.isStatic) v.index else v.index + 1
    analyzer.methodParameterTaintMap[method]?.get(idx)?.let { result = it }
  }

  override fun caseLocal(v: Local) {
    //        if (affectedVarResult?.sourceVars?.contains(v.number) == true) {
    //            for (useBox in v.useBoxes) {
    //                for (labelTag in useBox.tags.filterIsInstance<LabelTag>()) {
    //                    analyzer.addEdge(affectedVarResult.label, labelTag.label)
    //                }
    //            }
    //        }
    //        result = if (affectedVarResult?.affectedVars?.contains(v.number) == true) {
    //            setOf(LabelTag.get(affectedVarResult.label))
    //        } else {
    //            emptySet()
    //        }
    result += analyzer.localMap.getOrDefault(v, mutableSetOf())
  }

  override fun caseInstanceFieldRef(v: InstanceFieldRef) {
    val fieldClass = v.field.declaringClass?.name ?: ""
    v.base.apply(this)
    v.baseBox.addAll(result)
    val key = fieldClass + "." + v.field.name
    result =
        analyzer.fieldTaints.getOrDefault(key, mutableSetOf()).map { LabelTag.get(it) }.toSet() +
            v.baseBox.tags.filterIsInstance<LabelTag>()
    v.field.addAll(result)

    val className =
        affectedVarResult
            ?.clazz
            ?.substring(1, affectedVarResult.clazz.length - 1)
            ?.replace("/", ".")
    //        if (v.field.declaringClass?.name == className) {
    //            if (affectedVarResult?.sourceFields?.contains(v.field.name) == true) {
    //                for (labelTag in v.field.tags.filterIsInstance<LabelTag>()) {
    //                    analyzer.addEdge(affectedVarResult.label, labelTag.label)
    //                }
    //            }
    //        }
  }

  override fun caseCastExpr(v: CastExpr) {
    v.op.apply(this)
    v.opBox.addAll(result)
  }

  override fun defaultCase(obj: Any) {
    result = emptySet()
    when (obj) {
      is BinopExpr -> processBinopExpr(obj)
      is InvokeExpr -> processInvokeExpr(obj)
      is Expr -> processExpr(obj)
    }
  }
}
