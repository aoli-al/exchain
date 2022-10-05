package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.analyzers.AffectedVarResult
import soot.Local
import soot.RefType
import soot.SootMethod
import soot.jimple.*
import soot.tagkit.Host
import soot.tagkit.Tag
import java.awt.Label

class PropagationExprSwitch(val affectedVarResultAnalyzer: AffectedVarResultAnalyzer,
                            val affectedVarResult: AffectedVarResult?,
                            val method: SootMethod): AbstractJimpleValueSwitch<Set<Tag>>() {

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
    }

    override fun caseInstanceFieldRef(v: InstanceFieldRef) {
        val className = affectedVarResult?.clazz
            ?.substring(1, affectedVarResult.clazz.length - 1)
            ?.replace("/", ".")
        val fieldClass = v.field.declaringClass?.name ?: ""
        if (v.field.declaringClass?.name == className) {
            if (affectedVarResult?.sourceFields?.contains(v.field.name) == true) {
                for (labelTag in v.field.tags.filterIsInstance<LabelTag>()) {
                    affectedVarResultAnalyzer.addEdge(affectedVarResult.label, labelTag.label)
                }
            }
        }

        v.base.apply(this)
        v.baseBox.addAll(result)
        val key = fieldClass + "." + v.field.name
        result =
            affectedVarResultAnalyzer.fieldTaints.getOrDefault(key, mutableSetOf())
                .map { LabelTag.get(it) }.toSet() + v.baseBox.tags.filterIsInstance<LabelTag>()
        for (tag in result) {
            v.field.addTag(tag)
        }
        v.field.addAll(result)
    }

    override fun caseCastExpr(v: CastExpr) {
        v.op.apply(this)
        v.opBox.tags.addAll(result)
    }


    override fun defaultCase(obj: Any) {
        when (obj) {
            is BinopExpr -> processBinopExpr(obj)
            else -> {}
        }
        result = emptySet()
    }
}

