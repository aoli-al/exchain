#include "processor.hpp"

#include <classfile_constants.h>

#include <iostream>

#include "plog/Log.h"
#include "utils.hpp"

namespace exchain {

jintArray ExceptionProcessor::Process() {

    jvmtiFrameInfo frames[kMaxStackDepth];
    int count;
    if (CheckJvmTIError(
            jvmti_->GetStackTrace(thread_, 0, kMaxStackDepth, frames, &count),
            "failed to get stack trace.")) {
        PLOG_INFO << "Stack count: " << count;
        for (int stack_idx = 0; stack_idx < count; stack_idx++) {
            ProcessStackFrameInfo(frames[stack_idx], stack_idx);
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
           method_name.rfind("Lch/qos/", 0) != std::string::npos ||
           method_name.rfind("Lshadow/asm", 0) != std::string::npos ||
           method_name.rfind("Lnet/bytebuddy", 0) != std::string::npos ||
           method_name.rfind("Lal/aoli/exchain/instrumentation", 0) !=
               std::string::npos ||
           method_name.rfind("Lorg/slf4j", 0) != std::string::npos ||
           method_name.rfind("Lorg/apache/logging/log4j", 0) !=
               std::string::npos ||
           method_name.rfind("Lsun", 0) != std::string::npos ||
           method_name.rfind("Lkotlin", 0) != std::string::npos ||
           method_name.rfind("Ledu/columbia/cs/psl/phosphor", 0) !=
               std::string::npos;
}

void ExceptionProcessor::ProcessStackFrameInfo(jvmtiFrameInfo frame,
                                               int depth) {
    jint modifiers;
    jvmti_->GetMethodModifiers(frame.method, &modifiers);
    // We ignore static methods for now.
    auto class_signature = GetClassSignature(frame.method);
    auto method = GetMethodSignature(frame.method);

    // Ignore JDK classes.
    if (ShouldIgnoreClass(class_signature)) {
        LOG_INFO << "System class ignored: " << class_signature
                 << ", method: " << method << ", location: " << frame.location;
        return;
    }

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

    jobject result = (jintArray)jni_->CallStaticObjectMethod(
        clazz, method_id, class_jstring, method_jstring, frame.location,
        catch_current_method);

    ProcessAffectedVarResults(frame, depth, result);

    PLOG_INFO << "JAVA returns object: " << result;
}

jint ExceptionProcessor::GetCorrespondingTaintObjectSlot(jvmtiFrameInfo frame, int depth, int slot, jvmtiLocalVariableEntry *table, int table_size) {
    jobject object;
    for (int i = 0; i < table_size; i++) {
        auto entry = table[i];
        if (std::string(entry.name).rfind("phosphorShadowLVFor" + std::to_string(slot)) != 0) {
            continue;
        }
        if (entry.start_location > frame.location || entry.start_location + entry.length < frame.location) {
            continue;
        }
        return entry.slot;
        // jvmti_->GetLocalObject(thread_, depth, entry.slot, &object);
        // return object;
    }
    return -1;
}


void ExceptionProcessor::ProcessAffectedVarResults(jvmtiFrameInfo frame,
                                                   int depth, jobject result) {
    auto clazz = jni_->FindClass(kAffectedVarResultsClassName);
    auto affected_vars_field_id = jni_->GetFieldID(clazz, "affectedVars", "[I");
    jintArray affected_vars =
        (jintArray)jni_->GetObjectField(result, affected_vars_field_id);

    const auto affected_vars_length = jni_->GetArrayLength(affected_vars);
    auto affected_vars_cpy = jni_->GetIntArrayElements(affected_vars, NULL);

    jvmtiLocalVariableEntry *table_ptr;
    jint table_size;
    if (!CheckJvmTIError(
            jvmti_->GetLocalVariableTable(frame.method, &table_size, &table_ptr),
            "get local variable table failed.")) {
        return;
    }

    auto runtime_clazz = jni_->FindClass(kRuntimeClassName);
    auto method_id = jni_->GetStaticMethodID(
        runtime_clazz, kTaintObjectMethodName, kTaintObjectMethodDescriptor);

    LOG_INFO << "Taint method ID: " << method_id;
    for (int i = 0; i < affected_vars_length; i++) {
        const auto slot = affected_vars_cpy[i];
        // jobject obj;
        // if (CheckJvmTIError(
        //         jvmti_->GetLocalObject(thread_, depth, slot, &obj),
        //         "get local object failed, depth: " + std::to_string(depth) +
        //             ", slot: " + std::to_string(slot))) {
        jint taint_slot = GetCorrespondingTaintObjectSlot(frame, depth, slot, table_ptr, table_size);
        if (taint_slot == -1) continue;
        jobject taint;
        jvmti_->GetLocalObject(thread_, depth, taint_slot, &taint);
        if (taint == NULL) continue;
        jobject result = jni_->CallStaticObjectMethod(
            runtime_clazz, method_id, taint, slot, thread_, depth, exception_);
        if (result != NULL) {
            jvmti_->SetLocalObject(thread_, depth, taint_slot, result);
        }

        jni_->DeleteLocalRef(taint);
    }
    jni_->ReleaseIntArrayElements(affected_vars, affected_vars_cpy, NULL);

    auto affected_fields_field_id =
        jni_->GetFieldID(clazz, "affectedFields", "[Ljava/lang/String;");
    jobjectArray affected_fields =
        (jobjectArray)jni_->GetObjectField(result, affected_fields_field_id);
    jvmti_->Deallocate((unsigned char *)table_ptr);
}

bool ExceptionProcessor::CheckJvmTIError(jvmtiError error, std::string msg) {
    if (error != JVMTI_ERROR_NONE) {
        char *error_name = "";
        jvmti_->GetErrorName(error, &error_name);
        PLOG_ERROR << "JVMTI: " << error << "(" << error_name << "): " << msg;
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

int ExceptionProcessor::ComputeExceptionId(jobject obj) {
    auto clazz = jni_->FindClass("java/lang/System");
    auto method_id =
        jni_->GetMethodID(clazz, "identityHashCode", "(Ljava/lang/Object;)I");
    return jni_->CallStaticIntMethod(clazz, method_id, obj);
}

}  // namespace exchain
