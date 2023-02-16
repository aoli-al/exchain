from test_case_base import WrappedTest
import subprocess
import shutil
import time
import os
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
        self.src = os.path.join(self.work_dir, "output", "build")
        self.inst_dst = os.path.join(self.work_dir, "output", "instrumented")
        self.hybrid_dst = os.path.join(self.work_dir, "output", "hybrid")

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call("ant", cwd=self.work_dir, shell=True)

    def post(self, type: str, debug: bool, cmd: subprocess.Popen):
        time.sleep(10)
        subprocess.call("ab -c 10 -n 100000 http://localhost:8080/", shell=True)
        cmd.kill()
