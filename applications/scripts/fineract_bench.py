from test_case_base import SingleCommandTest
import subprocess
import time
import os

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

    def post(self, type: str, debug: bool, cmd: subprocess.Popen):
        if type == "dynamic":
            time.sleep(300)
        else:
            time.sleep(60)
        if not debug:
            subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
            subprocess.call("./gradlew --rerun-tasks integrationTest",
                            env={
                                "PERF_OUT_FILE": self.perf_result_path(type),
                                ** os.environ,
                            },
                            cwd=self.work_dir, shell=True)
            cmd.kill()
        else:
            cmd.communicate()