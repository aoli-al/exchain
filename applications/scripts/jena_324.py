from benchmark import Benchmark
import subprocess
import time
import os
import requests


class Jena(Benchmark):
    def __init__(self):
        super().__init__(
            "jena-324",
            "jena-tdb-0.9.4-SNAPSHOT-test-jar-with-dependencies.jar",
            "jena-tdb/target",
            "com.hp.hpl.jena.tdb.extra.T_TDBWriteTransaction",
            "Lcom/hp/hpl"
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call(["mvn", "install", "-DskipTests"], cwd=self.work_dir + "/jena-tdb")

