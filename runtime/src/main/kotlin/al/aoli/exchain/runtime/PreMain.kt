package al.aoli.exchain.runtime

import al.aoli.exchain.runtime.NativeRuntime
import al.aoli.exchain.runtime.analyzers.AffectedVarDriver
import al.aoli.exchain.runtime.analyzers.AffectedVarInterpreter
import al.aoli.exchain.runtime.objects.Type
import java.lang.instrument.Instrumentation

fun premain(arguments: String?, instrumentation: Instrumentation) {
    NativeRuntime.initializedCallback()
    if (arguments != null) {
        val (type, classPath) = arguments!!.split(":")
        AffectedVarDriver.instrumentedClassPath = classPath
        if (type == "static") {
            AffectedVarDriver.type = Type.Static
        }
    }
}