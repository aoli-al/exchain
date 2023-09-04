import subprocess
from commons import *
from typing import List, Optional, Any
import glob
import time
import os
from objects import *
import jsonpickle
import shutil
from process_results import *

DIR_PATH = os.path.dirname(os.path.realpath(__file__))


INTRINSICS = [
    "UseSHA1Intrinsics",
    "UseSHA3Intrinsics",
    "UseAdler32Intrinsics",
    "UseBASE64Intrinsics",
    "UseCharacterCompareIntrinsics",
    "UseCopySignIntrinsic",
    "UseSignumIntrinsic"
]
NO_OPT_ARGS = "-XX:+UnlockDiagnosticVMOptions " + " ".join([
    f"-XX:-{x}" for x in INTRINSICS
])


class Test:

    def __init__(self, test_name: str, application_namespace: str,
                 is_async: bool = False,
                 ignored_type: List[str] = [],
                 is_implicit: bool = True,
                 is_benchmark: bool = False,
                 work_dir: Optional[str] = None,
                 test_class: str = ""):
        self.test_name = test_name
        self.application_namespace = application_namespace
        self.work_dir = os.path.join(
            DIR_PATH, "..", self.test_name) if work_dir is None else work_dir
        self.instrumentation_classpath = EXCHAIN_WORKDIR + "/instrumented_classes/" + self.test_name
        self.origin_classpath = EXCHAIN_WORKDIR + "/origin_classes/" + self.test_name
        self.hybrid_classpath = EXCHAIN_WORKDIR + "/hybrid_classes/" + self.test_name
        self.out_path = os.path.join(EXCHAIN_OUT_DIR, self.test_name)
        self.ground_truth_path = os.path.join(
            BASE_FOLDER, "data", f"{self.test_name}.json")
        self.origin_log_path = os.path.join(self.out_path, "program_out.txt")
        self.is_async = is_async
        self.ignored_type = ignored_type
        self.is_implicit = is_implicit
        self.is_benchmark = is_benchmark
        self.test_class = test_class
        self.code_ql_out_dir = EXCHAIN_WORKDIR + "/codeql/" + self.test_name

        os.makedirs(self.out_path, exist_ok=True)
        os.makedirs(self.instrumentation_classpath, exist_ok=True)
        os.makedirs(self.origin_classpath, exist_ok=True)
        os.makedirs(self.hybrid_classpath, exist_ok=True)

        if "JAVA_HOME" in os.environ:
            del os.environ["JAVA_HOME"]
        if "JRE_HOME" in os.environ:
            del os.environ["JRE_HOME"]

    def convert_measurement(self, input: float) -> float:
        return input


    def analyze(self):
        types = ["assign", "call", "return"]
        for t in types:
            for v in ["all", "local"]:
                command = f"/home/aoli/.config/Code/User/globalStorage/github.vscode-codeql/distribution1/codeql/codeql query run --database={self.code_ql_out_dir} -- /home/aoli/repos/vscode-codeql-starter/codeql-custom-queries-java/{t}_{v}.ql"
                cmd = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)
                out, err = cmd.communicate()
                out = out.decode("utf-8")
                print(self.test_name, t, v, len(out.split("/n")))

    def get_latest_result(self, type: str) -> str:
        base_dir = os.path.join(self.out_path, f"{type}-results")
        latest = open(os.path.join(base_dir, "latest")).read().strip()
        return os.path.join(base_dir, latest)

    def perf_result_path(self, type: str, iter: int, disable_cache: bool) -> str:
        if disable_cache:
            return os.path.join(self.out_path, f"{type}-perf-no-cache.{iter}.txt")
        else:
            return os.path.join(self.out_path, f"{type}-perf.{iter}.txt")

    def read_ground_truth(self) -> List[Tuple[Link, LinkType]]:
        data = jsonpickle.decode(open(self.ground_truth_path).read())
        return data

    def read_latest_dynamic_dependency(self) -> List[Link]:
        path = self.get_latest_result("dynamic")
        exception_data = read_exceptions(os.path.join(path, "exception.json"))
        dependencies = read_dynamic_dependencies(os.path.join(
            path, "dynamic_dependency.json"), exception_data)
        return list(dependencies)

    def create_codeql_db(self):
        subprocess.call(["/home/aoli/.config/Code/User/globalStorage/github.vscode-codeql/distribution1/codeql/codeql",
        "database",
        "create",
        "--overwrite",
                         "--language=java",
                         f"{self.code_ql_out_dir}"],
                        cwd=self.work_dir)

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

    def read_latest_naive_static_dependency(self) -> List[Link]:
        path = self.get_latest_result("hybrid")
        exception_data = read_exceptions(os.path.join(path, "exception.json"))
        dependencies = read_static_dependencies(os.path.join(
            path, "dependency.naive.json"), exception_data)
        return list(dependencies)

    def build(self):
        pass

    def instrument_dynamic(self, input_path: str, output_path: str, debug=False):
        debug_options = []
        if debug:
            debug_options.append(
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")
        subprocess.call("jenv local 16", shell=True, cwd=self.work_dir)
        subprocess.call(["java", *debug_options,
                        f"-DPhosphor.INSTRUMENTATION_CLASSPATH={self.instrumentation_classpath}",
                         f"-DPhosphor.ORIGIN_CLASSPATH={self.origin_classpath}",
                         "-cp", PHOSPHOR_JAR_PATH, "edu.columbia.cs.psl.phosphor.Instrumenter",
                         input_path, output_path,
                         "-taintTagFactory", "al.aoli.exchain.phosphor.instrumenter.DynamicSwitchTaintTagFactory",
                         #  "-postClassVisitor", "al.aoli.exchain.phosphor.instrumenter.splitter.MethodSplitPostCV",
                         #  "-priorClassVisitor", "al.aoli.exchain.phosphor.instrumenter.splitter.MethodSplitPreCV"
                         ], cwd=self.work_dir)

    def instrument_hybrid(self, input_path: str, output_path: str, debug=False):
        debug_options = []
        if debug:
            debug_options.append(
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")
        subprocess.call("jenv local 16", shell=True, cwd=self.work_dir)
        subprocess.call(["java", *debug_options,
                        f"-DPhosphor.INSTRUMENTATION_CLASSPATH={self.hybrid_classpath}",
                         "-cp", PHOSPHOR_JAR_PATH, "edu.columbia.cs.psl.phosphor.Instrumenter",
                         input_path, output_path,
                         "-taintTagFactory", "al.aoli.exchain.phosphor.instrumenter.FieldOnlyTaintTagFactory",
                         "-postClassVisitor", "al.aoli.exchain.phosphor.instrumenter.UninstrumentedOriginPostCV"
                         ], cwd=self.work_dir)

    def instrument(self, debug: bool = False):
        pass

    def post(self, type: str, debug: bool, cmd: subprocess.Popen, iter: int, disable_cache: bool):
        if not self.is_benchmark and not debug:
            time.sleep(200)
            cmd.kill()
        else:
            out, err = cmd.communicate()

    def pre(self):
        pass

    def run_test(self, type: str, debug: bool = False, iter: int = 1, disable_cache: bool = False):
        for i in range(iter):
            print("?????!!!!!", type, debug, disable_cache)
            self.pre()
            cmd = self.exec(type, debug, disable_cache)
            self.post(type, debug, cmd, i, disable_cache)

    def process_bench_result(self, type: str, data: str):
        pass

    def post_analysis(self, type: str, debug: bool = False, naive: bool = False):
        if self.is_benchmark:
            return



        out_path = os.path.join(self.out_path, f"{type}-results")
        if type == "static":
            class_path = self.origin_classpath
        elif type == "hybrid":
            class_path = self.hybrid_classpath
        else:
            return
        if naive:
            ns = self.test_class + " --naive"
            if os.path.exists(os.path.join(self.get_latest_result(type), "dependency.naive.json")):
                return
        else:
            ns = self.application_namespace
            if os.path.exists(os.path.join(self.get_latest_result(type), "dependency.json")):
                return
        args = [
            f"--args={class_path} {out_path} {ns}"]

        if debug:
            args.insert(0, "--debug-jvm")

        start_time = time.time()
        print(args)
        subprocess.call(["./gradlew", "static-analyzer:run", *args],
                        cwd=os.path.join(DIR_PATH, "../.."), timeout=60 * 60 * 8,
                        env={"EXCHAIN_TYPE": "dynamic"})
        finish_time = time.time()
        if naive:
            out = "time.naive.txt"
        else:
            out = "time.txt"
        with open(os.path.join(out_path, out), "w") as f:
            f.write(str(finish_time - start_time))



    def get_exec_command(self, type: str, debug: bool) -> Tuple[List[str], Dict[str, str], str, Any]:
        return ([], {}, "", sys.stdout.buffer)

    def exec(self, type: str, debug: bool, disable_cache: bool) -> subprocess.Popen:
        cmd, env, work_dir, f = self.get_exec_command(type, debug)
        e = {
            "EXCHAIN_OUT_DIR": self.out_path,
            "EXCHAIN_TYPE": type,
            **env
        }
        if disable_cache:
            e["EXCHAIN_ENABLE_CACHE"] = "false"
        else:
            e["EXCHAIN_ENABLE_CACHE"] = "true"
        print(f"EXEC_TYPE: {type}")

        return subprocess.Popen(cmd,
                                stdout=f,
                                stderr=f,
                                env=e, cwd=work_dir)



class WrappedTest(Test):
    def __init__(self, test_name: str, application_namespace: str, dist_path: str, start_command: List[str], env_key: str, is_async: bool = False, ignored_type: List[str] = [], is_implicit: bool = True, is_benchmark: bool = False, work_dir: Optional[str] = None, test_class: str = ""):
        super().__init__(test_name, application_namespace,
                         is_async, ignored_type, is_implicit,
                         is_benchmark, work_dir, test_class)
        self.origin_dist = os.path.join(self.work_dir, dist_path)
        self.dynamic_dist = os.path.join(EXCHAIN_WORKDIR + "/dyn_dist/", self.test_name, "dyn_dist")
        self.hybrid_dist = os.path.join(EXCHAIN_WORKDIR + "/hybrid_dist/", self.test_name, "hybrid_dist")
        self.start_command = start_command
        self.env_key = env_key
        os.makedirs(self.dynamic_dist, exist_ok=True)
        os.makedirs(self.hybrid_dist, exist_ok=True)


    def instrument(self, debug: bool = False):
        shutil.copytree(self.origin_dist, self.dynamic_dist, dirs_exist_ok=True)
        shutil.copytree(self.origin_dist, self.hybrid_dist, dirs_exist_ok=True)

        self.instrument_dynamic(self.origin_dist, self.dynamic_dist, debug)
        self.instrument_hybrid(self.origin_dist, self.hybrid_dist, debug)

    def get_exec_command(self, type: str, debug: bool) -> Tuple[List[str], Dict[str, str], str, Any]:
        env = {}
        if type == "static":
            env["JAVA_HOME"] = os.path.join(os.path.expanduser("~"), ".jenv", "versions", "16")
            env[self.env_key] = f"-javaagent:{RUNTIME_JAR_PATH}=static:{self.origin_classpath} -agentpath:{NATIVE_LIB_PATH}=exchain:{self.application_namespace}"
            work_dir = self.origin_dist
        if type == "dynamic":
            env["JAVA_HOME"] = INSTRUMENTED_JAVA_HOME
            env[self.env_key] = f"-javaagent:{PHOSPHOR_AGENT_PATH}=taintTagFactory=al.aoli.exchain.phosphor.instrumenter.DynamicSwitchTaintTagFactory -javaagent:{RUNTIME_JAR_PATH}=dynamic:{self.instrumentation_classpath} -agentpath:{NATIVE_LIB_PATH}=exchain:{self.application_namespace}"
            work_dir = self.dynamic_dist
        if type == "hybrid":
            env["JAVA_HOME"] = os.path.join(os.path.expanduser("~"), ".jenv", "versions", "16")
            env[self.env_key] = f"-javaagent:{RUNTIME_JAR_PATH}=hybrid:{self.origin_classpath} -agentpath:{NATIVE_LIB_PATH}=exchain:{self.application_namespace}"
            work_dir = self.origin_dist
            #  env["JAVA_HOME"] = HYBRID_JAVA_HOME
            #  env[self.env_key] = f"-javaagent:{PHOSPHOR_AGENT_PATH}=taintTagFactory=al.aoli.exchain.phosphor.instrumenter.FieldOnlyTaintTagFactory,postClassVisitor=al.aoli.exchain.phosphor.instrumenter.UninstrumentedOriginPostCV -javaagent:{RUNTIME_JAR_PATH}=hybrid:{self.hybrid_classpath} -agentpath:{NATIVE_LIB_PATH}=exchain:{self.application_namespace}"
            #  work_dir = self.hybrid_dist
        if "origin" in type:
            env["JAVA_HOME"] = os.path.join(os.path.expanduser("~"), ".jenv", "versions", "16")
            work_dir = self.origin_dist
            if "debug" in type:
                if self.env_key not in env:
                    env[self.env_key] = f"-agentpath:{NATIVE_LIB_PATH}=dummy"
                else:
                    env[self.env_key] += f" -agentpath:{NATIVE_LIB_PATH}=dummy"
            if "noopt" in type:
                if self.env_key not in env:
                    env[self.env_key] = f"-javaagent:{RUNTIME_JAR_PATH}=hybrid:{self.hybrid_classpath} -agentpath:{NATIVE_LIB_PATH}=dummy"
                else:
                    env[self.env_key] += f" -javaagent:{RUNTIME_JAR_PATH}=hybrid:{self.hybrid_classpath} -agentpath:{NATIVE_LIB_PATH}=dummy"
        if debug:
            if self.env_key not in env:
                env[self.env_key] = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:5005"
            else:
                env[self.env_key] += " -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:5005"
        if type == "origin" and not self.is_benchmark:
            f = open(self.origin_log_path, "w")
        elif self.is_benchmark:
            #  f = subprocess.PIPE
            #  f = open(self.origin_log_path, "w")
            f = sys.stdout.buffer
        else:
            f = sys.stdout.buffer
            #  f = open(self.origin_log_path, "w")
        return (self.start_command, env, work_dir, f)



class SingleCommandTest(Test):

    def __init__(self, test_name: str, jar_name: str, origin_jar_path: str, test_class: str, application_namespace: str, additional_args: List[str] = [], additional_classpaths: List[str] = [], is_async: bool = False, ignored_type: List[str] = [], is_implicit: bool = True, is_single_jar: bool = True, is_benchmark: bool = False):
        super().__init__(test_name, application_namespace,
                         is_async, ignored_type, is_implicit,
                         is_benchmark, test_class=test_class)
        self.additional_classpaths = additional_classpaths
        self.additional_args = additional_args
        self.jar_name = jar_name
        self.origin_jar_path = origin_jar_path
        self.origin_jar_path = os.path.join(self.work_dir, origin_jar_path)
        self.is_single_jar = is_single_jar
        self.hybrid_output = EXCHAIN_WORKDIR + "/hybrid_output/" + self.test_name
        self.instrumentation_output = EXCHAIN_WORKDIR + "/instrumentation_output/" + self.test_name
        os.makedirs(self.instrumentation_output, exist_ok=True)
        os.makedirs(self.hybrid_output, exist_ok=True)

    def instrument(self, debug: bool = False):
        if self.is_single_jar:
            input_name = f"{self.origin_jar_path}/{self.jar_name}"
        else:
            input_name = self.origin_jar_path
        self.instrument_dynamic(input_name, self.instrumentation_output, debug)
        self.instrument_hybrid(input_name, self.hybrid_output, debug)

#
    def get_origin_jar(self) -> str:
        return ":".join(self.additional_classpaths + sorted(list(glob.glob(f"{self.origin_jar_path}/*.jar"))))

    def get_hybrid_jar(self) -> str:
        return ":".join(self.additional_classpaths + sorted(list(glob.glob(f"{self.hybrid_output}/*.jar"))))

    def get_instrumented_jar(self) -> str:
        return ":".join(self.additional_classpaths + sorted(list(glob.glob(f"{self.instrumentation_output}/*.jar"))))

    def origin_commands(self) -> List[str]:
        return ["-cp", self.get_origin_jar(), self.test_class]

    def static_commands(self) -> List[str]:
        return ["-cp", self.get_origin_jar(),
                f"-javaagent:{RUNTIME_JAR_PATH}=static:{self.origin_classpath}",
                f"-agentpath:{NATIVE_LIB_PATH}=exchain:{self.application_namespace}",
                self.test_class]

    def hybrid_commands(self) -> List[str]:
        return ["-cp", self.get_origin_jar(),
                f"-javaagent:{RUNTIME_JAR_PATH}=hybrid:{self.origin_classpath}",
                f"-agentpath:{NATIVE_LIB_PATH}=exchain:{self.application_namespace}",
                self.test_class]

    def dynamic_commands(self) -> List[str]:
        return [
            "-cp", self.get_instrumented_jar(),
            f"-javaagent:{PHOSPHOR_AGENT_PATH}=taintTagFactory=al.aoli.exchain.phosphor.instrumenter.DynamicSwitchTaintTagFactory",
            f"-javaagent:{RUNTIME_JAR_PATH}=dynamic:{self.instrumentation_classpath}",
            f"-agentpath:{NATIVE_LIB_PATH}=exchain:{self.application_namespace}",
            self.test_class]

    def get_exec_command(self, type: str, debug: bool) -> Tuple[List[str], Dict[str, str], str, Any]:
        if "origin" in type:
            cmd = self.origin_commands()
            java = os.path.join(os.path.expanduser("~"), ".jenv", "versions", "16", "bin", "java")
            if "debug" in type or "noopt" in type:
                cmd.insert(0, f"-agentpath:{NATIVE_LIB_PATH}=dummy")
                if "noopt" in type:
                    cmd[1:1] = [f"-javaagent:{RUNTIME_JAR_PATH}=hybrid:{self.hybrid_classpath}"]
        elif type == "static":
            cmd = self.static_commands()
            java = os.path.join(os.path.expanduser("~"), ".jenv", "versions", "16", "bin", "java")
        elif type == "hybrid":
            cmd = self.hybrid_commands()
            java = os.path.join(os.path.expanduser("~"), ".jenv", "versions", "16", "bin", "java")
        elif type == "dynamic":
            cmd = self.dynamic_commands()
            java = INSTRUMENTED_JAVA_EXEC

        if debug:
            cmd.insert(
                0, "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
        cmd = [java, *self.additional_args, *cmd]
        print(" ".join(cmd))
        if type == "origin" and not self.is_benchmark:
            f = open(self.origin_log_path, "w")
        elif self.is_benchmark:
            f = subprocess.PIPE
        else:
            f = sys.stdout.buffer
        return (cmd, os.environ, self.work_dir, f)
