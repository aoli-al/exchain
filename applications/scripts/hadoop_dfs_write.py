from test_case_base import WrappedTest
import subprocess
import shutil
import os
import re
from typing import Optional, Tuple
from commons import EXCHAIN_WORKDIR

DIR_PATH = os.path.dirname(os.path.realpath(__file__))

class HadoopDFSWrite(WrappedTest):
    def __init__(self):
        super().__init__(
            "hdfs_dfs_write",
            "Lorg/apache/",
            "hadoop-dist/target/hadoop-3.3.4",
            ["./bin/hadoop", "jar", "./share/hadoop/mapreduce/hadoop-mapreduce-client-jobclient-3.3.4-tests.jar",
             "TestDFSIO", f"-Dtest.build.data={EXCHAIN_WORKDIR}/dfs-write", "-Ddfs.replication=5", "-write", "-nrFiles", "50", "-size", "100MB"],
            "HADOOP_OPTS",
            is_benchmark=True,
            work_dir=os.path.join(DIR_PATH, "..", "hadoop_bench")
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call(
            "mvn package -Pdist -DskipTests  -Dmaven.javadoc.skip=true -DskipTests=true", shell=True, cwd=self.work_dir)

    def pre(self):
        shutil.rmtree(f"{EXCHAIN_WORKDIR}/dfs-write", ignore_errors=True)

    def post(self, type: str, debug: bool, cmd: subprocess.Popen, iter: int, disable_cache: bool):
        out, err = cmd.communicate()
        shutil.rmtree(f"{EXCHAIN_WORKDIR}/dfs-write", ignore_errors=True)
        result, latency = self.find_result(err.decode("utf-8"))
        with open(self.perf_result_path(type, iter, disable_cache), "w") as f:
            f.write(f"throughput, {result}\n")
            f.write(f"exec_time, {latency}\n")

    def get_measure(self, type: str) -> str:
        if type == "latency":
            return "s"
        else:
            return "mb/s"


    def find_result(self, out: str):
        print(out)
        result = re.search(r"Throughput mb/sec: (\d+\.?\d*)", out)
        throughput = float(result.group(1))
        result = re.search(r"Test exec time sec: (\d+\.?\d*)", out)
        exec_time = float(result.group(1))
        return throughput, exec_time


