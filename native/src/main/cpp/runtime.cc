#include "runtime.hpp"

#include <jvmti.h>
#include <iostream>
#include <set>

bool initialized = false;
std::set<jthread> working_threads;

JNIEXPORT void JNICALL Java_al_aoli_exchain_instrumentation_runtime_NativeRuntime_initializedCallback(JNIEnv *env, jclass clazz) {
    std::cout << "INITIALIZED" << std::endl;
    initialized = true;
}

JNIEXPORT void JNICALL Java_al_aoli_exchain_instrumentation_runtime_NativeRuntime_registerWorkingThread(JNIEnv *env, jthread thread) {
    std::cout << "Thread registered!" << std::endl;
    working_threads.insert(thread);
}

JNIEXPORT void JNICALL Java_al_aoli_exchain_instrumentation_runtime_NativeRuntime_unregisterWorkingThread(JNIEnv *env, jthread thread) {
    std::cout << "Thread unregistered!" << std::endl;
    working_threads.erase(thread);
}