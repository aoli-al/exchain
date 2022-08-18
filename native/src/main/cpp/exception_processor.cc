#include "exception_processor.hpp"

#include <classfile_constants.h>

#include <iostream>
#include <string_view>

#include "plog/Log.h"
#include "utils.hpp"
#include "constants.hpp"
#include "affected_result_processor.hpp"

namespace exchain {

void ExceptionProcessor::Process() {
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

void ExceptionProcessor::ProcessStackFrameInfo(jvmtiFrameInfo frame, int depth) {
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
    jboolean is_throw_insn = depth == 0;

    jobject result = (jintArray)jni_->CallStaticObjectMethod(
        clazz, method_id, class_jstring, method_jstring, frame.location,
        catch_current_method, is_throw_insn);

    if (result == NULL) {
        return;
    }

    // jstring location_string = jni_->NewStringUTF(
    //     (class_signature + ":" + method + ":" + std::to_string(frame.location))
    //         .c_str());

    AffectedResultProcessor processor(jvmti_, jni_, frame, depth, result,
                                      frame.method == catch_method_,
                                      location_string_, exception_, thread_);
    processor.Process();
}

std::string ExceptionProcessor::GetMethodSignature(jmethodID method) {
    char *name, *signature;
    ProcessorBase::CheckJvmTIError(jvmti_->GetMethodName(method, &name, &signature, NULL),
                    "get method name failed.");
    std::string sig = std::string(name) + std::string(signature);
    jvmti_->Deallocate((unsigned char *)name);
    jvmti_->Deallocate((unsigned char *)signature);
    return sig;
}

std::string ExceptionProcessor::GetClassSignature(jmethodID method) {
    jclass clazz;
    jvmti_->GetMethodDeclaringClass(method, &clazz);
    char *signature;
    ProcessorBase::CheckJvmTIError(jvmti_->GetClassSignature(clazz, &signature, NULL),
                    "get class signature failed.");
    std::string sig = signature;
    jvmti_->Deallocate((unsigned char *)signature);
    return sig;
}

int ExceptionProcessor::ComputeExceptionId(jobject obj) {
    auto clazz = jni_->FindClass("java/lang/System");
    auto method_id =
        jni_->GetMethodID(clazz, "identityHashCode", "(Ljava/lang/Object;)I");
    return jni_->CallStaticIntMethod(clazz, method_id, obj);
}

}  // namespace exchain
