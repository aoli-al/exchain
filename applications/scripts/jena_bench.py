from test_case_base import SingleCommandTest
import subprocess
import shutil
import time
import os
from typing import *
from commons import *


class Jena(SingleCommandTest):

    def __init__(self):
        super().__init__(
            "jena_bench",
            "jena-integration-tests-4.7.0-test-jar-with-dependencies.jar",
            "jena-integration-tests/target",
            "org.apache.jena.Runner",
            "Lcom/hp/hpl:Lorg/apache/jena",
            is_benchmark=True
        )

    def get_exec_command(self, type: str, debug: bool) -> Tuple[List[str], Dict[str, str], str]:
        (cmd, env, work_dir) = super().get_exec_command(type, debug)
        env["PERF_OUT_FILE"] = os.path.join(self.out_path, f"{type}-perf.txt")
        return cmd, env, work_dir

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call(["./mvnw", "install", "-DskipTests", "-Drat.skip=true", "-Dmaven.javadoc.skip=true"], cwd=self.work_dir)

