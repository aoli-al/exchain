package al.aoli.exchain.instrumentation.runtime

import al.aoli.exchain.instrumentation.analyzers.AffectedVarDriver
import al.aoli.exchain.instrumentation.analyzers.AffectedVarResults
import al.aoli.exchain.instrumentation.runtime.exceptions.ExceptionInjector
import al.aoli.exchain.instrumentation.server.ExceptionServiceImpl
import edu.columbia.cs.psl.phosphor.runtime.Taint
import edu.columbia.cs.psl.phosphor.struct.PowerSetTree.SetNode

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
    fun onExceptionStackInfo(clazz: String, method: String, throwLocation: Long, catchLocation: Long, isThrowInsn: Boolean): AffectedVarResults? {
        return AffectedVarDriver.analyzeAffectedVar(clazz, method, throwLocation, catchLocation, isThrowInsn)
    }

    @JvmStatic
    fun taintObject(obj: Any?, idx: Int, thread: Thread, depth: Int, exception: Any): Taint<*>? {
        if (obj == null) return null
        if (obj !is SetNode) return null
        return AffectedVarDriver.taintAffectedVar(obj, idx, thread, depth, exception)
    }

    @JvmStatic
    fun analyzeSource(obj: Any?, exception: Any, location: String) {
        if (obj == null) return
        if (obj !is Taint<*>) return
        AffectedVarDriver.analyzeSource(obj, exception, location)
    }
}