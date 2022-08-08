#pragma once

#include <jvmti.h>

#include <string>

namespace exchain {

class ExceptionProcessor {
   private:
    jvmtiEnv *jvmti_;
    JNIEnv *jni_;
    jmethodID throw_method_;
    jlocation throw_location_;
    jmethodID catch_method_;
    jlocation catch_location_;
    jthread thread_;
    int exception_id_;
    jobject exception_;

    static const int kMaxStackDepth = 100;
    const char *kRuntimeClassName =
        "al/aoli/exchain/instrumentation/runtime/ExceptionRuntime";
    const char *kAffectedVarResultClassName =
        "al/aoli/exchain/instrumentation/analyzers/AffectedVarResult";
    const char *kExceptionStackInfoMethodName = "onExceptionStackInfo";
    const char *kExceptionStackInfoDescriptor =
        "(Ljava/lang/String;Ljava/lang/String;JJZ)Lal/aoli/exchain/"
        "instrumentation/analyzers/AffectedVarResult;";
    const char *kTaintObjectMethodName = "taintObject";
    const char *kTaintObjectMethodDescriptor = "(Ljava/lang/Object;ILjava/lang/Thread;ILjava/lang/Object;)Ledu/columbia/cs/psl/phosphor/runtime/Taint;";
    const char *kAnalyzeSourceMethodName = "analyzeSource";
    const char *kAnalyzeSourceMethodDescriptor = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V";

   public:
    ExceptionProcessor(jvmtiEnv *jvmti, JNIEnv *jni, jthread thread,
                       jmethodID throw_method, jlocation throw_location,
                       jmethodID catch_method, jlocation catch_location,
                       jobject exception)
        : jvmti_(jvmti),
          jni_(jni),
          throw_method_(throw_method),
          throw_location_(throw_location),
          catch_method_(catch_method),
          catch_location_(catch_location),
          thread_(thread),
          exception_(exception){
              // exception_id_ = ComputeExceptionId(exception);
          };

   private:
    void ProcessStackFrameInfo(jvmtiFrameInfo frameInfo, int depth);
    std::string GetMethodSignature(jmethodID method);
    std::string GetClassSignature(jmethodID clazz);
    bool CheckJvmTIError(jvmtiError error, std::string msg);
    bool ShouldIgnoreClass(std::string signature);
    bool ShouldTerminateEarly(std::string signature);
    void ProcessAffectedVarResult(jvmtiFrameInfo frame, int depth,
                                   jobject result, jboolean is_throw_insn, jstring location_string);
    jint GetCorrespondingTaintObjectSlot(jvmtiFrameInfo frame, int depth, int slot, jvmtiLocalVariableEntry *table, int table_size);
    int ComputeExceptionId(jobject obj);

   public:
    void Process();
};

}  // namespace exchain
