from test_case_base import SingleCommandTest
import subprocess
import time
import os
import sys
from typing import *


class Fineract(SingleCommandTest):

    def __init__(self):
        super().__init__(
            "fineract-1211",
            "fineract-provider.jar",
            "fineract-provider/build/libs",
            "org.springframework.boot.loader.JarLauncher",
            "Lorg/apache/fineract")

    def build(self):
        subprocess.call("jenv local 11", shell=True)
        args = ["./gradlew", "bootJar"]
        subprocess.call(args, cwd=self.work_dir)

    def pre(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call("docker stop mysql-5.7", shell=True)
        subprocess.call(
            "docker run --rm --name mysql-5.7 -p 3306:3306 -e MYSQL_ROOT_PASSWORD=mysql -d mysql:5.7", shell=True)
        time.sleep(10)
        subprocess.call(
            "./gradlew createDB -PdbName=fineract_tenants", shell=True, cwd=self.work_dir)
        subprocess.call(
            "./gradlew createDB -PdbName=fineract_default", shell=True, cwd=self.work_dir)

    def get_exec_command(self, type: str, debug: bool) -> Tuple[List[str], Dict[str, str], str, Any]:
        cmd, env, work_dir, _ = super().get_exec_command(type, debug)
        return cmd, env, work_dir, sys.stdout.buffer


    def post(self, type: str, debug: bool, cmd: subprocess.Popen, iter: int):
        if type == "dynamic":
            time.sleep(300)
        else:
            time.sleep(60)
        if not debug:
            print(self.work_dir)
            subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
            subprocess.call("./gradlew :fineract-provider:triggerBug --tests org.apache.fineract.integrationtests.HookIntegrationTest.shouldSendOfficeCreationNotification",
                            env={"PERF_OUT_FILE": "/tmp/out",
                                **os.environ
                                },
                            cwd=self.work_dir, shell=True)
            cmd.kill()
        else:
            cmd.communicate()
