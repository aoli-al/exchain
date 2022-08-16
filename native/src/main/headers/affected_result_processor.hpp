#pragma once
#include <jni.h>
#include <jvmti.h>

#include "constants.hpp"
#include "processor_base.hpp"

namespace exchain {

class AffectedResultProcessor: ProcessorBase {
   private:
    jclass result_class_;
    jclass runtime_class_;
    jvmtiFrameInfo frame_;
    int depth_;
    jobject result_;
    jboolean is_caught_by_frame_;
    jstring location_;
    jobject exception_;
    jthread thread_;
    jvmtiLocalVariableEntry *table_;
    jint table_size_;

   public:
    AffectedResultProcessor(jvmtiEnv *jvmti, JNIEnv *jni, jvmtiFrameInfo frame,
                            int depth, jobject result, jboolean is_caught_by_frame,
                            jstring location)
        : ProcessorBase(jvmti, jni),
          frame_(frame),
          depth_(depth),
          result_(result),
          is_caught_by_frame_(is_caught_by_frame),
          location_(location) {
        result_class_ = jni_->FindClass(kAffectedVarResultClassName);
        runtime_class_ = jni_->FindClass(kRuntimeClassName);
    }

    void Process();

   private:
    void ProcessAffectedVars();
    void ProcessSourceVars();
    void ProcessAffectedParams();
    void ProcessAffectedFields();
    jint GetCorrespondingTaintObjectSlot(jvmtiFrameInfo frame, int depth, int slot, jvmtiLocalVariableEntry *table, int table_size);
    std::string GetLocalObjectSignature(jvmtiFrameInfo frame, int depth, int slot, jvmtiLocalVariableEntry *table, int table_size);
};
}  // namespace exchain