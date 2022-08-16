#pragma once

#include <jni.h>
#include <jvmti.h>
#include <string>

namespace exchain {

class ProcessorBase {
   protected:
    jvmtiEnv *jvmti_;
    JNIEnv *jni_;

   public:
    ProcessorBase(jvmtiEnv *jvmti, JNIEnv *jni) : jvmti_(jvmti), jni_(jni) {}
    bool CheckJvmTIError(jvmtiError error, std::string msg);
};

}  // namespace exchain
