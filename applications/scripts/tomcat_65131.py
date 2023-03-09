from test_case_base import WrappedTest
import subprocess
import shutil
import time
import os
from commons import *


class Tomcat(WrappedTest):

    def __init__(self):
        super().__init__(
            "tomcat-65131",
            "Lorg/apache/tomcat",
            "output/build",
            ["bin/catalina.sh", "run"],
            "JAVA_OPTS",
            test_class="org.apache.catalina.startup.Bootstrap"
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call("ant", cwd=self.work_dir, shell=True)

    def post(self, type: str, debug: bool, cmd: subprocess.Popen, iter: int):
        time.sleep(10)
        subprocess.call("curl -q -k https://localhost:8443", shell=True)
        cmd.kill()
