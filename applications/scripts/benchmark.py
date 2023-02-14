import subprocess
from commons import *
from typing import List, Optional
import glob
import time
import os
from objects import *
import jsonpickle
from process_results import *

DIR_PATH = os.path.dirname(os.path.realpath(__file__))


class Benchmark:

    def __init__(self, test_name: str, jar_name: str, origin_jar_path: str, test_class: str, application_namespace: str,
                 is_single_jar: bool = True,
                 additional_args: List[str] = [],
                 additional_classpaths: List[str] = [],
                 is_async: bool = False,
                 ignored_type: List[str] = [],
                 is_running_service: bool = True):
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
        self.ground_truth_path = os.path.join(BASE_FOLDER, "data", f"{self.test_name}.json")
        self.origin_log_path = os.path.join(self.out_path, "program_out.txt")
        self.is_async = is_async
        self.ignored_type = ignored_type
        self.is_running_service = is_running_service


        os.makedirs(self.out_path, exist_ok=True)
        os.makedirs(self.instrumentation_classpath, exist_ok=True)
        os.makedirs(self.instrumentation_output, exist_ok=True)
        os.makedirs(self.origin_classpath, exist_ok=True)
        os.makedirs(self.hybrid_classpath, exist_ok=True)
        os.makedirs(self.hybrid_output, exist_ok=True)

        if "JAVA_HOME" in os.environ:
            del os.environ["JAVA_HOME"]
        if "JRE_HOME" in os.environ:
            del os.environ["JRE_HOME"]

    def get_latest_result(self, type: str)-> str:
        base_dir = os.path.join(self.out_path, f"{type}-results")
        latest = open(os.path.join(base_dir, "latest")).read().strip()
        return os.path.join(base_dir, latest)

    def read_ground_truth(self) -> List[Tuple[Link, LinkType]]:
        data = jsonpickle.decode(open(self.ground_truth_path).read())
        return data

    def read_latest_dynamic_dependency(self) -> List[Link]:
        path = self.get_latest_result("dynamic")
        exception_data = read_exceptions(os.path.join(path, "exception.json"))
        dependencies = read_dynamic_dependencies(os.path.join(path, "dynamic_dependency.json"), exception_data)
        return list(dependencies)

    def read_latest_hybrid_dependency(self) -> Tuple[List[Link], List[Link]]:
        path = self.get_latest_result("hybrid")
        exception_data = read_exceptions(os.path.join(path, "exception.json"))
        dynamic_dependency = read_dynamic_dependencies(os.path.join(
            path, "dynamic_dependency.json"), exception_data)
        static_dependency = read_static_dependencies(os.path.join(
            path, "dependency.json"), exception_data)

        return list(dynamic_dependency), list(static_dependency)

    def read_latest_static_dependency(self) -> List[Link]:
        path = self.get_latest_result("static")
        exception_data = read_exceptions(os.path.join(path, "exception.json"))
        dependencies = read_static_dependencies(os.path.join(
            path, "dependency.json"), exception_data)
        return list(dependencies)

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
#
    def get_origin_jar(self) -> str:
        return ":".join(self.additional_classpaths + sorted(list(glob.glob(f"{self.origin_jar_path}/*.jar"))))

    def get_hybrid_jar(self) -> str:
        return ":".join(self.additional_classpaths + sorted(list(glob.glob(f"{self.hybrid_output}/*.jar"))))

    def get_instrumented_jar(self) -> str:
        return ":".join(self.additional_classpaths + sorted(list(glob.glob(f"{self.instrumentation_output}/*.jar"))))

    def post(self, type: str, debug: bool, cmd: subprocess.Popen):
        time.sleep(200)
        if not debug:
            cmd.kill()
        else:
            cmd.communicate()

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
                f"-javaagent:{PHOSPHOR_AGENT_PATH}=taintTagFactory=al.aoli.exchain.phosphor.instrumenter.FieldOnlyTaintTagFactory,postClassVisitor=al.aoli.exchain.phosphor.instrumenter.UninstrumentedOriginPostCV",
                f"-javaagent:{RUNTIME_JAR_PATH}=hybrid:{self.hybrid_classpath}",
                f"-agentpath:{NATIVE_LIB_PATH}=exchain:{self.application_namespace}",
                self.test_class]

    def dynamic_commands(self) -> List[str]:
        return [
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

    def post_analysis(self, type: str, debug: bool = False):
        print(self.out_path)
        if type == "static":
            args = [f"--args={self.origin_classpath} {self.out_path}/static-results"]
        elif type == "hybrid":
            args = [f"--args={self.hybrid_classpath} {self.out_path}/hybrid-results"]
        else:
            return
        if debug:
            args.insert(0, "--debug-jvm")

        subprocess.call(["./gradlew", "static-analyzer:run", *args],
                        cwd=os.path.join(DIR_PATH, "../.."), timeout=60 * 60 * 8)

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
        cmd = [java, *self.additional_args, *cmd]
        print(" ".join(cmd))
        if type == "origin":
            f = open(self.origin_log_path, "w")
        else:
            f = sys.stdout.buffer
        return subprocess.Popen(cmd,
                                stdout=f, stderr=f,
                                env={
            "EXCHAIN_OUT_DIR": self.out_path,
            **os.environ
        }, cwd=self.work_dir)
