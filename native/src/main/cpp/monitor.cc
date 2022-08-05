#include <classfile_constants.h>
#include <jvmti.h>

#include <iostream>
#include <vector>
#include <set>
#include <thread>

#include "runtime.hpp"
#include "processor.hpp"
#include "plog/Init.h"
#include "plog/Log.h"
#include "plog/Appenders/ColorConsoleAppender.h"
#include "plog/Formatters/FuncMessageFormatter.h"



// void GetLocalVariables(jvmtiEnv *jvmti, jlocation current_location, jmethodID method, std::string method_name) {
//     jvmtiLocalVariableEntry *table_ptr;
//     jint entry_count;
//     if (CheckJvmTIError(
//             jvmti,
//             jvmti->GetLocalVariableTable(method, &entry_count, &table_ptr),
//             "get local variable table failed.")) {
//         for (int i = 0; i < entry_count; i++) {
//             if (table_ptr[i].start_location >= current_location) {
//                 continue;
//             }
//         }
//     }
//     jvmti->Deallocate((unsigned char *)table_ptr);
// }


static std::set<jthread> processing_threads;


void JNICALL ExceptionCallback(jvmtiEnv *jvmti, JNIEnv *env, jthread thread,
                               jmethodID method, jlocation location,
                               jobject exception, jmethodID catch_method,
                               jlocation catch_location) {
    PLOG_INFO << "Start processing: " << thread;
    for (auto stored: processing_threads) {
        if (env->IsSameObject(stored, thread)) {
            return;
        }
    }
    // We need to create a new thread to avoid internal exceptions.
    std::thread new_thread([=]() {
        JavaVM *jvm;
        auto result = env->GetJavaVM(&jvm);
        if (result != 0) {
            LOG_ERROR << "Failed to get JavaVM: " << result;
        }
        jvm->AttachCurrentThread((void **)&env, NULL);

        auto cls = env->FindClass("java/lang/Thread");
        auto mid = env->GetStaticMethodID(cls, "currentThread", "()Ljava/lang/Thread;");
        jthread current_thread = env->CallStaticObjectMethod(cls, mid);
        processing_threads.insert(current_thread);
        exchain::ExceptionProcessor processor(jvmti, env, thread, method,
                                              location, catch_method,
                                              catch_location, exception);
        processor.Process();
        processing_threads.erase(current_thread);
        jvm->DetachCurrentThread();
    });
    new_thread.join();
    PLOG_INFO << "Finish processing: " << thread;
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    jvmtiEnv *jvmti;
    vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_0);

    jvmtiCapabilities capabilities = {0};
    capabilities.can_generate_exception_events = 1;
    capabilities.can_get_bytecodes = 1;
    capabilities.can_get_constant_pool = 1;
    capabilities.can_access_local_variables = 1;
    jvmti->AddCapabilities(&capabilities);

    jvmtiEventCallbacks callbacks = {0};
    callbacks.Exception = ExceptionCallback;
    jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION, NULL);

    static plog::ColorConsoleAppender<plog::FuncMessageFormatter> console_appender;
    plog::init(plog::info, &console_appender);

    return 0;
}