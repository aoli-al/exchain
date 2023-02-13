from benchmark import Benchmark
import subprocess
import shutil
import os
from commons import *

class Tomcat(Benchmark):
    def __init__(self):
        super().__init__(
            "tomcat-65131",
            "",
            "",
            "",
            ""
        )
        self.src = os.path.join(self.work_dir, "output", "build")
        self.inst_dst = os.path.join(self.work_dir, "output", "instrumented")
        self.hybrid_dst = os.path.join(self.work_dir, "output", "hybrid")

    def build(self):
        subprocess.call("jenv local 11", shell=True)
        subprocess.call("ant", cwd=self.work_dir, shell=True)

    def run_test(self, type: str, debug: bool = False):
        if type == "dynamic":
            subprocess.call([os.path.join(self.inst_dst, "bin", "cataliba.sh"), "run"],
                            env={
                                "JAVA_HOME": INSTRUMENTED_JAVA_HOME
                            }, cwd=self.work_dir)

    def instrument(self):
        subprocess.call("jenv local 16", shell=True, cwd=self.work_dir)
        shutil.copytree(self.src, self.inst_dst, dirs_exist_ok=True)
        shutil.copytree(self.src, self.hybrid_dst, dirs_exist_ok=True)


        subprocess.call(["java",
                        f"-DPhosphor.INSTRUMENTATION_CLASSPATH={self.instrumentation_classpath}",
                         f"-DPhosphor.ORIGIN_CLASSPATH={self.origin_classpath}",
                         "-cp", PHOSPHOR_JAR_PATH, "edu.columbia.cs.psl.phosphor.Instrumenter",
                         os.path.join(self.src, "bin"), os.path.join(self.inst_dst, "bin"),
                         "-taintTagFactory", "al.aoli.exchain.phosphor.instrumenter.DynamicSwitchTaintTagFactory",
                         ], cwd=self.work_dir)
        subprocess.call(["java",
                        f"-DPhosphor.INSTRUMENTATION_CLASSPATH={self.instrumentation_classpath}",
                         f"-DPhosphor.ORIGIN_CLASSPATH={self.origin_classpath}",
                         "-cp", PHOSPHOR_JAR_PATH, "edu.columbia.cs.psl.phosphor.Instrumenter",
                         os.path.join(self.src, "lib"), os.path.join(self.inst_dst, "lib"),
                         "-taintTagFactory", "al.aoli.exchain.phosphor.instrumenter.DynamicSwitchTaintTagFactory",
                         ], cwd=self.work_dir)
        subprocess.call(["java",
                        f"-DPhosphor.INSTRUMENTATION_CLASSPATH={self.hybrid_classpath}",
                         "-cp", PHOSPHOR_JAR_PATH, "edu.columbia.cs.psl.phosphor.Instrumenter",
                         os.path.join(self.src, "lib"), os.path.join(self.hybrid_dst, "lib"),
                         "-taintTagFactory", "al.aoli.exchain.phosphor.instrumenter.FieldOnlyTaintTagFactory",
                         "-postClassVisitor", "al.aoli.exchain.phosphor.instrumenter.UninstrumentedOriginPostCV"
                         ], cwd=self.work_dir)
        subprocess.call(["java",
                        f"-DPhosphor.INSTRUMENTATION_CLASSPATH={self.hybrid_classpath}",
                         "-cp", PHOSPHOR_JAR_PATH, "edu.columbia.cs.psl.phosphor.Instrumenter",
                         os.path.join(self.src, "lib"), os.path.join(self.hybrid_dst, "lib"),
                         "-taintTagFactory", "al.aoli.exchain.phosphor.instrumenter.FieldOnlyTaintTagFactory",
                         "-postClassVisitor", "al.aoli.exchain.phosphor.instrumenter.UninstrumentedOriginPostCV"
                         ], cwd=self.work_dir)