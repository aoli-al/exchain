from test_case_base import WrappedTest
import subprocess
import shutil
import time
import os
import re
import signal
from commons import *


class Tomcat(WrappedTest):

    def __init__(self):
        super().__init__(
            "tomcat_bench",
            "Lorg/apache/tomcat",
            "output/build",
            ["bin/catalina.sh", "run"],
            "JAVA_OPTS",
            is_benchmark=True
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call("ant", cwd=self.work_dir, shell=True)

    def convert_measurement(self, input: float) -> float:
        return 1000 / input

    def post(self, type: str, debug: bool, cmd: subprocess.Popen, iter: int):
        time.sleep(10)
        measure = subprocess.call(
            "ab -k -c 200 -n 10000 http://localhost:8080/", shell=True)
        measure = subprocess.Popen("ab -k -c 200 -n 10000 http://localhost:8080/", shell=True,
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        out, err = measure.communicate()
        print(out.decode("utf-8"))
        result = re.search(
            r"Time per request:\s+(\d+\.?\d*) \[ms\] \(mean\)", out.decode("utf-8"))
        latency = float(result.group(1))
        result = re.search(
            r"Requests per second:\s+(\d+\.?\d*) \[#/sec\]", out.decode("utf-8"))
        throughput = float(result.group(1))
        with open(self.perf_result_path(type, iter), "w") as f:
            f.write(f"latency, {latency}\n")
            f.write(f"throughput, {throughput}\n")
        cmd.kill()