from test_case_base import SingleCommandTest
import subprocess
import time
import os
import requests


class Wicket(SingleCommandTest):
    def __init__(self):
        super().__init__(
            "wicket-6908",
            "myproject-1.0-SNAPSHOT-test-jar-with-dependencies.jar",
            "target",
            "org.apache.wicket.testapplication.Start",
            "Lorg/apache/",
            is_async=True,
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call(["mvn", "install", "-DskipTests"], cwd=self.work_dir)

    def post(self, type: str, debug: bool, cmd: subprocess.Popen, iter: int, _: bool):
        time.sleep(10)
        if not debug:
            s = requests.Session()
            s2 = requests.Session()
            s2.get("http://localhost:8080/?0")
            print(s2.get("http://localhost:8080/?0-1.-test3").text)
            s.get("http://localhost:8080")
            s.get("http://localhost:8080/?0-1.-test")
            s.get("http://localhost:8080/wicket/page?1-999.-btn")
            s2.get("http://localhost:8080/?1")
            print(s2.get("http://localhost:8080/?1-1.-test2").text)


            s.get("http://localhost:8080/wicket/page?1-999.-btn")

            s.close()
            s2.close()
            time.sleep(1)

            cmd.kill()
        else:
            cmd.communicate()

