#include "exception_processor.hpp"

#include <classfile_constants.h>

#include <iostream>
#include <string_view>

#include "affected_result_processor.hpp"
#include "constants.hpp"
#include "configuration.hpp"
#include "plog/Log.h"
#include "utils.hpp"

namespace exchain {

void ExceptionProcessor::FullPass() {
    jvmtiFrameInfo frames[kMaxStackDepth];
    int count;
    if (ProcessorBase::CheckJvmTIError(
            jvmti_->GetStackTrace(thread_, 0, kMaxStackDepth, frames, &count),
            "failed to get stack trace.")) {
        PLOG_INFO << "Stack count: " << count;
        for (int stack_idx = 0; stack_idx < count; stack_idx++) {
            auto class_signature = GetClassSignature(frames[stack_idx].method);
            if (ShouldIgnoreClass(class_signature)) {
                if (frames[stack_idx].method == catch_method_) {
                    break;
                }
                continue;
            }
            if (ShouldTerminateEarly(class_signature)) {
                return;
            }
            ProcessStackFrameInfo(frames[stack_idx], stack_idx);
            if (frames[stack_idx].method == catch_method_) {
                break;
            }
        }
    }
    return;
}

void ExceptionProcessor::LoggingPass() {
    PLOG_INFO << "Logging pass started!!!";
    auto clazz = jni_->FindClass(kRuntimeClassName);
    auto method_id = jni_->GetStaticMethodID(
        clazz, kOnExceptionCaughtMethodName, kOnExceptionCaughtMethodDescriptor);
    PLOG_INFO << "Class found: " << clazz;
    PLOG_INFO << "Method found: " << method_id;
    jni_->CallStaticVoidMethod(clazz, method_id, exception_);
}

void ExceptionProcessor::Process() {
    switch (Configuration::GetInstance().mode())
    {
        case LOGGING:
            LoggingPass();
            break;
        case TAINT:
        case STAT:
            FullPass();
            break;
        default:
            break;
    }
}

bool ExceptionProcessor::ShouldIgnoreClass(std::string method_name) {
    return method_name.starts_with("Ljava") ||
           method_name.starts_with("Ljavax") ||
           method_name.starts_with("Ljdk") ||
           method_name.starts_with("Lch/qos/") ||
           method_name.starts_with("Lshadow/asm") ||
           method_name.starts_with("Lnet/bytebuddy") ||
           method_name.starts_with("Lorg/apache/catalina/loader") ||
           method_name.starts_with("Lal/aoli/exchain/instrumentation") ||
           method_name.starts_with("Lorg/slf4j") ||
           method_name.starts_with("Lorg/apache/logging/log4j") ||
           method_name.starts_with("Lsun") ||
           method_name.starts_with("Lcom/sun") ||
           method_name.starts_with("Lkotlin") ||
           method_name.starts_with("Lal/aoli/exchain/instrumentation") ||
           method_name.starts_with("Lal/aoli/exchain/phosphor") ||
           method_name.starts_with("Ledu/columbia/cs/psl/");
}

bool ExceptionProcessor::ShouldTerminateEarly(std::string method_name) {
    return method_name.starts_with("Lorg/springframework/boot") ||
           method_name.starts_with("Lorg/springframework/util/ClassUtils") ||
           method_name.starts_with("Lorg.springframework.asm.ClassReader") ||
           method_name.starts_with(
               "Lorg/springframework/cglib/core/ClassNameReader") ||
           method_name.starts_with(
               "Lorg/springframework/core/io/ClassPathResource");
}

void ExceptionProcessor::ProcessStackFrameInfo(jvmtiFrameInfo frame,
                                               int depth) {
    auto class_signature = GetClassSignature(frame.method);
    auto method = GetMethodSignature(frame.method);

    PLOG_INFO << "Throw class: " << class_signature << ", method: " << method
              << ", location: " << frame.location;

    auto clazz = jni_->FindClass(kRuntimeClassName);
    auto method_id = jni_->GetStaticMethodID(
        clazz, kExceptionStackInfoMethodName, kExceptionStackInfoDescriptor);

    if (method_id == NULL || clazz == NULL) {
        PLOG_WARNING << "Cannot load JAVA method, abort.";
        return;
    }

    PLOG_INFO << "Calling JAVA method at address: " << method_id
              << ", class: " << clazz;

    jstring method_jstring = jni_->NewStringUTF(method.c_str());
    jstring class_jstring = jni_->NewStringUTF(class_signature.c_str());
    jlong catch_current_method =
        catch_method_ == frame.method ? catch_location_ : -1;
    jboolean is_throw_insn = (depth == 0) || (depth == 1 && is_throw_from_helper_);
    if (depth == 0) {
        LOG_INFO << "Method: " << method;
        if (kExceptionHelpers.find(method) != kExceptionHelpers.end()) {
            is_throw_from_helper_ = true;
        }
    }
    jobject result = (jintArray)jni_->CallStaticObjectMethod(
        clazz, method_id, exception_,
        class_jstring, method_jstring, frame.location,
        catch_current_method, is_throw_insn);
    jni_->DeleteLocalRef(method_jstring);
    jni_->DeleteLocalRef(class_jstring);
    if (result == NULL) {
        return;
    }
    AffectedResultProcessor processor(jvmti_, jni_, frame, depth, result,
                                      frame.method == catch_method_,
                                      location_string_, exception_, thread_);
    processor.Process();
}

int ExceptionProcessor::ComputeExceptionId(jobject obj) {
    auto clazz = jni_->FindClass("java/lang/System");
    auto method_id =
        jni_->GetMethodID(clazz, "identityHashCode", "(Ljava/lang/Object;)I");
    return jni_->CallStaticIntMethod(clazz, method_id, obj);
}

}  // namespace exchain
