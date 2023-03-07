from test_case_base import SingleCommandTest
import subprocess
import time
import os
import requests
import re


class Wicket(SingleCommandTest):
    def __init__(self):
        super().__init__(
            "wicket_bench",
            "myproject-1.0-SNAPSHOT-test-jar-with-dependencies.jar",
            "target",
            "org.apache.wicket.myapplication.Start",
            "Lorg/apache/",
            is_benchmark=True
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call(["mvn", "install", "-DskipTests"], cwd=self.work_dir)

    def convert_measurement(self, input: float) -> float:
        return 1000 / input

    def post(self, type: str, debug: bool, cmd: subprocess.Popen, iter: int):
        time.sleep(10)
        measure = subprocess.Popen("ab -k -c 200 -n 10000 http://localhost:8080/", shell=True,
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        out, err = measure.communicate()
        result = re.search(
            r"Time per request:\s+(\d+\.?\d*) \[ms\] \(mean\)", out.decode("utf-8"))
        tpr = float(result.group(1))
        cmd.kill()
        with open(self.perf_result_path(type, iter), "w") as f:
            f.write(f"tpr, {tpr}\n")
