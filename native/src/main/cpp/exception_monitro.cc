#include <classfile_constants.h>
#include <jni.h>
#include <jvmti.h>

#include <iostream>
#include <vector>

const int kMaxStackDepth = 100;

std::string GetMethodInformation(jvmtiEnv *jvmti, jmethodID method) {
    char *name, *signature;
    jvmti->GetMethodName(method, &name, &signature, NULL);
    std::string info = std::string(name) + ":" + std::string(signature);
    jvmti->Deallocate((unsigned char *)name);
    jvmti->Deallocate((unsigned char *)signature);
    return info;
}

void SendStackFrameInfo(
    JNIEnv *env, std::vector<std::pair<jmethodID, jlocation>> exception_trace) {
    auto class_name =
        "al/aoli/exception/instrumentation/runtime/ExceptionRuntime";
    auto method_name = "onExceptionStackInfo";
    auto descriptor = "([Ljava/lang/reflect/Method;[I)V";
    auto clazz = env->FindClass(class_name);
    auto method_id = env->GetStaticMethodID(clazz, method_name, descriptor);

    auto method_class= env->FindClass("java/lang/reflect/Method");

}

void JNICALL ExceptionCallback(jvmtiEnv *jvmti, JNIEnv *env, jthread thread,
                               jmethodID method, jlocation location,
                               jobject exception, jmethodID catch_method,
                               jlocation catch_location) {
    std::vector<std::pair<jmethodID, jlocation>> exception_trace;
    // auto method_info = GetMethodInformation(jvmti, method);
    // exception_trace.emplace_back(method, location);
    // std::cout << "Exception: " << method_info << ":" << location <<
    // std::endl;

    jvmtiFrameInfo frames[kMaxStackDepth];
    int count;
    auto err = jvmti->GetStackTrace(thread, 0, kMaxStackDepth, frames, &count);
    if (err == JVMTI_ERROR_NONE) {
        for (int stack_idx = 0; stack_idx < count; stack_idx++) {
            auto method_info = GetMethodInformation(jvmti, frames[stack_idx].method);
            exception_trace.emplace_back(frames[stack_idx].method, frames[stack_idx].location);
            std::cout << "Stack: " << method_info << ":" << frames[stack_idx].location
                      << std::endl;
            if (frames[stack_idx].method == catch_method) {
                std::cout << "Catch: " << method_info << ":" << catch_location
                          << std::endl;
                exception_trace.emplace_back(frames[stack_idx].method, catch_location);
                break;
            } else {
                jint local_count;
                jvmtiLocalVariableEntry *table;
                jvmti->GetLocalVariableTable(frames[stack_idx].method, &local_count, &table);
                for (int local_idx = 0; local_idx < local_count; local_idx++) {
                    auto local = table[local_idx];
                }
            }
        }
        SendStackFrameInfo(env, std::move(exception_trace));
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