package al.aoli.exchain.runtime

import al.aoli.exchain.runtime.NativeRuntime
import al.aoli.exchain.runtime.analyzers.AffectedVarDriver
import java.lang.instrument.Instrumentation

fun premain(arguments: String?, instrumentation: Instrumentation) {
    NativeRuntime.initializedCallback()
    AffectedVarDriver.instrumentedClassPath = arguments
}