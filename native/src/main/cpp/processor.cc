#include "processor.hpp"

#include <classfile_constants.h>

#include <iostream>
#include "utils.hpp"
#include "plog/Log.h"

namespace exchain {

jintArray ExceptionProcessor::Process() {
    JavaVM *jvm;
    auto result = jni_->GetJavaVM(&jvm);
    if (result != 0) {
        LOG_ERROR << "Failed to get JavaVM: " << result;
    }
    jvm->AttachCurrentThread((void **)&jni_, NULL);

    jvmtiFrameInfo frames[kMaxStackDepth];
    int count;
    if (CheckJvmTIError(
            jvmti_->GetStackTrace(thread_, 0, kMaxStackDepth, frames, &count),
            "failed to get stack trace.")) {
        PLOG_INFO << "Stack count: " << count;
        for (int stack_idx = 0; stack_idx < count; stack_idx++) {
            SendStackFrameInfo(frames[stack_idx]);
            if (frames[stack_idx].method == catch_method_) {
                break;
            }
        }
    }

    jvm->DetachCurrentThread();
    return NULL;
}

bool ExceptionProcessor::ShouldIgnoreClass(std::string method_name) {
    return method_name.rfind("Ljava", 0) != std::string::npos ||
           method_name.rfind("Ljavax", 0) != std::string::npos ||
           method_name.rfind("Ljdk", 0) != std::string::npos ||
           method_name.rfind("Lch/qos/", 0) != std::string::npos ||
           method_name.rfind("Lshadow/asm", 0) != std::string::npos ||
           method_name.rfind("Lnet/bytebuddy", 0) != std::string::npos ||
           method_name.rfind("Lal/aoli/exchain/instrumentation", 0) != std::string::npos ||
           method_name.rfind("Lorg/slf4j", 0) != std::string::npos ||
           method_name.rfind("Lorg/apache/logging/log4j", 0) != std::string::npos ||
           method_name.rfind("Lsun", 0) != std::string::npos;
}


void ExceptionProcessor::SendStackFrameInfo(jvmtiFrameInfo frame) {
    jint modifiers;
    jvmti_->GetMethodModifiers(frame.method, &modifiers);
    // We ignore static methods for now.
    auto class_signature = GetClassSignature(frame.method);
    // Ignore JDK classes.
    if (ShouldIgnoreClass(class_signature)) {
        LOG_INFO << "System class ignored: " << class_signature;
        return;
    }

    auto method = GetMethodSignature(frame.method);
    PLOG_INFO << "Throw class: " << class_signature << ", method: " << method;

    auto clazz = jni_->FindClass(kRuntimeClassName);
    auto method_id = jni_->GetStaticMethodID(clazz, kMethodName, kDescriptor);

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

    jintArray result = (jintArray)jni_->CallStaticObjectMethod(
        clazz, method_id, class_jstring, method_jstring, frame.location,
        catch_current_method);

    PLOG_INFO << "JAVA returns object: " << result;
}

bool ExceptionProcessor::CheckJvmTIError(jvmtiError error, std::string msg) {
    if (error != JVMTI_ERROR_NONE) {
        char *error_name = "";
        jvmti_->GetErrorName(error, &error_name);
        PLOG_ERROR << "JVMTI: " << error << "(" << error_name
                  << "): " << msg;
        jvmti_->Deallocate((unsigned char *)error_name);
        return false;
    }
    return true;
}

std::string ExceptionProcessor::GetMethodSignature(jmethodID method) {
    char *name, *signature;
    CheckJvmTIError(jvmti_->GetMethodName(method, &name, &signature, NULL),
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
    CheckJvmTIError(jvmti_->GetClassSignature(clazz, &signature, NULL),
                    "get class signature failed.");
    std::string sig = signature;
    jvmti_->Deallocate((unsigned char *)signature);
    return sig;
}

}  // namespace exchain
