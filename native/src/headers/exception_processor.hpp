#pragma once

#include <jvmti.h>

#include <string>

#include "plog/Log.h"
#include "processor_base.hpp"
#include "constants.hpp"

namespace exchain {

class ExceptionProcessor : ProcessorBase {
   private:
    jmethodID throw_method_;
    jlocation throw_location_;
    jmethodID catch_method_;
    jlocation catch_location_;
    jthread thread_;
    int exception_id_;
    jobject exception_;
    std::string location_string_;
    bool is_cause_identified_ = false;

    static jclass runtime_class_;

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
        if (runtime_class_ == nullptr) {
            runtime_class_ = jni_->FindClass(kRuntimeClassName);
            if (runtime_class_ == nullptr) {
                PLOG_ERROR << "Failed to find Runtime class";
                return;
            }
        }

        location_string_ = GetClassSignature(throw_method_) + ":" +
                           GetMethodSignature(throw_method_) + ":" +
                           std::to_string(throw_location_);
    };

   private:
    void ProcessStackFrameInfo(jvmtiFrameInfo frameInfo, int depth);
    bool CheckJvmTIError(jvmtiError error, std::string msg);
    bool ShouldIgnoreClass(std::string signature);
    bool ShouldTerminateEarly(std::string signature, std::string method_name);
    bool ContainsApplicationCode(std::string signature);
    int ComputeExceptionId(jobject obj);
    void FullPass();
    void LoggingPass();

   public:
    void Process();
};

}  // namespace exchain
