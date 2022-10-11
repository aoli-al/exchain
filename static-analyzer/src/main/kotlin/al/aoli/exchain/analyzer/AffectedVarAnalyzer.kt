package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.analyzers.AffectedVarResult
import soot.Local
import soot.SootMethod
import soot.jimple.*

class AffectedVarAnalyzer(val affectedVarResult: AffectedVarResult, val method: SootMethod, val stmt: Stmt):
    AbstractJimpleValueSwitch<Boolean>()  {

    override fun caseInstanceFieldRef(v: InstanceFieldRef) {
        result = false
        if (v.field.declaringClass?.name == affectedVarResult.getSootClassName()) {
            val index = affectedVarResult.affectedFieldName.indexOf(v.field.name)
            if (index != -1 && affectedVarResult.affectedFieldLine[index] == stmt.javaSourceStartLineNumber)  {
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
        val index = affectedVarResult.affectedLocalName.indexOf(name)
        if (index != -1 && affectedVarResult.affectedLocalLine[index] == stmt.javaSourceStartLineNumber) {
            result = true
        }
    }

    override fun defaultCase(obj: Any) {
        result = false
    }
}