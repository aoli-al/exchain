package al.aoli.exchain.instrumentation.runtime

import al.aoli.exchain.instrumentation.analyzers.AffectedVarDriver
import al.aoli.exchain.instrumentation.analyzers.AffectedVarResult
import al.aoli.exchain.instrumentation.runtime.exceptions.ExceptionInjector
import al.aoli.exchain.instrumentation.server.ExceptionServiceImpl
import edu.columbia.cs.psl.phosphor.runtime.Taint
import edu.columbia.cs.psl.phosphor.struct.PowerSetTree.SetNode
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag

object ExceptionRuntime {

    @JvmStatic
    fun onException(e: Throwable, origin: String) {
        if (!ExceptionServiceImpl.started) return
        ExceptionInjector.thrownExceptions(e)
//        ExceptionTreeAnalyzer.push(e, origin)
    }

    @JvmStatic
    fun onCatch() {
        if (!ExceptionServiceImpl.started) return
//        ExceptionTreeAnalyzer.catchEnd()
    }

    @JvmStatic
    fun onCatchBegin(e: Throwable) {
        if (!ExceptionServiceImpl.started) return
        ExceptionInjector.thrownExceptions(e)
//        ExceptionTreeAnalyzer.exceptionCaught(e)
    }

    @JvmStatic
    fun onCatchWithException(e: Throwable, origin: String) {
        if (!ExceptionServiceImpl.started) return
        ExceptionInjector.thrownExceptions(e)
//        ExceptionTreeAnalyzer.catchWithException(e, origin)
    }

    @JvmStatic
    fun taintEnabled(): Boolean {
        return AffectedVarDriver.taintEnabled
    }

    @JvmStatic
    fun onExceptionStackInfo(clazz: String, method: String, throwLocation: Long, catchLocation: Long, isThrowInsn: Boolean): AffectedVarResult? {
        return AffectedVarDriver.analyzeAffectedVar(clazz, method, throwLocation, catchLocation, isThrowInsn)
    }

    @JvmStatic
    fun taintObject(obj: Any?, idx: Int, thread: Thread, depth: Int, exception: Any) {
        if (obj == null) return
        if (obj !is TaintedWithObjTag) return
        AffectedVarDriver.taintObject(obj, exception)
    }

    @JvmStatic
    fun taintFields(obj: Any?, affectedVarResult: AffectedVarResult, exception: Any) {
        if (obj == null) return
        AffectedVarDriver.updateAffectedFields(obj, affectedVarResult, exception)
    }

    @JvmStatic
    fun updateTaint(obj: Any?, idx: Int, thread: Thread, depth: Int, exception: Any): Taint<*>? {
        if (obj == null) return null
        if (obj !is SetNode) return null
        return AffectedVarDriver.updateTaint(obj, exception)
    }

    @JvmStatic
    fun analyzeSourceVars(obj: Any?, exception: Any, location: String) {
        if (obj == null) return
        AffectedVarDriver.analyzeSourceVars(obj, exception, location)
    }

    @JvmStatic
    fun analyzeSourceFields(obj: Any?, affectedVarResult: AffectedVarResult, exception: Any, location: String) {
        if (obj == null) return
        AffectedVarDriver.analyzeSourceFields(obj, affectedVarResult, exception, location)
    }
}