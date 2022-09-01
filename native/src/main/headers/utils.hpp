#pragma once

#include <jni.h>

#include <string>

namespace exchain {
void PrintObject(JNIEnv *env, jobject object);
}  // namespace exchain