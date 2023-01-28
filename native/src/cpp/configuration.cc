#include "configuration.hpp"

#include <ranges>
#include <sstream>
#include <string>

namespace exchain {

void Configuration::Init(std::string args) {
    char delim = ':';
    std::string line;
    std::stringstream ss(args);
    while(std::getline(ss, line, delim)) {
        application_.push_back(line);
    }
    mode_ = EXCHAIN;
    // if (type == "logging") {
    //     mode_ = LOGGING;
    // } else {
    //     mode_ = EXCHAIN;
    // }
}

}  // namespace exchain