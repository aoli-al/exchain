package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.analyzers.AffectedVarResult
import soot.Local
import soot.SootMethod
import soot.Value
import soot.jimple.*
import soot.tagkit.Tag
import java.awt.Label

class PropagationStmtSwitch(val affectedVarResultAnalyzer: AffectedVarResultAnalyzer,
                            val affectedVarResult: AffectedVarResult?,
                            val method: SootMethod): StmtSwitch, AbstractJimpleValueSwitch<Set<Tag>>() {

    val localMap = mutableMapOf<Local, MutableSet<Tag>>()

    override fun caseBreakpointStmt(stmt: BreakpointStmt) {
    }

    override fun caseInvokeStmt(stmt: InvokeStmt) {
        stmt.invokeExpr.apply(this)
    }

    override fun caseAssignStmt(stmt: AssignStmt) {
        stmt.rightOp.apply(this)
        stmt.rightOpBox.addAll(result)
        updateValue(stmt.leftOp, result)
    }

    fun updateValue(value: Value, result: Set<Tag>) {
        if (value is FieldRef) {
            val field = value.field
            if (field.declaringClass?.name != null) {
                val key = field.declaringClass.name + "." + field.name
                for (labelTag in result.filterIsInstance<LabelTag>()) {
                    affectedVarResultAnalyzer.fieldTaints
                        .getOrPut(key) { mutableSetOf() }
                        .add(labelTag.label)
                }
            }
        } else if (value is Local) {
            localMap.getOrPut(value) { mutableSetOf() }.addAll(result)
        } else if (value is ArrayRef) {
            updateValue(value.base, result)
        }
    }

    override fun caseIdentityStmt(stmt: IdentityStmt) {
    }

    override fun caseEnterMonitorStmt(stmt: EnterMonitorStmt) {
    }

    override fun caseExitMonitorStmt(stmt: ExitMonitorStmt) {
    }

    override fun caseGotoStmt(stmt: GotoStmt) {
    }

    override fun caseIfStmt(stmt: IfStmt) {
    }

    override fun caseLookupSwitchStmt(stmt: LookupSwitchStmt) {
    }

    override fun caseNopStmt(stmt: NopStmt) {
    }

    override fun caseRetStmt(stmt: RetStmt) {
    }

    override fun caseReturnStmt(stmt: ReturnStmt) {
    }

    override fun caseReturnVoidStmt(stmt: ReturnVoidStmt) {
    }

    override fun caseTableSwitchStmt(stmt: TableSwitchStmt) {
    }

    override fun caseThrowStmt(stmt: ThrowStmt) {
    }

    fun processBinopExpr(binopExpr: BinopExpr) {
        binopExpr.op1.apply(this)
        binopExpr.op1Box.addAll(result)
        binopExpr.op2.apply(this)
        binopExpr.op2Box.addAll(result)
        result = (binopExpr.op1Box.tags + binopExpr.op2Box.tags)
            .filterIsInstance<LabelTag>().toSet()
    }

    override fun caseLocal(v: Local) {
        if (affectedVarResult?.sourceVars?.contains(v.number) == true) {
            for (useBox in v.useBoxes) {
                for (labelTag in useBox.tags.filterIsInstance<LabelTag>()) {
                    affectedVarResultAnalyzer.addEdge(affectedVarResult.label, labelTag.label)
                }
            }
        }
        result = if (affectedVarResult?.affectedVars?.contains(v.number) == true) {
            setOf(LabelTag.get(affectedVarResult.label))
        } else {
            emptySet()
        }

        result += localMap.getOrDefault(v, mutableSetOf())
    }

    override fun caseInstanceFieldRef(v: InstanceFieldRef) {
        val fieldClass = v.field.declaringClass?.name ?: ""
        v.base.apply(this)
        v.baseBox.addAll(result)
        val key = fieldClass + "." + v.field.name
        result =
            affectedVarResultAnalyzer.fieldTaints.getOrDefault(key, mutableSetOf())
                .map { LabelTag.get(it) }.toSet() + v.baseBox.tags.filterIsInstance<LabelTag>()
        v.field.addAll(result)

        val className = affectedVarResult?.clazz
            ?.substring(1, affectedVarResult.clazz.length - 1)
            ?.replace("/", ".")
        if (v.field.declaringClass?.name == className) {
            if (affectedVarResult?.sourceFields?.contains(v.field.name) == true) {
                for (labelTag in v.field.tags.filterIsInstance<LabelTag>()) {
                    affectedVarResultAnalyzer.addEdge(affectedVarResult.label, labelTag.label)
                }
            }
        }
    }

    override fun caseCastExpr(v: CastExpr) {
        v.op.apply(this)
        v.opBox.addAll(result)
    }


    override fun defaultCase(obj: Any) {
        when (obj) {
            is BinopExpr -> processBinopExpr(obj)
            else -> {}
        }
        result = emptySet()
    }
}