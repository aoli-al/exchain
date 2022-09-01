#include <classfile_constants.h>
#include <jvmti.h>

#include <iostream>
#include <vector>
#include <set>
#include <thread>
#include <mutex>

#include "runtime.hpp"
#include "utils.hpp"
#include "exception_processor.hpp"
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


static std::mutex processing_threads_mutex;
static std::set<jlong> processing_threads;

jlong GetThreadId(jthread thread, JNIEnv *env) {
    auto clazz = env->FindClass("java/lang/Thread");
    auto method_id = env->GetMethodID(clazz, "getId", "()J");
    return env->CallLongMethod(thread, method_id);
}


void JNICALL ExceptionCallback(jvmtiEnv *jvmti, JNIEnv *env, jthread thread,
                               jmethodID method, jlocation location,
                               jobject exception, jmethodID catch_method,
                               jlocation catch_location) {
    exchain::PrintObject(env, exception);

    if (!initialized) return;
    PLOG_INFO << "Start processing: " << thread;
    jlong exception_thread_id = GetThreadId(thread, env);
    if (processing_threads.find(exception_thread_id) != processing_threads.end()) {
        return;
    }
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
        jlong current_thread_id = GetThreadId(current_thread, env);

        processing_threads_mutex.lock();
        processing_threads.insert(current_thread_id);
        processing_threads_mutex.unlock();


        exchain::ExceptionProcessor processor(jvmti, env, thread, method,
                                              location, catch_method,
                                              catch_location, exception);
        processor.Process();

        processing_threads_mutex.lock();
        processing_threads.erase(current_thread_id);
        processing_threads_mutex.unlock();

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