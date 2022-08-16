#include "processor_base.hpp"

#include "plog/Log.h"

namespace exchain {
bool ProcessorBase::CheckJvmTIError(jvmtiError error, std::string msg) {
    if (error != JVMTI_ERROR_NONE) {
        char *error_name = "";
        jvmti_->GetErrorName(error, &error_name);
        PLOG_ERROR << "JVMTI: " << error << "(" << error_name << "): " << msg;
        jvmti_->Deallocate((unsigned char *)error_name);
        return false;
    }
    return true;
}
}  // namespace exchain
