from test_case_base import WrappedTest
import subprocess
import time
import os
from typing import Tuple, List, Dict


class Solr(WrappedTest):

    def __init__(self):
        super().__init__(
            "solr_bench",
            "Lorg/apache/solr",
            "solr/benchmark/build/libs/",
            ["bash", "jmh.sh"],
            "JAVA_OPTS",
            is_benchmark=True,
        )

    def get_exec_command(self, type: str, debug: bool) -> Tuple[List[str], Dict[str, str], str]:
        cmd, env, work_dir = super().get_exec_command(type, debug)
        print(cmd)
        if type == "dynamic":
            cmd.append(self.dynamic_dist)
        elif type == "hybrid":
            cmd.append(self.hybrid_dist)
        else:
            cmd.append(self.origin_dist)
        work_dir = os.path.join(self.work_dir, "solr", "benchmark")
        print(cmd)
        print(work_dir)
        return cmd, env, work_dir

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call(["./gradlew", ":solr:build", "-Pvalidation.git.failOnModified=false"], cwd=self.work_dir)
        subprocess.call(["./gradlew", ":solr:benchmark:jar"], cwd=self.work_dir)
        subprocess.call(["./gradlew", ":solr:benchmark:copyDependencies"], cwd=self.work_dir)