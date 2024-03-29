#include "exception_processor.hpp"

#include <classfile_constants.h>

#include <iostream>
#include <string_view>

#include "affected_result_processor.hpp"
#include "configuration.hpp"
#include "constants.hpp"
#include "plog/Log.h"
#include "utils.hpp"

namespace exchain {

jclass ExceptionProcessor::runtime_class_ = nullptr;

void ExceptionProcessor::FullPass() {
    jvmtiFrameInfo frames[kMaxStackDepth];
    int count;
    if (ProcessorBase::CheckJvmTIError(
            jvmti_->GetStackTrace(thread_, 0, kMaxStackDepth, frames, &count),
            "failed to get stack trace.")) {
        // PLOG_ERROR << "Exception: " << throw_method_ << " " << catch_method_
        //            << " Stack count: " << count;
        bool is_application_code_visited = false;
        for (int stack_idx = 0; stack_idx < count; stack_idx++) {
            auto method_signature = GetMethodSignature(frames[stack_idx].method);
            if (method_signature.find("lambda$") != std::string::npos) {
                continue;
            }
            auto class_signature = GetClassSignature(frames[stack_idx].method);
            if (ShouldIgnoreClass(class_signature)) {
                break;
            }
            for (auto &application: Configuration::GetInstance().application()) {
                if (class_signature.starts_with(application)) {
                    is_application_code_visited = true;
                    break;
                }
            }
            if (frames[stack_idx].method == catch_method_) {
                break;
            }
        }

        if (!is_application_code_visited) {
            return;
        }

        ProcessException();

        for (int stack_idx = 0; stack_idx < count; stack_idx++) {
            jint flags;
            jvmti_->GetMethodModifiers(frames[stack_idx].method, &flags);
            if (flags & JVM_ACC_NATIVE) {
                continue;
            }
            ProcessStackFrameInfo(frames[stack_idx], stack_idx);
            if (frames[stack_idx].method == catch_method_) {
                break;
            }
        }
    }
    is_cause_identified_ = false;
    return;
}

void ExceptionProcessor::ProcessException() {
    static auto method_id = jni_->GetStaticMethodID(
        runtime_class_, kProcessException, kProcessExceptionDescriptor);
    jni_->CallStaticVoidMethod(runtime_class_, method_id, exception_);
}

void ExceptionProcessor::LoggingPass() {
    PLOG_INFO << "Logging pass started!!!";
    auto clazz = jni_->FindClass(kRuntimeClassName);
    auto method_id =
        jni_->GetStaticMethodID(clazz, kOnExceptionCaughtMethodName,
                                kOnExceptionCaughtMethodDescriptor);
    jni_->CallStaticVoidMethod(clazz, method_id, exception_);
}

void ExceptionProcessor::Process() {
    switch (Configuration::GetInstance().mode()) {
        case LOGGING:
            LoggingPass();
            break;
        case EXCHAIN:
            FullPass();
            break;
        default:
            break;
    }
}

bool ExceptionProcessor::ShouldIgnoreClass(std::string class_name) {
    // return method_name.starts_with("Ljava") ||
    //        method_name.starts_with("Ljavax") ||
    //        method_name.starts_with("Ljdk") ||
    //        method_name.starts_with("Lch/qos/") ||
    //        method_name.starts_with("Lshadow/asm") ||
    //        method_name.starts_with("Lnet/bytebuddy") ||
    //        method_name.starts_with("Lorg/apache/catalina/loader") ||
    //        method_name.starts_with("Lal/aoli/exchain/instrumentation") ||
    //        method_name.starts_with("Lorg/slf4j") ||
    //        method_name.starts_with("Lorg/apache/logging/log4j") ||
    //        method_name.starts_with("Lsun") ||
    //        method_name.starts_with("Lcom/sun") ||
    //        method_name.starts_with("Lkotlin") ||
    return class_name.starts_with("Lorg/apache/zookeeper") ||
           class_name.starts_with("Ljava/lang/Class") ||
           class_name.starts_with("Lorg/apache/solr/parser");
}

bool ExceptionProcessor::ShouldTerminateEarly(std::string class_name,
                                              std::string method_name) {
    return method_name.starts_with(
               "findClass(Ljava/lang/String;)Ljava/lang/Class;") ||
           method_name.starts_with("loadClass(Ljava/lang/String") ||
           method_name.starts_with(
               "getField(Ljava/lang/String;)Ljava/lang/reflect/Field;") ||
           class_name.starts_with("Lmu/") ||
           class_name.starts_with("Lkotlin/") ||
           class_name.starts_with("Lal/aoli/exchain/instrumentation") ||
           class_name.starts_with("Lal/aoli/exchain/phosphor") ||
           class_name.starts_with("Ledu/columbia/cs/psl/") ||
           class_name.starts_with(
               "Lorg/springframework/boot/loader/jar/JarURLConnection") ||
           class_name.starts_with("Lorg/springframework/util/ClassUtils") ||
           class_name.starts_with("Lorg/springframework/asm/ClassReader") ||
           class_name.starts_with(
               "Lorg/springframework/core/io/ClassPathResource") ||
           class_name.starts_with("Ljavax/naming/spi/NamingManager") ||
           class_name.starts_with("Ljava/lang/Class");

    // return false;
    // return method_name.starts_with("Lorg/springframework/boot") ||
    //        method_name.starts_with("Lorg/springframework/util/ClassUtils") ||
    //        method_name.starts_with("Lorg.springframework.asm.ClassReader") ||
    //        method_name.starts_with(
    //            "Lorg/springframework/cglib/core/ClassNameReader") ||
    //        method_name.starts_with(
    //            "Lorg/springframework/core/io/ClassPathResource");
}

void ExceptionProcessor::ProcessStackFrameInfo(jvmtiFrameInfo frame,
                                               int depth) {
    auto class_signature = GetClassSignature(frame.method);
    auto method = GetMethodSignature(frame.method);

    PLOG_INFO << "Depth: " << depth << ", Throw class: " << class_signature
              << ", method: " << method << ", location: " << frame.location;

    static auto method_id =
        jni_->GetStaticMethodID(runtime_class_, kExceptionStackInfoMethodName,
                                kExceptionStackInfoDescriptor);

    PLOG_INFO << "location: " << location_string_;

    jstring method_jstring = jni_->NewStringUTF(method.c_str());
    jstring class_jstring = jni_->NewStringUTF(class_signature.c_str());
    jlong catch_current_method =
        catch_method_ == frame.method ? catch_location_ : -1;
    jboolean is_throw_insn = depth == 0;
    jobject result = (jintArray)jni_->CallStaticObjectMethod(
        runtime_class_, method_id, exception_, class_jstring, method_jstring,
        frame.location, catch_current_method, is_throw_insn);
    jni_->DeleteLocalRef(method_jstring);
    jni_->DeleteLocalRef(class_jstring);

    // When the result is null, dynamic tracing is not enabled.
    if (result == NULL) {
        return;
    }
    AffectedResultProcessor processor(
        jvmti_, jni_, frame, depth, result, frame.method == catch_method_,
        location_string_, exception_, thread_, is_cause_identified_);
    is_cause_identified_ |= processor.Process();
}

int ExceptionProcessor::ComputeExceptionId(jobject obj) {
    auto clazz = jni_->FindClass("java/lang/System");
    auto method_id =
        jni_->GetMethodID(clazz, "identityHashCode", "(Ljava/lang/Object;)I");
    return jni_->CallStaticIntMethod(clazz, method_id, obj);
}

}  // namespace exchain
