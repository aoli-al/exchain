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

    static const int kMaxStackDepth = 100;
    const char *kRuntimeClassName =
        "al/aoli/exchain/instrumentation/runtime/ExceptionRuntime";
    const char *kMethodName = "onExceptionStackInfo";
    const char *kDescriptor = "(Ljava/lang/Class;Ljava/lang/String;JJ)[I";

   public:
    ExceptionProcessor(jvmtiEnv *jvmti, JNIEnv *jni, jthread thread,
                       jmethodID throw_method, jlocation throw_location,
                       jmethodID catch_method, jlocation catch_location)
        : jvmti_(jvmti),
          jni_(jni),
          throw_method_(throw_method),
          throw_location_(throw_location),
          catch_method_(catch_method),
          catch_location_(catch_location),
          thread_(thread){};

   private:
    void SendStackFrameInfo(jvmtiFrameInfo frameInfo);
    std::string GetMethodSignature(jmethodID method);
    std::string GetClassSignature(jclass clazz);
    bool CheckJvmTIError(jvmtiError error, std::string msg);
    bool ShouldIgnoreClass(std::string signature);

   public:
    jintArray Process();
};

}  // namespace exchain
