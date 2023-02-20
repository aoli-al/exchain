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
#include "thread_pool.hpp"
#include "utils.hpp"

static JavaVM *jvm = nullptr;
exchain::ThreadPool *thread_pool = nullptr;

void JNICALL ExceptionCallback(jvmtiEnv *jvmti, JNIEnv *env, jthread thread,
                               jmethodID method, jlocation location,
                               jobject exception, jmethodID catch_method,
                               jlocation catch_location) {
    if (thread_pool == nullptr) {
        thread_pool = new exchain::ThreadPool(10, jvm);
    }
    if (!initialized) return;
    if (thread == NULL) return;
    jlong exception_thread_id = exchain::GetThreadId(thread, env);
    if (thread_pool->IsProcessingThread(exception_thread_id)) {
        return;
    }
    auto task = thread_pool->Submit([=](JNIEnv *jni) {
        exchain::ExceptionProcessor processor(jvmti, jni, thread, method,
                                              location, catch_method,
                                              catch_location, exception);
        processor.Process();
    });
    task.wait();
}

JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm) {
    PLOG_INFO << "Agent unloaded";
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
    plog::init(plog::error, &console_appender);

    PLOG_INFO << "Agent initialization done!"
              << " mode: " << exchain::Configuration::GetInstance().mode()
              << " application: "
              << exchain::Configuration::GetInstance().application();

    return 0;
}