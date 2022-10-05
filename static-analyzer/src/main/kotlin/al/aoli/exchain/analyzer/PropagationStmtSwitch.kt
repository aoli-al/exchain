package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.analyzers.AffectedVarResult
import soot.Local
import soot.SootMethod
import soot.jimple.*
import java.awt.Label

class PropagationStmtSwitch(val affectedVarResultAnalyzer: AffectedVarResultAnalyzer,
                            val affectedVarResult: AffectedVarResult?,
                            val method: SootMethod): StmtSwitch {

    val exprSwitch = PropagationExprSwitch(affectedVarResultAnalyzer, affectedVarResult, method)

    override fun caseBreakpointStmt(stmt: BreakpointStmt) {
    }

    override fun caseInvokeStmt(stmt: InvokeStmt) {
        stmt.invokeExpr.apply(exprSwitch)
    }

    override fun caseAssignStmt(stmt: AssignStmt) {
        stmt.rightOp.apply(exprSwitch)
        stmt.rightOpBox.addAll(exprSwitch.result)
        stmt.leftOpBox.addAll(stmt.rightOpBox.tags.filterIsInstance<LabelTag>().toSet())
        val leftOp = stmt.leftOp
        if (leftOp is FieldRef) {
            val field = leftOp.field
            if (field.declaringClass?.name != null) {
                val key = field.declaringClass.name + "." + field.name
                for (labelTag in stmt.leftOpBox.tags.filterIsInstance<LabelTag>()) {
                    affectedVarResultAnalyzer.fieldTaints.getOrDefault(key, mutableSetOf())
                        .add(labelTag.label)
                }
            }
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

    override fun defaultCase(obj: Any) {
    }
}