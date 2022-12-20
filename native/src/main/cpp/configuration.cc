#include "configuration.hpp"

namespace exchain {

void Configuration::Init(std::string args) {
    std::string delimiter = ":";
    std::string type = args.substr(0, args.find(delimiter));
    application_ = args.substr(args.find(delimiter) + 1, args.length());
    if (type == "logging") {
        mode_ = LOGGING;
    } else {
        mode_ = EXCHAIN;
    }
}

}  // namespace exchain