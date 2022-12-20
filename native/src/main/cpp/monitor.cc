#include <classfile_constants.h>
#include <jvmti.h>

#include <iostream>
#include <mutex>
#include <set>
#include <thread>
#include <vector>

#include "configuration.hpp"
#include "exception_processor.hpp"
#include "plog/Appenders/ColorConsoleAppender.h"
#include "plog/Formatters/FuncMessageFormatter.h"
#include "plog/Init.h"
#include "plog/Log.h"
#include "runtime.hpp"
#include "utils.hpp"

static std::mutex processing_threads_mutex;
static std::set<jlong> processing_threads;
static JavaVM *jvm = nullptr;

jlong GetThreadId(jthread thread, JNIEnv *env) {
    auto clazz = env->FindClass("java/lang/Thread");
    auto method_id = env->GetMethodID(clazz, "getId", "()J");
    return env->CallLongMethod(thread, method_id);
}

void JNICALL ExceptionCallback(jvmtiEnv *jvmti, JNIEnv *env, jthread thread,
                               jmethodID method, jlocation location,
                               jobject exception, jmethodID catch_method,
                               jlocation catch_location) {
    if (!initialized) return;
    if (thread == NULL) return;
    jlong exception_thread_id = GetThreadId(thread, env);
    processing_threads_mutex.lock();
    if (processing_threads.find(exception_thread_id) !=
        processing_threads.end()) {
        processing_threads_mutex.unlock();
        return;
    }
    processing_threads_mutex.unlock();
    std::thread new_thread([=]() {
        if (jvm == nullptr) {
            LOG_ERROR << "JavaVM is null!";
            return;
        }
        JNIEnv *jni;
        if (auto result = jvm->AttachCurrentThread((void **)&jni, NULL) != 0) {
            LOG_ERROR << "Failed to attach current thread: " << result;
            return;
        }

        auto cls = jni->FindClass("java/lang/Thread");
        auto mid = jni->GetStaticMethodID(cls, "currentThread",
                                          "()Ljava/lang/Thread;");
        jthread current_thread = jni->CallStaticObjectMethod(cls, mid);
        jlong current_thread_id = GetThreadId(current_thread, jni);
        jni->DeleteLocalRef(current_thread);

        processing_threads_mutex.lock();
        processing_threads.insert(current_thread_id);
        processing_threads_mutex.unlock();

        exchain::ExceptionProcessor processor(jvmti, jni, thread, method,
                                              location, catch_method,
                                              catch_location, exception);
        processor.Process();

        processing_threads_mutex.lock();
        processing_threads.erase(current_thread_id);
        processing_threads_mutex.unlock();

        jvm->DetachCurrentThread();
    });
    new_thread.join();
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    exchain::Configuration::GetInstance().Init(options);

    jvmtiEnv *jvmti;
    jvm = vm;
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

    static plog::ColorConsoleAppender<plog::FuncMessageFormatter>
        console_appender;
    plog::init(plog::info, &console_appender);

    PLOG_INFO << "Agent initialization done!"
              << " mode: " << exchain::Configuration::GetInstance().mode()
              << " application: "
              << exchain::Configuration::GetInstance().application();

    return 0;
}