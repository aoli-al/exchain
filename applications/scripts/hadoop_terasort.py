from test_case_base import WrappedTest
import subprocess
import shutil
import os
import time
from commons import EXCHAIN_WORKDIR
import re

DIR_PATH = os.path.dirname(os.path.realpath(__file__))

class HadoopTerasort(WrappedTest):
    def __init__(self):
        super().__init__(
            "mapreduce_terasort",
            "Lorg/apache/",
            "hadoop-dist/target/hadoop-3.3.4",
            ["./bin/hadoop", "jar", "./share/hadoop/mapreduce/hadoop-mapreduce-examples-3.3.4.jar",
                "terasort", f"{EXCHAIN_WORKDIR}/workload-gen", f"{EXCHAIN_WORKDIR}/workload-out"],
            "HADOOP_OPTS",
            is_benchmark=True,
            work_dir=os.path.join(DIR_PATH, "..", "hadoop_bench")
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call(
            "mvn package -Pdist -DskipTests  -Dmaven.javadoc.skip=true -DskipTests=true", shell=True, cwd=self.work_dir)

    def convert_measurement(self, input: float) -> float:
        return 10000000 / input

    def pre(self):
        shutil.rmtree(f"{EXCHAIN_WORKDIR}/workload-gen", ignore_errors=True)
        shutil.rmtree(f"{EXCHAIN_WORKDIR}/workload-out", ignore_errors=True)
        print(self.origin_dist)
        subprocess.call([f"./bin/hadoop jar ./share/hadoop/mapreduce/hadoop-mapreduce-examples-3.3.4.jar teragen 10000000 {EXCHAIN_WORKDIR}/workload-gen"],
                        cwd=self.origin_dist, shell=True,
                        env={
                            "JAVA_HOME": os.path.join(os.path.expanduser("~"), ".jenv", "versions", "11")
        })

    def get_measure(self, type: str) -> str:
        if type == "latency":
            return "ms"
        else:
            return "mb/s"

    def post(self, type: str, debug: bool, cmd: subprocess.Popen, iter: int, disable_cache: bool):
        # super().post(type, debug, cmd)
        out, err = cmd.communicate()
        print(out.decode("utf-8"))
        print(err.decode('utf-8'))
        result = re.search(r"run time:(\d+\.?\d*)", out.decode('utf-8'))
        with open(self.perf_result_path(type, iter, disable_cache), "w") as f:
            f.write(f"exec_time, {result.group(1)}\n")

