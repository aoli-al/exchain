import subprocess
from commons import *
from typing import List
import glob
import time
import os

DIR_PATH = os.path.dirname(os.path.realpath(__file__))


class Benchmark:

    def __init__(self, test_name: str, jar_name: str, origin_jar_path: str, test_class: str, application_namespace: str, is_single_jar: bool = True, additional_args: List[str] = [],
                 additional_classpaths: List[str] = []):
        self.test_name = test_name
        self.jar_name = jar_name
        self.test_class = test_class
        self.application_namespace = application_namespace
        self.work_dir = os.path.join(DIR_PATH, "..", self.test_name)
        self.origin_jar_path = os.path.join(self.work_dir, origin_jar_path)
        self.is_single_jar = is_single_jar
        self.additional_args = additional_args
        self.instrumentation_classpath = "/tmp/instrumented_classes/" + self.test_name
        self.instrumentation_output = "/tmp/instrumentation_output/" + self.test_name
        self.origin_classpath = "/tmp/origin_classes/" + self.test_name
        self.hybrid_classpath = "/tmp/hybrid_classes/" + self.test_name
        self.hybrid_output = "/tmp/hybrid_output/" + self.test_name
        self.additional_classpaths = additional_classpaths
        self.out_path = os.path.join(EXCHAIN_OUT_DIR, self.test_name)

        os.makedirs(self.instrumentation_classpath, exist_ok=True)
        os.makedirs(self.instrumentation_output, exist_ok=True)
        os.makedirs(self.origin_classpath, exist_ok=True)
        os.makedirs(self.hybrid_classpath, exist_ok=True)
        os.makedirs(self.hybrid_output, exist_ok=True)

        if "JAVA_HOME" in os.environ:
            del os.environ["JAVA_HOME"]

    def build(self):
        pass

    def instrument(self):
        subprocess.call("jenv local 16", shell=True, cwd=self.work_dir)
        if self.is_single_jar:
            input_name = f"{self.origin_jar_path}/{self.jar_name}"
        else:
            input_name = self.origin_jar_path

        subprocess.call(["java",
                        f"-DPhosphor.INSTRUMENTATION_CLASSPATH={self.instrumentation_classpath}",
                         f"-DPhosphor.ORIGIN_CLASSPATH={self.origin_classpath}",
                         "-cp", PHOSPHOR_JAR_PATH, "edu.columbia.cs.psl.phosphor.Instrumenter",
                         input_name, self.instrumentation_output,
                         "-taintTagFactory", "al.aoli.exchain.phosphor.instrumenter.DynamicSwitchTaintTagFactory",
                         #  "-postClassVisitor", "al.aoli.exchain.phosphor.instrumenter.splitter.MethodSplitPostCV",
                         #  "-priorClassVisitor", "al.aoli.exchain.phosphor.instrumenter.splitter.MethodSplitPreCV"
                         ], cwd=self.work_dir)
        subprocess.call(["java",
                        f"-DPhosphor.INSTRUMENTATION_CLASSPATH={self.hybrid_classpath}",
                         "-cp", PHOSPHOR_JAR_PATH, "edu.columbia.cs.psl.phosphor.Instrumenter",
                         input_name, self.hybrid_output,
                         "-taintTagFactory", "al.aoli.exchain.phosphor.instrumenter.FieldOnlyTaintTagFactory",
                         "-postClassVisitor", "al.aoli.exchain.phosphor.instrumenter.UninstrumentedOriginPostCV"
                         ], cwd=self.work_dir)

    def get_origin_jar(self) -> str:
        return ":".join(sorted(list(glob.glob(f"{self.origin_jar_path}/*.jar")) + self.additional_classpaths))

    def get_hybrid_jar(self) -> str:
        return ":".join(sorted(list(glob.glob(f"{self.hybrid_output}/*.jar")) + self.additional_classpaths))

    def get_instrumented_jar(self) -> str:
        return ":".join(sorted(list(glob.glob(f"{self.instrumentation_output}/*.jar")) + self.additional_classpaths))

    def post(self, type: str, debug: bool, cmd: subprocess.Popen):
        time.sleep(200)
        if not debug:
            cmd.kill()

    def pre(self):
        pass

    def origin_commands(self) -> List[str]:
        return ["-cp", self.get_origin_jar(), self.test_class]

    def static_commands(self) -> List[str]:
        return ["-cp", self.get_origin_jar(),
                f"-javaagent:{RUNTIME_JAR_PATH}=static:{self.origin_classpath}",
                f"-agentpath:{NATIVE_LIB_PATH}=exchain:{self.application_namespace}",
                self.test_class]

    def hybrid_commands(self) -> List[str]:
        return ["-cp", self.get_hybrid_jar(),
                f"-DPhosphor.INSTRUMENTATION_CLASSPATH={self.hybrid_classpath}",
                f"-DPhosphor.ORIGIN_CLASSPATH={self.origin_classpath}",
                f"-javaagent:{PHOSPHOR_AGENT_PATH}=taintTagFactory=al.aoli.exchain.phosphor.instrumenter.FieldOnlyTaintTagFactory,postClassVisitor=al.aoli.exchain.phosphor.instrumenter.UninstrumentedOriginPostCV",
                f"-javaagent:{RUNTIME_JAR_PATH}=hybrid:{self.hybrid_classpath}",
                f"-agentpath:{NATIVE_LIB_PATH}=exchain:{self.application_namespace}",
                self.test_class]

    def dynamic_commands(self) -> List[str]:
        return [
            f"-DPhosphor.INSTRUMENTATION_CLASSPATH={self.instrumentation_classpath}",
            f"-DPhosphor.ORIGIN_CLASSPATH={self.origin_classpath}",
            "-cp", self.get_instrumented_jar(),
            f"-javaagent:{PHOSPHOR_AGENT_PATH}=taintTagFactory=al.aoli.exchain.phosphor.instrumenter.DynamicSwitchTaintTagFactory",
            # ",postClassVisitor=al.aoli.exchain.phosphor.instrumenter.splitter.MethodSplitPostCV"
            # ",priorClassVisitor=al.aoli.exchain.phosphor.instrumenter.splitter.MethodSplitPreCV",
            f"-javaagent:{RUNTIME_JAR_PATH}=dynamic:{self.instrumentation_classpath}",
            f"-agentpath:{NATIVE_LIB_PATH}=exchain:{self.application_namespace}",
            self.test_class]

    def run_test(self, type: str, debug: bool = False):
        self.pre()
        cmd = self.exec(type, debug)
        self.post(type, debug, cmd)

    def post_analysis(self, type: str):
        print(self.out_path)
        if type == "static":
            subprocess.call(["./gradlew", "static-analyzer:run", f"--args={self.origin_classpath} {self.out_path}/static-results"],
                            cwd=os.path.join(DIR_PATH, "../.."))
        elif type == "hybrid":
            subprocess.call(["./gradlew", "static-analyzer:run", f"--args={self.hybrid_classpath} {self.out_path}/hybrid-results"],
                            cwd=os.path.join(DIR_PATH, "../.."))

    def exec(self, type: str, debug: bool) -> subprocess.Popen:
        if type == "origin":
            cmd = self.origin_commands()
            java = "java"
        elif type == "static":
            cmd = self.static_commands()
            java = "java"
        elif type == "hybrid":
            cmd = self.hybrid_commands()
            java = HYBRID_JAVA_EXEC
        elif type == "dynamic":
            cmd = self.dynamic_commands()
            java = INSTRUMENTED_JAVA_EXEC

        if debug:
            cmd.insert(
                0, "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")
        if type == "origin":
            f = open(os.path.join([java, *self.additional_args, *cmd]))
        else:
            f = sys.stdout.buffer
        return subprocess.Popen([java, *self.additional_args, *cmd],
                                stdout=f, stderr=f,
                                env={
            "EXCHAIN_OUT_DIR": self.out_path,
            **os.environ
        }, cwd=self.work_dir)
