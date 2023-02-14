from test_case_base import WrappedTest
import subprocess
import shutil
import time
import os
from commons import *


class Tomcat(WrappedTest):

    def __init__(self):
        super().__init__(
            "tomcat-65131",
            "Lorg/apache/tomcat",
            "output/build",
            ["bin/catalina.sh", "run"],
            "JAVA_OPTS",
        )
        self.src = os.path.join(self.work_dir, "output", "build")
        self.inst_dst = os.path.join(self.work_dir, "output", "instrumented")
        self.hybrid_dst = os.path.join(self.work_dir, "output", "hybrid")

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call("ant", cwd=self.work_dir, shell=True)

    def post(self, type: str, debug: bool, cmd: subprocess.Popen):
        time.sleep(10)
        subprocess.call("curl -q -k https://localhost:8443", shell=True)
        cmd.kill()

    # def exec(self, type: str, debug: bool = False) -> subprocess.Popen:
    #     if type == "static":
    #         return subprocess.Popen([os.path.join(self.src, "bin", "catalina.sh"), "run"],
    #                                 env={
    #             **os.environ
    #         }, cwd=self.work_dir)
    #     if type == "dynamic":
    #         return subprocess.Popen([os.path.join(self.inst_dst, "bin", "catalina.sh"), "run"],
    #                                 env={
    #             "JAVA_HOME": INSTRUMENTED_JAVA_HOME,
    #             "JAVA_OPTS":
    #             f"-javaagent:{PHOSPHOR_AGENT_PATH}=taintTagFactory=al.aoli.exchain.phosphor.instrumenter.DynamicSwitchTaintTagFactory -javaagent:{RUNTIME_JAR_PATH}=dynamic:{self.instrumentation_classpath} -agentpath:{NATIVE_LIB_PATH}=exchain:{self.application_namespace}",
    #             "EXCHAIN_OUT_DIR": self.out_path,
    #         }, cwd=self.work_dir)
    #     if type == "hybrid":
    #         return subprocess.Popen([os.path.join(self.hybrid_dst, "bin", "catalina.sh"), "run"],
    #                                 env={
    #             "JAVA_HOME": HYBRID_JAVA_HOME,
    #             "JAVA_OPTS":
    #             f"-javaagent:{PHOSPHOR_AGENT_PATH}=taintTagFactory=al.aoli.exchain.phosphor.instrumenter.FieldOnlyTaintTagFactory,postClassVisitor=al.aoli.exchain.phosphor.instrumenter.UninstrumentedOriginPostCV -javaagent:{RUNTIME_JAR_PATH}=hybrid:{self.hybrid_classpath} -agentpath:{NATIVE_LIB_PATH}=exchain:{self.application_namespace}",
    #             "EXCHAIN_OUT_DIR": self.out_path,
    #         }, cwd=self.work_dir)
    #     if type == "origin":
    #         f = open(self.origin_log_path, "w")
    #         return subprocess.Popen([os.path.join(self.src, "bin", "catalina.sh"), "run"],
    #                                 env={
    #             "JAVA_HOME": os.path.join(os.path.expanduser("~"), ".jenv", "versions", "11"),
    #             **os.environ
    #         }, cwd=self.work_dir, stdout=f, stderr=f)

    # def instrument(self):
    #     subprocess.call("jenv local 16", shell=True, cwd=self.work_dir)
    #     shutil.copytree(self.src, self.inst_dst, dirs_exist_ok=True)
    #     shutil.copytree(self.src, self.hybrid_dst, dirs_exist_ok=True)

    #     subprocess.call(["java",
    #                     f"-DPhosphor.INSTRUMENTATION_CLASSPATH={self.instrumentation_classpath}",
    #                      f"-DPhosphor.ORIGIN_CLASSPATH={self.origin_classpath}",
    #                      "-cp", PHOSPHOR_JAR_PATH, "edu.columbia.cs.psl.phosphor.Instrumenter",
    #                      os.path.join(self.src, "bin"), os.path.join(
    #                          self.inst_dst, "bin"),
    #                      "-taintTagFactory", "al.aoli.exchain.phosphor.instrumenter.DynamicSwitchTaintTagFactory",
    #                      ], cwd=self.work_dir)
    #     subprocess.call(["java",
    #                     f"-DPhosphor.INSTRUMENTATION_CLASSPATH={self.instrumentation_classpath}",
    #                      f"-DPhosphor.ORIGIN_CLASSPATH={self.origin_classpath}",
    #                      "-cp", PHOSPHOR_JAR_PATH, "edu.columbia.cs.psl.phosphor.Instrumenter",
    #                      os.path.join(self.src, "lib"), os.path.join(
    #                          self.inst_dst, "lib"),
    #                      "-taintTagFactory", "al.aoli.exchain.phosphor.instrumenter.DynamicSwitchTaintTagFactory",
    #                      ], cwd=self.work_dir)
    #     subprocess.call(["java",
    #                     f"-DPhosphor.INSTRUMENTATION_CLASSPATH={self.hybrid_classpath}",
    #                      "-cp", PHOSPHOR_JAR_PATH, "edu.columbia.cs.psl.phosphor.Instrumenter",
    #                      os.path.join(self.src, "lib"), os.path.join(
    #                          self.hybrid_dst, "lib"),
    #                      "-taintTagFactory", "al.aoli.exchain.phosphor.instrumenter.FieldOnlyTaintTagFactory",
    #                      "-postClassVisitor", "al.aoli.exchain.phosphor.instrumenter.UninstrumentedOriginPostCV"
    #                      ], cwd=self.work_dir)
    #     subprocess.call(["java",
    #                     f"-DPhosphor.INSTRUMENTATION_CLASSPATH={self.hybrid_classpath}",
    #                      "-cp", PHOSPHOR_JAR_PATH, "edu.columbia.cs.psl.phosphor.Instrumenter",
    #                      os.path.join(self.src, "lib"), os.path.join(
    #                          self.hybrid_dst, "lib"),
    #                      "-taintTagFactory", "al.aoli.exchain.phosphor.instrumenter.FieldOnlyTaintTagFactory",
    #                      "-postClassVisitor", "al.aoli.exchain.phosphor.instrumenter.UninstrumentedOriginPostCV"
    #                      ], cwd=self.work_dir)
