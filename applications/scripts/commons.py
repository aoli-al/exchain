import os
import sys


BASE_FOLDER = os.path.dirname(os.path.realpath(__file__))

EXCHAIN_WORKDIR = os.path.join(os.path.expanduser('~'), "tmp")

INSTRUMENTED_JAVA_HOME = os.path.join(BASE_FOLDER, "../..", "runtime/build/jre-inst/")
INSTRUMENTED_JAVA_EXEC = os.path.join(BASE_FOLDER, "../..", "runtime/build/jre-inst/bin/java")
HYBRID_JAVA_EXEC = os.path.join(BASE_FOLDER, "../..", "runtime/build/jre-inst-field-only/bin/java")
HYBRID_JAVA_HOME = os.path.join(BASE_FOLDER, "../..", "runtime/build/jre-inst-field-only/")
PHOSPHOR_AGENT_PATH = os.path.join(BASE_FOLDER, "../..", "dependencies/phosphor/phosphor-jigsaw-javaagent/target/phosphor-jigsaw-javaagent-0.1.0-SNAPSHOT.jar")
PHOSPHOR_JAR_PATH = os.path.join(BASE_FOLDER, "../..", "phosphor/build/libs/phosphor.jar") + ":" + os.path.join(
    BASE_FOLDER, "../..", "dependencies/phosphor/Phosphor/target/Phosphor-0.1.0-SNAPSHOT.jar")

RUNTIME_JAR_PATH = os.path.join(BASE_FOLDER, "../..", "runtime/build/libs/runtime-shadow.jar")
EXCHAIN_OUT_DIR = os.path.join(BASE_FOLDER, "../results")

TEST_NAME = os.getcwd().split("/")[-1]

DEFAULT_TYPES = ["origin", "dynamic", "hybrid", "static"]


if sys.platform == "linux" or sys.platform == "linux2":
    NATIVE_LIB_PATH = os.path.join(BASE_FOLDER, "../..", "native/build/cmake/native_release/linux-amd64/cpp/libnative.so")
else:
    NATIVE_LIB_PATH = os.path.join(BASE_FOLDER, "../..", "native/build/cmake/native_release/macos_x86/cpp/libnative.dylib")
