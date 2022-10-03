#include "configuration.hpp"

namespace exchain {

void Configuration::Init(std::string args) {
    if (args == "logging") {
        mode_ = LOGGING;
    } else if (args == "stats") {
        mode_ = STAT;
    }
}

}  // namespace exchain