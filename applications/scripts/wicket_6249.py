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
            "Lorg/apache/",
            is_async=True,
            ignored_type=[
                "org.apache.wicket.Component/internalRenderComponent:2589",
                "org.apache.wicket.Page/checkRendering:666",
                ""
            ]
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call(["mvn", "install", "-DskipTests"], cwd=self.work_dir)

    def post(self, type: str, debug: bool, cmd: subprocess.Popen):
        time.sleep(10)
        if not debug:
            requests.get("http://127.0.0.1:8080")
            requests.get("http://127.0.0.1:8080/wicket/page?1")
            requests.get("http://localhost:8080/wicket/page?1-999.-btn")
            requests.get("http://localhost:8080/wicket/page?1-3.ILinkListener-test2")
            requests.get("http://127.0.0.1:8080")
            cmd.kill()
        else:
            cmd.communicate()

