package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.objects.AffectedVarResult
import soot.jimple.DefinitionStmt
import soot.jimple.Stmt
import soot.jimple.infoflow.InfoflowManager
import soot.jimple.infoflow.data.AccessPath
import soot.jimple.infoflow.data.SootMethodAndClass
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo

class SourceSinkManager(val methodSignature: String, val result: AffectedVarResult,
                        val sourceVarAnalyzer: SourceVarAnalyzer):
    ISourceSinkManager {
    override fun initialize() {
    }

    override fun getSourceInfo(stmt: Stmt, manager: InfoflowManager): SourceInfo? {
        val currentMethod = manager.icfg.getMethodOf(stmt) ?: return null
        if (currentMethod.signature == methodSignature) {
            if (stmt is DefinitionStmt) {
                val analyzer = AffectedVarAnalyzer(result, currentMethod, stmt)
                stmt.leftOp.apply(analyzer)
                if (analyzer.result) {
                    val targetAp = manager.accessPathFactory.createAccessPath(stmt.leftOp, true)
                    return SourceInfo(MethodSourceSinkDefinition(SootMethodAndClass(currentMethod)), targetAp)
                }
            }
        }
        return null
    }

    override fun getSinkInfo(stmt: Stmt, manager: InfoflowManager, ap: AccessPath?): SinkInfo? {
        val currentMethod = manager.icfg.getMethodOf(stmt) ?: return null
        val result = sourceVarAnalyzer.process(stmt, currentMethod)
        if (result != null) {
            return SinkInfo(LabeledSinkDefinition(SootMethodAndClass(currentMethod), result))
        }
        return null
    }
}