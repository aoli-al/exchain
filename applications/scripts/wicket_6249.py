from benchmark import Benchmark
import subprocess
import time
import os
import requests


class Wicket(Benchmark):
    def __init__(self):
        super().__init__(
            "wicket-6249",
            "myproject-1.0-SNAPSHOT-test-jar-with-dependencies.jar",
            "target",
            "org.apache.wicket.testapplication.Start",
            "Lorg/apache/wicket"
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call(["mvn", "install", "-DskipTests"], cwd=self.work_dir)

    def post(self, type: str, cmd: subprocess.Popen):
        time.sleep(10)
        requests.get("http://127.0.0.1:8080")
        requests.get("http://127.0.0.1:8080")
        cmd.kill()
