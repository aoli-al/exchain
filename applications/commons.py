import os
from sys import platform


BASE_FOLDER = os.path.dirname(os.path.realpath(__file__))

INSTRUMENTED_JAVA_EXEC = os.path.join(BASE_FOLDER, "..", "runtime/build/jre-inst/bin/java")
HYBRID_JAVA_EXEC = os.path.join(BASE_FOLDER, "..", "runtime/build/jre-inst-field-only/bin/java")
HYBRID_JAVA_HOME = os.path.join(BASE_FOLDER, "..", "runtime/build/jre-inst-field-only/")
PHOSPHOR_AGENT_PATH = os.path.join(BASE_FOLDER, "..", "dependencies/phosphor/phosphor-jigsaw-javaagent/target/phosphor-jigsaw-javaagent-0.1.0-SNAPSHOT.jar")
PHOSPHOR_JAR_PATH = os.path.join(BASE_FOLDER, "..", "phosphor/build/libs/phosphor.jar") + ":" + os.path.join(
    BASE_FOLDER, "..", "dependencies/phosphor/Phosphor/target/Phosphor-0.1.0-SNAPSHOT.jar")

RUNTIME_JAR_PATH = os.path.join(BASE_FOLDER, "..", "runtime/build/libs/runtime-shadow.jar")

INSTRUMENTATION_FOLDER_NAME = "/tmp/instrumented"
HYBRID_FOLDER_NAME = "/tmp/hybrid"
INSTRUMENTATION_CLASSPATH = "/tmp/instrumented_classes"
ORIGIN_CLASSPATH = "/tmp/origin_classes"
HYBRID_CLASSPATH = "/tmp/hybrid_classes"



if platform == "linux" or platform == "linux2":
    NATIVE_LIB_PATH = os.path.join(BASE_FOLDER, "..", "native/build/cmake/native_release/linux-amd64/cpp/libnative.so")
else:
    NATIVE_LIB_PATH = os.path.join(BASE_FOLDER, "..", "native/build/cmake/native_release/macos_x86/cpp/libnative.dylib")
