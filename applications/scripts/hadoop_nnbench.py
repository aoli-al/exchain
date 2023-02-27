from test_case_base import WrappedTest
import subprocess
import shutil
import os
import re
from typing import Optional, Tuple

DIR_PATH = os.path.dirname(os.path.realpath(__file__))

class HadoopNNBench(WrappedTest):
    def __init__(self):
        super().__init__(
            "hadoop_nnbench",
            "Lorg/apache/",
            "hadoop-dist/target/hadoop-3.3.4",
            ["./bin/hadoop", "jar", "./share/hadoop/mapreduce/hadoop-mapreduce-client-jobclient-3.3.4-tests.jar",
             "nnbench", "-operation", "create_write", "-baseDir", "/tmp/nnbench",
             "-maps", "12", "-reduces", "6", "-blockSize", "1", "-bytesToWrite", "0",
             "-numberOfFiles", "1000", "-replicationFactorPerFile", "3", "-readFileAfterOpen", "true"],
            "HADOOP_OPTS",
            is_benchmark=True,
            work_dir=os.path.join(DIR_PATH, "..", "hadoop_bench")
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call(
            "mvn package -Pdist -DskipTests  -Dmaven.javadoc.skip=true -DskipTests=true", shell=True, cwd=self.work_dir)

    def pre(self):
        shutil.rmtree("/tmp/nnbench", ignore_errors=True)

    def post(self, type: str, debug: bool, cmd: subprocess.Popen, iter: int):
        out, err = cmd.communicate()
        shutil.rmtree("/tmp/nnbench", ignore_errors=True)
        result = self.find_result(err.decode("utf-8"))
        with open(self.perf_result_path(type, iter), "w") as f:
            f.write(f"tps, {result}\n")


    def find_result(self, out: str):
        print(out)
        result = re.search(r"TPS: Create/Write/Close: (\d+\.?\d*)", out)
        throughput = float(result.group(1))
        return throughput

