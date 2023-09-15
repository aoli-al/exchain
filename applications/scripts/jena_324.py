from test_case_base import SingleCommandTest
import subprocess
import time
import os
import requests


class Jena(SingleCommandTest):
    def __init__(self):
        super().__init__(
            "jena-324",
            "jena-tdb-0.9.4-SNAPSHOT-test-jar-with-dependencies.jar",
            "jena-tdb/target",
            "com.hp.hpl.jena.tdb.extra.T_TDBWriteTransaction",
            "Lcom/hp/hpl:Lorg/apache/jena",
            is_async=True
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call(["mvn", "install", "-DskipTests"], cwd=self.work_dir + "/jena-tdb")

    def post(self, type: str, debug: bool, cmd: subprocess.Popen, iter: int, _: bool):
        print("POST!")
        if type == "dynamic":
            time.sleep(40)
        else:
            time.sleep(20)
        if not debug:
            cmd.kill()
        else:
            cmd.communicate()
