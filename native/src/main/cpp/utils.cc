#include "utils.hpp"

#include <iostream>

namespace exchain {

void PrintObject(JNIEnv *env, jobject object) {
    jclass syscls = env->FindClass("java/lang/System");
    jfieldID fid =
        env->GetStaticFieldID(syscls, "out", "Ljava/io/PrintStream;");
    jobject out = env->GetStaticObjectField(syscls, fid);
    jclass pscls = env->FindClass("java/io/PrintStream");
    jmethodID mid = env->GetMethodID(pscls, "println", "(Ljava/lang/Object;)V");
    env->CallVoidMethod(out, mid, object);
}

}  // namespace exchain
