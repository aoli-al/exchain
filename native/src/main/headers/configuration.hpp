#pragma once

#include <string>

namespace exchain {

enum ExceptionMode {
    LOGGING,
    STAT,
    FULL,
};

class Configuration {
   public:
    static Configuration& GetInstance() {
        static Configuration instance;
        return instance;
    }

   public:
    void Init(std::string args);
    ExceptionMode mode() { return mode_; };

   public:
    Configuration() {}
    Configuration(Configuration const &) = delete;
    void operator=(Configuration const &) = delete;

   private:
    ExceptionMode mode_ = FULL;
};

}  // namespace exchain
