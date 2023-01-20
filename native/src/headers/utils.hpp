#pragma once

#include <jni.h>
#include <jvmti.h>

#include <string>

namespace exchain {
void PrintObject(JNIEnv *env, jobject object);

jlong GetThreadId(jthread thread, JNIEnv *env);
}  // namespace exchain