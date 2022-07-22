#include "processor.hpp"

#include <classfile_constants.h>

#include <iostream>
#include "utils.hpp"

namespace exchain {

jintArray ExceptionProcessor::Process() {
    jvmtiFrameInfo frames[kMaxStackDepth];
    int count;
    if (CheckJvmTIError(
            jvmti_->GetStackTrace(thread_, 0, kMaxStackDepth, frames, &count),
            "failed to get stack trace.")) {
        for (int stack_idx = 0; stack_idx < count; stack_idx++) {
            auto method_info =
                GetMethodSignature(frames[stack_idx].method);
            std::cout << "Stack: " << method_info << ":"
                      << frames[stack_idx].location << std::endl;
            SendStackFrameInfo(frames[stack_idx]);
            if (frames[stack_idx].method == catch_method_) {
                break;
            }
        }
    }
    return NULL;
}

bool ExceptionProcessor::ShouldIgnoreClass(std::string method_name) {
    return method_name.rfind("Ljava", 0) != std::string::npos ||
           method_name.rfind("Ljavax", 0) != std::string::npos ||
           method_name.rfind("Ljdk", 0) != std::string::npos ||
           method_name.rfind("Lshadow/asm", 0) != std::string::npos ||
           method_name.rfind("Lnet/bytebuddy", 0) != std::string::npos ||
           method_name.rfind("Lal/aoli/exchain/instrumentation", 0) != std::string::npos ||
           method_name.rfind("Lsun", 0) != std::string::npos;
}


void ExceptionProcessor::SendStackFrameInfo(jvmtiFrameInfo frame) {
    jint modifiers;
    jvmti_->GetMethodModifiers(frame.method, &modifiers);
    // We ignore static methods for now.
    if (modifiers & JVM_ACC_STATIC) return;

    jclass throw_class;
    jvmti_->GetMethodDeclaringClass(frame.method, &throw_class);

    auto class_signature = GetClassSignature(throw_class);

    // Ignore JDK classes.
    if (ShouldIgnoreClass(class_signature)) return;

    std::cout << "Start processing: " << class_signature << std::endl;

    auto method = GetMethodSignature(frame.method);
    auto clazz = jni_->FindClass(kRuntimeClassName);
    auto method_id = jni_->GetStaticMethodID(clazz, kMethodName, kDescriptor);

    std::cout << "Calling method at address: " << method_id
              << ", class: " << clazz << std::endl;

    jstring method_jstring = jni_->NewStringUTF(method.c_str());
    jlong catch_current_method =
        catch_method_ == frame.method ? catch_location_ : -1;

    jintArray result = (jintArray)jni_->CallStaticObjectMethod(
        clazz, method_id, throw_class, method_jstring, frame.location,
        catch_current_method);
}

bool ExceptionProcessor::CheckJvmTIError(jvmtiError error, std::string msg) {
    if (error != JVMTI_ERROR_NONE) {
        char *error_name = "";
        jvmti_->GetErrorName(error, &error_name);
        std::cout << "ERROR: JVMTI: " << error << "(" << error_name
                  << "): " << msg << std::endl;
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

std::string ExceptionProcessor::GetClassSignature(jclass clazz) {
    char *signature;
    CheckJvmTIError(jvmti_->GetClassSignature(clazz, &signature, NULL),
                    "get class signature failed.");
    std::string sig = signature;
    jvmti_->Deallocate((unsigned char *)signature);
    return sig;
}

}  // namespace exchain
