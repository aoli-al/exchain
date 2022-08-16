#include "affected_result_processor.hpp"
#include "plog/Log.h"

namespace exchain {

void AffectedResultProcessor::Process() {
    if (!CheckJvmTIError(
            jvmti_->GetLocalVariableTable(frame_.method, &table_size_, &table_),
            "get local variable table failed.")) {
        return;
    }

    ProcessAffectedVars();
    ProcessAffectedFields();
    ProcessAffectedParams();

    jvmti_->Deallocate((unsigned char *)table_);
}

void AffectedResultProcessor::ProcessSourceVars() {
    PLOG_INFO << "Start analyzing exception sources!";
    auto analyze_source_method_id =
        jni_->GetStaticMethodID(runtime_class_, kAnalyzeSourceMethodName,
                                kAnalyzeSourceMethodDescriptor);
    auto source_vars_field_id = jni_->GetFieldID(result_class_, "sourceVars", "[I");
    PLOG_INFO << "Source var ID: " << source_vars_field_id;
    jintArray source_vars =
        (jintArray)jni_->GetObjectField(result_, source_vars_field_id);

    const auto source_vars_length = jni_->GetArrayLength(source_vars);
    auto source_vars_cpy = jni_->GetIntArrayElements(source_vars, NULL);
    for (int i = 0; i < source_vars_length; i++) {
        const auto slot = source_vars_cpy[i];
        jint taint_slot = GetCorrespondingTaintObjectSlot(
            frame_, depth_, slot, table_, table_size_);
        if (taint_slot == -1) continue;
        jobject taint;
        jvmti_->GetLocalObject(thread_, depth_, taint_slot, &taint);
        if (taint == NULL) continue;
        jni_->CallStaticVoidMethod(runtime_class_, analyze_source_method_id,
                                   taint, exception_, location_);
    }
    jni_->ReleaseIntArrayElements(source_vars, source_vars_cpy, NULL);
}

jint AffectedResultProcessor::GetCorrespondingTaintObjectSlot(
    jvmtiFrameInfo frame, int depth, int slot, jvmtiLocalVariableEntry *table,
    int table_size) {
    for (int i = 0; i < table_size; i++) {
        auto entry = table[i];
        if (!std::string(entry.name)
                .starts_with("phosphorShadowLVFor" + std::to_string(slot))) {
            continue;
        }
        if (entry.start_location > frame.location ||
            entry.start_location + entry.length < frame.location) {
            continue;
        }
        return entry.slot;
    }
    return -1;
}

void AffectedResultProcessor::ProcessAffectedVars() {
    PLOG_INFO << "Start processing affected variables!";
    auto affected_vars_field_id = jni_->GetFieldID(result_class_, "affectedVars", "[I");
    jintArray affected_vars =
        (jintArray)jni_->GetObjectField(result_, affected_vars_field_id);

    const auto affected_vars_length = jni_->GetArrayLength(affected_vars);
    auto affected_vars_cpy = jni_->GetIntArrayElements(affected_vars, NULL);

    auto taint_object_method_id = jni_->GetStaticMethodID(
        runtime_class_, kTaintObjectMethodName, kTaintObjectMethodDescriptor);
    auto update_taint_method_id = jni_->GetStaticMethodID(
        runtime_class_, kUpdateTaintMethodName, kUpdateTaintMethodDescriptor);

    for (int i = 0; i < affected_vars_length; i++) {
        const auto slot = affected_vars_cpy[i];
        std::string signature = GetLocalObjectSignature(
            frame_, depth_, slot, table_, table_size_);

        if (signature == "" || signature.contains("Ledu.columbia.cs")) {
            // Ignore taint objects.
            continue;
        }

        if (signature.starts_with("L") || signature.starts_with("[")) {
            jobject obj;
            jvmti_->GetLocalObject(thread_, depth_, slot, &obj);
            if (obj == NULL) continue;
            jni_->CallStaticVoidMethod(runtime_class_, taint_object_method_id,
                                       obj, slot, thread_, depth_, exception_);
            jni_->DeleteLocalRef(obj);
        } else if (is_caught_by_frame_) {
            // We only taint primitive types if the exception is
            // caught by the current frame.
            jint taint_slot = GetCorrespondingTaintObjectSlot(
                frame_, depth_, slot, table_, table_size_);
            if (taint_slot == -1) continue;
            jobject taint;
            jvmti_->GetLocalObject(thread_, depth_, taint_slot, &taint);
            if (taint == NULL) continue;
            jobject result = jni_->CallStaticObjectMethod(
                runtime_class_, update_taint_method_id, taint, slot, thread_,
                depth_, exception_);
            if (result != NULL) {
                jvmti_->SetLocalObject(thread_, depth_, taint_slot, result);
            }
            jni_->DeleteLocalRef(taint);
        }
    }
    jni_->ReleaseIntArrayElements(affected_vars, affected_vars_cpy, NULL);
}
}  // namespace exchain
