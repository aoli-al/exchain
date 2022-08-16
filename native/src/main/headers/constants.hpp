namespace exchain {
const char *kRuntimeClassName =
    "al/aoli/exchain/instrumentation/runtime/ExceptionRuntime";
const char *kAffectedVarResultClassName =
    "al/aoli/exchain/instrumentation/analyzers/AffectedVarResult";
const char *kExceptionStackInfoMethodName = "onExceptionStackInfo";
const char *kExceptionStackInfoDescriptor =
    "(Ljava/lang/String;Ljava/lang/String;JJZ)Lal/aoli/exchain/"
    "instrumentation/analyzers/AffectedVarResult;";
const char *kTaintObjectMethodName = "taintObject";
const char *kUpdateTaintMethodName = "updateTaint";
const char *kTaintObjectMethodDescriptor =
    "(Ljava/lang/Object;ILjava/lang/Thread;ILjava/lang/Object;)V";
const char *kUpdateTaintMethodDescriptor =
    "(Ljava/lang/Object;ILjava/lang/Thread;ILjava/lang/Object;)Ledu/columbia/"
    "cs/psl/phosphor/runtime/Taint;";
const char *kAnalyzeSourceMethodName = "analyzeSource";
const char *kAnalyzeSourceMethodDescriptor =
    "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V";

}  // namespace exchain
