#pragma once

#include <map>
#include <vector>

#include <jni.h>
#include <jvmti.h>

#include "constants.hpp"
#include "processor_base.hpp"

namespace exchain {

class AffectedResultProcessor: ProcessorBase {
   private:
    static jclass result_class_;
    static jclass runtime_class_;
    jvmtiFrameInfo frame_;
    int depth_;
    jobject result_;
    jboolean is_caught_by_frame_;
    jstring location_jstring_;
    std::string location_string_;
    jobject exception_;
    jthread thread_;
    jvmtiLocalVariableEntry *table_;
    jint table_size_;
    int num_of_primitives_ = 0;
    int num_of_objects_ = 0;
    int num_of_nulls_ = 0;
    int num_of_arrays_ = 0;
    std::vector<std::pair<int, std::string>> local_variable_map_;
    bool is_static_method_ = false;
    bool is_cause_identified_;

   public:
    AffectedResultProcessor(jvmtiEnv *jvmti, JNIEnv *jni, jvmtiFrameInfo frame,
                            int depth, jobject result, jboolean is_caught_by_frame,
                            std::string location_string, jobject exception, jthread thread, bool is_cause_identified)
        : ProcessorBase(jvmti, jni),
          frame_(frame),
          depth_(depth),
          result_(result),
          is_caught_by_frame_(is_caught_by_frame),
          location_string_(location_string),
          exception_(exception),
          thread_(thread),
          is_cause_identified_(is_cause_identified) {
        if (result_class_ == nullptr) {
            result_class_ = jni_->FindClass(kAffectedVarResultClassName);
            runtime_class_ = jni_->FindClass(kRuntimeClassName);
        }
        location_jstring_ =
            jni_->NewStringUTF(location_string_.c_str());
        jint modifier = 0;
        jvmti_->GetMethodModifiers(frame_.method, &modifier);
        is_static_method_ = (modifier & 0x0008) != 0;
    }

    bool Process();

   private:
    void ProcessAffectedVars();
    void ProcessSourceVars();
    void ProcessAffectedFields();
    void ProcessSourceFields();
    void ReportStats();
    jint GetCorrespondingTaintObjectSlot(int slot);
    jint GetCorrespondingObjectSlot(int slot);
    jvmtiLocalVariableEntry *GetLocalVariableEntry(int slot);
};
}  // namespace exchain