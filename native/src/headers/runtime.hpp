#pragma once

#include <jni.h>
#include <jvmti.h>
#include <set>
#include "thread_pool.hpp"


extern bool initialized;
extern std::set<jthread> working_threads;
extern exchain::ThreadPool *thread_pool;

#ifdef __cplusplus
extern "C" {
    JNIEXPORT void JNICALL Java_al_aoli_exchain_runtime_NativeRuntime_initializedCallback(JNIEnv *env);
    JNIEXPORT void JNICALL Java_al_aoli_exchain_runtime_NativeRuntime_shutdown(JNIEnv *env);
    JNIEXPORT void JNICALL Java_al_aoli_exchain_runtime_NativeRuntime_registerWorkingThread(JNIEnv *env, jthread clazz);
    JNIEXPORT void JNICALL Java_al_aoli_exchain_runtime_NativeRuntime_unregisterWorkingThread(JNIEnv *env, jthread clazz);
}
#endif