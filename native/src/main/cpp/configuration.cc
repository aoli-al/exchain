#include "configuration.hpp"

namespace exchain {

void Configuration::Init(std::string args) {
    if (args == "logging") {
        mode_ = LOGGING;
    }
}

}  // namespace exchain