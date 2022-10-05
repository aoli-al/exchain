#include "affected_result_processor.hpp"

#include "plog/Log.h"
#include "configuration.hpp"

namespace exchain {

void AffectedResultProcessor::Process() {
    if (!ProcessorBase::CheckJvmTIError(
            jvmti_->GetLocalVariableTable(frame_.method, &table_size_, &table_),
            "get local variable table failed.")) {
        return;
    }
    PLOG_INFO << "Start processing affected result!";

    ProcessSourceVars();
    ProcessAffectedVars();

    jint modifier = 0;
    jvmti_->GetMethodModifiers(frame_.method, &modifier);
    if (!(modifier & 0x0008) && Configuration::GetInstance().mode() == TAINT) {
        ProcessAffectedFields();
        ProcessSourceFields();
    }

    if (Configuration::GetInstance().mode() == STAT) {
        ReportStats();
    }
    jvmti_->Deallocate((unsigned char *)table_);
}



void AffectedResultProcessor::ReportStats() {
    auto report_stats_method_id =
        jni_->GetStaticMethodID(runtime_class_, kOnExceptionStatsMethodName,
                                kOnExceptionStatsMethodDescriptor);
    PLOG_INFO << "report " << report_stats_method_id;
    jni_->CallStaticVoidMethod(runtime_class_, report_stats_method_id,
                               exception_, result_, num_of_objects_,
                               num_of_arrays_, num_of_primitives_,
                               num_of_nulls_, is_caught_by_frame_);
}

void AffectedResultProcessor::ProcessAffectedFields() {
    PLOG_INFO << "Start processing affected fields.";
    auto taint_fields_method_id = jni_->GetStaticMethodID(
        runtime_class_, kTaintFieldsMethodName, kTaintFieldsMethodDescriptor);
    jobject obj = NULL;
    jvmti_->GetLocalObject(thread_, depth_, 0, &obj);
    if (obj != NULL && taint_fields_method_id != NULL) {
        jni_->CallStaticVoidMethod(runtime_class_, taint_fields_method_id, obj,
                                   result_, exception_);
    }
}

void AffectedResultProcessor::ProcessSourceFields() {
    PLOG_INFO << "Start processing source fields.";
    auto analyze_source_method_id =
        jni_->GetStaticMethodID(runtime_class_, kAnalyzeSourceFieldsMethodName,
                                kAnalyzeSourceFieldsMethodDescriptor);
    jobject obj = NULL;
    jvmti_->GetLocalObject(thread_, depth_, 0, &obj);
    if (obj != NULL) {
        jni_->CallStaticVoidMethod(runtime_class_, analyze_source_method_id,
                                   obj, result_, exception_, location_);
    }
}

void AffectedResultProcessor::ProcessSourceVars() {
    PLOG_INFO << "Start analyzing exception sources!";
    if (Configuration::GetInstance().mode() != TAINT) return;
    auto analyze_source_method_id =
        jni_->GetStaticMethodID(runtime_class_, kAnalyzeSourceVarsMethodName,
                                kAnalyzeSourceVarsMethodDescriptor);
    auto source_vars_field_id =
        jni_->GetFieldID(result_class_, "sourceVars", "[I");
    jintArray source_vars =
        (jintArray)jni_->GetObjectField(result_, source_vars_field_id);

    const auto source_vars_length = jni_->GetArrayLength(source_vars);
    auto source_vars_cpy = jni_->GetIntArrayElements(source_vars, NULL);
    PLOG_INFO << "Source var size: " << source_vars_length;
    for (int i = 0; i < source_vars_length; i++) {
        const auto slot = source_vars_cpy[i];
        jint taint_slot = GetCorrespondingTaintObjectSlot(slot);
        if (taint_slot != -1) {
            jobject taint;
            jvmti_->GetLocalObject(thread_, depth_, taint_slot, &taint);
            if (taint != NULL) {
                jni_->CallStaticVoidMethod(runtime_class_,
                                           analyze_source_method_id, taint,
                                           exception_, location_);
            }
            jni_->DeleteLocalRef(taint);
        }

        auto *entry = GetLocalVariableEntry(slot);
        if (entry == nullptr) continue;
        std::string signature = entry->signature;
        if (signature.starts_with("L") || signature.starts_with("[")) {
            jobject obj;
            jvmti_->GetLocalObject(thread_, depth_, slot, &obj);
            if (obj != NULL) {
                jni_->CallStaticVoidMethod(runtime_class_,
                                           analyze_source_method_id, obj,
                                           exception_, location_);
            }
            jni_->DeleteLocalRef(obj);
        }
    }
    jni_->ReleaseIntArrayElements(source_vars, source_vars_cpy, NULL);
}

jvmtiLocalVariableEntry *AffectedResultProcessor::GetLocalVariableEntry(int slot) {
    for (int i = 0; i < table_size_; i++) {
        auto *entry = &table_[i];
        if (entry->slot == slot && entry->start_location <= frame_.location &&
            entry->start_location + entry->length >= frame_.location) {
        // if (entry->slot == slot) {
            return entry;
        }
    }
    return nullptr;
}


jint AffectedResultProcessor::GetCorrespondingTaintObjectSlot(int slot) {
    for (int i = 0; i < table_size_; i++) {
        auto entry = table_[i];
        if (!std::string(entry.name)
                 .starts_with("phosphorShadowLVFor" + std::to_string(slot) +
                              "XX")) {
            continue;
        }
        if (entry.start_location > frame_.location ||
            entry.start_location + entry.length < frame_.location) {
            continue;
        }
        LOG_INFO << "Found taint tag: " << entry.name << " for slot: " << slot;
        return entry.slot;
    }
    return -1;
}

void AffectedResultProcessor::ProcessAffectedVars() {
    PLOG_INFO << "Start processing affected variables!";
    auto affected_vars_field_id =
        jni_->GetFieldID(result_class_, "affectedVars", "[I");
    jintArray affected_vars =
        (jintArray)jni_->GetObjectField(result_, affected_vars_field_id);

    const auto affected_vars_length = jni_->GetArrayLength(affected_vars);
    auto affected_vars_cpy = jni_->GetIntArrayElements(affected_vars, NULL);

    auto taint_object_method_id = jni_->GetStaticMethodID(
        runtime_class_, kTaintObjectMethodName, kTaintObjectMethodDescriptor);
    auto update_taint_method_id = jni_->GetStaticMethodID(
        runtime_class_, kUpdateTaintMethodName, kUpdateTaintMethodDescriptor);

    PLOG_INFO << "Affected var size: " << affected_vars_length;
    for (int i = 0; i < affected_vars_length; i++) {
        const auto slot = affected_vars_cpy[i];
        auto *entry = GetLocalVariableEntry(slot);
        if (entry == nullptr) {
            LOG_INFO << "Ignored slot: " << slot;
            continue;
        }
        LOG_INFO << "Found local entry slot: " << slot
                 << " name: " << entry->name
                 << " signature: " << entry->signature
                 << " start: " << entry->start_location
                 << " length: " << entry->length;
        std::string signature = entry->signature;
        if (signature.contains("Ledu/columbia/cs")) {
            // Ignore taint objects.
            continue;
        }

        if (signature.starts_with("L")) {
            num_of_objects_ += 1;
        } else if (signature.starts_with("[")) {
            num_of_arrays_ += 1;
        } else {
            num_of_primitives_ += 1;
        }
        if (signature.starts_with("L") || signature.starts_with("[")) {
            jobject obj;
            jvmti_->GetLocalObject(thread_, depth_, slot, &obj);
            if (obj == NULL) {
                num_of_nulls_ += 1;
            }
        }
        local_variable_map_.emplace_back(entry->slot, entry->name);
        if (Configuration::GetInstance().mode() == TAINT) {
            // We only taint primitive types if the exception is
            // caught by the current frame.
            jint taint_slot = GetCorrespondingTaintObjectSlot(slot);

            if (is_caught_by_frame_ && taint_slot != -1) {
                PLOG_INFO << "Taint local variable with type: " << entry
                          << " at slot: " << slot;
                jobject taint;
                jvmti_->GetLocalObject(thread_, depth_, taint_slot, &taint);
                if (taint == NULL) continue;
                jobject result = jni_->CallStaticObjectMethod(
                    runtime_class_, update_taint_method_id, taint, slot,
                    thread_, depth_, exception_);
                if (result != NULL) {
                    jvmti_->SetLocalObject(thread_, depth_, taint_slot, result);
                }
                jni_->DeleteLocalRef(taint);
            } else if (signature.starts_with("L") ||
                       signature.starts_with("[")) {
                jobject obj;
                jvmti_->GetLocalObject(thread_, depth_, slot, &obj);
                if (obj == NULL) continue;
                PLOG_INFO << "Taint object with type: " << entry
                          << " at slot: " << slot;
                jni_->CallStaticVoidMethod(runtime_class_,
                                           taint_object_method_id, obj, slot,
                                           thread_, depth_, exception_);
                jni_->DeleteLocalRef(obj);
            }
        }
    }
    jni_->ReleaseIntArrayElements(affected_vars, affected_vars_cpy, NULL);
}
}  // namespace exchain
