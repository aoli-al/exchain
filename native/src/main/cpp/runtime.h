#pragma once

#include <jni.h>

static bool initialized = false;

#ifdef __cplusplus
extern "C" {
    JNIEXPORT void JNICALL Java_al_aoli_exchain_instrumentation_runtime_NativeRuntime_initializedCallback(JNIEnv *env, jclass clazz);
}
#endif