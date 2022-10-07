#pragma once

#include <jvmti.h>
#include <string>
#include "processor_base.hpp"

namespace exchain {

class ExceptionProcessor: ProcessorBase {
   private:
    jmethodID throw_method_;
    jlocation throw_location_;
    jmethodID catch_method_;
    jlocation catch_location_;
    jthread thread_;
    int exception_id_;
    jobject exception_;
    jstring location_string_;
    bool is_throw_from_helper_ = false;

    static const int kMaxStackDepth = 100;

   public:
    ExceptionProcessor(jvmtiEnv *jvmti, JNIEnv *jni, jthread thread,
                       jmethodID throw_method, jlocation throw_location,
                       jmethodID catch_method, jlocation catch_location,
                       jobject exception)
        : ProcessorBase(jvmti, jni),
          throw_method_(throw_method),
          throw_location_(throw_location),
          catch_method_(catch_method),
          catch_location_(catch_location),
          thread_(thread),
          exception_(exception) {
        // exception_id_ = ComputeExceptionId(exception);
        location_string_ =
            jni_->NewStringUTF((GetClassSignature(throw_method_) + ":" +
                                GetMethodSignature(throw_method_) + ":" +
                                std::to_string(throw_location_))
                                   .c_str());
    };

   private:
    void ProcessStackFrameInfo(jvmtiFrameInfo frameInfo, int depth);
    bool CheckJvmTIError(jvmtiError error, std::string msg);
    bool ShouldIgnoreClass(std::string signature);
    bool ShouldTerminateEarly(std::string signature);
    int ComputeExceptionId(jobject obj);
    void FullPass();
    void LoggingPass();

   public:
    void Process();
};

}  // namespace exchain
