#include "runtime.h"

#include <iostream>

JNIEXPORT void JNICALL Java_al_aoli_exchain_instrumentation_runtime_NativeRuntime_initializedCallback(JNIEnv *env, jclass clazz) {
    std::cout << "INITIALIZED" << std::endl;
    initialized = true;
}