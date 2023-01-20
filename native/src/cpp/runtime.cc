#include "runtime.hpp"

#include <jvmti.h>
#include <iostream>
#include <set>

#include "plog/Log.h"

bool initialized = false;
std::set<jthread> working_threads;

JNIEXPORT void JNICALL Java_al_aoli_exchain_runtime_NativeRuntime_initializedCallback(JNIEnv *env) {
    PLOG_INFO << "Native runtime started.";
    initialized = true;
}

JNIEXPORT void JNICALL Java_al_aoli_exchain_runtime_NativeRuntime_registerWorkingThread(JNIEnv *env, jthread thread) {
    PLOG_INFO << "Worker thread: " << thread << " registered!";
    working_threads.insert(thread);
}

JNIEXPORT void JNICALL Java_al_aoli_exchain_runtime_NativeRuntime_unregisterWorkingThread(JNIEnv *env, jthread thread) {
    PLOG_INFO << "Worker thread: " << thread << " unregistered!";
    working_threads.erase(thread);
}