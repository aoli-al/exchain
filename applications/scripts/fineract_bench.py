from test_case_base import SingleCommandTest
import subprocess
import time
import os
import re

from typing import *

class FineractBench(SingleCommandTest):

    def __init__(self):
        super().__init__(
            "fineract_bench",
            "fineract-provider.jar",
            "fineract-provider/build/libs",
            "org.springframework.boot.loader.JarLauncher",
            "Lorg/apache/fineract:Lorg/springframework/core:Lretrofit/client",
            additional_args=[
                "--add-opens=java.base/java.lang=ALL-UNNAMED",

            ],
            is_benchmark=True)

    def build(self):
        subprocess.call("jenv local 11", shell=True)
        args = ["./gradlew", "bootJar"]
        subprocess.call(args, cwd=self.work_dir)

    def pre(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call("docker rm -f mysql-5.7", shell=True)
        subprocess.call(
            "docker run --name mysql-5.7 -p 3306:3306 -e MYSQL_ROOT_PASSWORD=mysql -d mysql:5.7", shell=True)
        time.sleep(10)
        subprocess.call(
            "./gradlew createDB -PdbName=fineract_tenants", shell=True, cwd=self.work_dir)
        subprocess.call(
            "./gradlew createDB -PdbName=fineract_default", shell=True, cwd=self.work_dir)

    def get_exec_command(self, type: str, debug: bool) -> Tuple[List[str], Dict[str, str], str, Any]:
        cmd, env, work_dir, _ = super().get_exec_command(type, debug)
        return cmd, env, work_dir, open("/tmp/fineract.out", "w")

    def convert_measurement(self, input: float) -> float:
        return 1000 / input

    def post(self, type: str, debug: bool, cmd: subprocess.Popen, iter: int):
        if type == "dynamic":
            time.sleep(300)
        else:
            time.sleep(60)
        if not debug:
            subprocess.call(
                "ab -p test.json -T application/json -H Fineract-Platform-TenantId:default -c 200 -n 10000 https://localhost:8443/fineract-provider/api/v1/authentication", shell=True)
            measure = subprocess.Popen(
                "ab -p test.json -T application/json -H Fineract-Platform-TenantId:default -c 200 -n 10000 https://localhost:8443/fineract-provider/api/v1/authentication", shell=True,
                stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            out, err = measure.communicate()
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
        else:
            cmd.communicate()
