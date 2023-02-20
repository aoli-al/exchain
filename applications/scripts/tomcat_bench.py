from test_case_base import WrappedTest
import subprocess
import shutil
import time
import os
import re
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

    def post(self, type: str, debug: bool, cmd: subprocess.Popen):
        time.sleep(10)
        cmd = subprocess.Popen("ab -c 10 -n 100000 http://localhost:8080/", shell=True,
                               stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        out, err = cmd.communicate()
        result = re.search(
            r"Time per request:\s+(\d+\.?\d*) \[ms\] \(mean\)", out.decode("utf-8"))
        tpr = float(result.group(1))
        cmd.kill()
        with open(self.perf_result_path(type), "w") as f:
            f.write(f"tpr, {tpr}\n")
