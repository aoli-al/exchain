package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.analyzers.AffectedVarResult
import soot.SootMethod
import soot.jimple.AssignStmt
import soot.jimple.DefinitionStmt
import soot.jimple.IdentityStmt
import soot.jimple.Stmt
import soot.jimple.infoflow.InfoflowManager
import soot.jimple.infoflow.data.AccessPath
import soot.jimple.infoflow.data.SootMethodAndClass
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo

class SourceSinkManager(val method: SootMethod, val result: AffectedVarResult, val sourceVarAnalyzer: SourceVarAnalyzer):
    ISourceSinkManager {
    override fun initialize() {
    }

    override fun getSourceInfo(stmt: Stmt, manager: InfoflowManager): SourceInfo? {
        val currentMethod = manager.icfg.getMethodOf(stmt)
        if (currentMethod.signature == method.signature) {
            if (stmt is DefinitionStmt) {
                val analyzer = AffectedVarAnalyzer(result, currentMethod, stmt)
                stmt.leftOp.apply(analyzer)
                if (analyzer.result) {
                    val targetAp = manager.accessPathFactory.createAccessPath(stmt.leftOp, true)
                    return SourceInfo(MethodSourceSinkDefinition(SootMethodAndClass(method)), targetAp)
                }
            }
        }
        return null
    }

    override fun getSinkInfo(stmt: Stmt, manager: InfoflowManager, ap: AccessPath?): SinkInfo? {
        val currentMethod = manager.icfg.getMethodOf(stmt)
        val result = sourceVarAnalyzer.process(stmt, currentMethod)
        if (result != null) {
            return SinkInfo(LabeledSinkDefinition(SootMethodAndClass(currentMethod), result))
        }
        return null
    }
}