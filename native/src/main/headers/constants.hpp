#pragma once

namespace exchain {
const static char *kRuntimeClassName =
    "al/aoli/exchain/runtime/ExceptionRuntime";
const static char *kAffectedVarResultClassName =
    "al/aoli/exchain/runtime/analyzers/AffectedVarResult";
const static char *kExceptionStackInfoMethodName = "onExceptionStackInfo";
const static char *kExceptionStackInfoDescriptor =
    "(Ljava/lang/Throwable;Ljava/lang/String;Ljava/lang/String;JJZ)Lal/aoli/"
    "exchain/"
    "runtime/analyzers/AffectedVarResult;";
const static char *kTaintObjectMethodName = "taintObject";
const static char *kUpdateTaintMethodName = "updateTaint";
const static char *kTaintObjectMethodDescriptor =
    "(Ljava/lang/Object;ILjava/lang/Thread;ILjava/lang/Object;)V";
const static char *kUpdateTaintMethodDescriptor =
    "(Ljava/lang/Object;ILjava/lang/Thread;ILjava/lang/Object;)Ledu/columbia/"
    "cs/psl/phosphor/runtime/Taint;";
const static char *kTaintFieldsMethodName = "taintFields";
const static char *kTaintFieldsMethodDescriptor =
    "(Ljava/lang/Object;Lal/aoli/exchain/"
    "runtime/analyzers/AffectedVarResult;Ljava/lang/"
    "Object;)V";
const static char *kAnalyzeSourceVarsMethodName = "analyzeSourceVars";
const static char *kAnalyzeSourceVarsMethodDescriptor =
    "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V";
const static char *kAnalyzeSourceFieldsMethodName = "analyzeSourceFields";
const static char *kAnalyzeSourceFieldsMethodDescriptor =
    "(Ljava/lang/Object;Lal/aoli/exchain/runtime/analyzers/"
    "AffectedVarResult;Ljava/lang/Object;Ljava/lang/String;)V";
const static char *kOnExceptionCaughtMethodName = "onExceptionCaught";
const static char *kOnExceptionCaughtMethodDescriptor =
    "(Ljava/lang/Throwable;)V";
const static char *kOnExceptionStatsMethodName = "onExceptionStats";
const static char *kOnExceptionStatsMethodDescriptor =
    "(Ljava/lang/Throwable;Lal/aoli/exchain/runtime/analyzers/"
    "AffectedVarResult;IIIIZ)V";

}  // namespace exchain
