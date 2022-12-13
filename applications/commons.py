import os
from sys import platform


BASE_FOLDER = os.path.dirname(os.path.realpath(__file__))

INSTRUMENTED_JAVA_EXEC = os.path.join(BASE_FOLDER, "..", "runtime/build/jre-inst/bin/java")
INSTRUMENTED_JAVA_HOME = os.path.join(BASE_FOLDER, "..", "runtime/build/jre-inst/")
PHOSPHOR_AGENT_PATH = os.path.join(BASE_FOLDER, "..", "dependencies/phosphor/phosphor-jigsaw-javaagent/target/phosphor-jigsaw-javaagent-0.1.0-SNAPSHOT.jar")
PHOSPHOR_JAR_PATH = os.path.join(BASE_FOLDER, "..", "dependencies/phosphor/Phosphor/target/Phosphor-0.1.0-SNAPSHOT.jar")

INSTRUMENTATION_AGENT_PATH = os.path.join(BASE_FOLDER, "..", "instrumentation/build/libs/instrumentation.jar")
RUNTIME_JAR_PATH = os.path.join(BASE_FOLDER, "..", "runtime/build/libs/runtime-shadow.jar")



if platform == "linux" or platform == "linux2":
    NATIVE_LIB_PATH = os.path.join(BASE_FOLDER, "..", "native/build/lib/main/debug/libnative.so")
else:
    NATIVE_LIB_PATH = os.path.join(BASE_FOLDER, "..", "native/build/lib/main/debug/libnative.dylib")