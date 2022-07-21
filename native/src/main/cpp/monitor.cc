#include <classfile_constants.h>
#include <jvmti.h>

#include <iostream>
#include <vector>

#include "runtime.h"

const int kMaxStackDepth = 100;


std::string GetMethodInformation(jvmtiEnv *jvmti, jmethodID method) {
    char *name, *signature;
    jvmti->GetMethodName(method, &name, &signature, NULL);
    std::string info = std::string(name) + std::string(signature);
    jvmti->Deallocate((unsigned char *)name);
    jvmti->Deallocate((unsigned char *)signature);
    return info;
}

void SendStackFrameInfo(
    jvmtiEnv *jvmti, JNIEnv *env, jthread thread,
    std::vector<std::pair<jmethodID, jlocation>> exception_trace,
    jmethodID catch_method, jlocation catch_location) {
    std::cout << "method start!" << std::endl;
    auto class_name =
        "al/aoli/exchain/instrumentation/runtime/ExceptionRuntime";
    auto method_name = "onExceptionStackInfo";
    auto descriptor = "(Ljava/lang/Class;Ljava/lang/String;JJ)[I";
    auto clazz = env->FindClass(class_name);
    auto method_id = env->GetStaticMethodID(clazz, method_name, descriptor);
    for (auto trace : exception_trace) {
        jclass throw_class;
        jvmti->GetMethodDeclaringClass(trace.first, &throw_class);
        auto method = GetMethodInformation(jvmti, trace.first);
        jstring method_jstring = env->NewStringUTF(method.c_str());
        jlong catch_current_method =
            catch_method == trace.first ? catch_location : -1;
        jintArray result = (jintArray)env->CallStaticObjectMethod(
            clazz, method_id, throw_class, method_jstring, trace.second,
            catch_current_method);
    }
}

void JNICALL ExceptionCallback(jvmtiEnv *jvmti, JNIEnv *env, jthread thread,
                               jmethodID method, jlocation location,
                               jobject exception, jmethodID catch_method,
                               jlocation catch_location) {
    if (!initialized) return;
    std::vector<std::pair<jmethodID, jlocation>> exception_trace;
    jvmtiFrameInfo frames[kMaxStackDepth];
    int count;
    auto err = jvmti->GetStackTrace(thread, 0, kMaxStackDepth, frames, &count);
    if (err == JVMTI_ERROR_NONE) {
        for (int stack_idx = 0; stack_idx < count; stack_idx++) {
            auto method_info =
                GetMethodInformation(jvmti, frames[stack_idx].method);
            exception_trace.emplace_back(frames[stack_idx].method,
                                         frames[stack_idx].location);
            std::cout << "Stack: " << method_info << ":"
                      << frames[stack_idx].location << std::endl;
            if (frames[stack_idx].method == catch_method) {
                break;
            }
        }
        std::cout << "Stack end!" << std::endl;
        SendStackFrameInfo(jvmti, env, thread, std::move(exception_trace),
                           catch_method, catch_location);
    }
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    jvmtiEnv *jvmti;
    vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_0);

    jvmtiCapabilities capabilities = {0};
    capabilities.can_generate_exception_events = 1;
    capabilities.can_get_bytecodes = 1;
    capabilities.can_get_constant_pool = 1;
    jvmti->AddCapabilities(&capabilities);

    jvmtiEventCallbacks callbacks = {0};
    callbacks.Exception = ExceptionCallback;
    jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION, NULL);

    return 0;
}