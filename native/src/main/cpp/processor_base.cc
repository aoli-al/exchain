#include "processor_base.hpp"

#include "plog/Log.h"

namespace exchain {
bool ProcessorBase::CheckJvmTIError(jvmtiError error, std::string msg) {
    if (error != JVMTI_ERROR_NONE) {
        char *error_name = "";
        jvmti_->GetErrorName(error, &error_name);
        PLOG_ERROR << "JVMTI: " << error << "(" << error_name << "): " << msg;
        jvmti_->Deallocate((unsigned char *)error_name);
        return false;
    }
    return true;
}

std::string ProcessorBase::GetMethodSignature(jmethodID method) {
    char *name, *signature;
    ProcessorBase::CheckJvmTIError(
        jvmti_->GetMethodName(method, &name, &signature, NULL),
        "get method name failed.");
    std::string sig = std::string(name) + std::string(signature);
    jvmti_->Deallocate((unsigned char *)name);
    jvmti_->Deallocate((unsigned char *)signature);
    return sig;
}

std::string ProcessorBase::GetClassSignature(jmethodID method) {
    jclass clazz;
    jvmti_->GetMethodDeclaringClass(method, &clazz);
    char *signature;
    ProcessorBase::CheckJvmTIError(
        jvmti_->GetClassSignature(clazz, &signature, NULL),
        "get class signature failed.");
    std::string sig = signature;
    jvmti_->Deallocate((unsigned char *)signature);
    return sig;
}

}  // namespace exchain
