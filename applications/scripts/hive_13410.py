from benchmark import Benchmark
import subprocess
import time
import os

ARGS = ["-noverify",
        "-Dderby.version=10.14.2.0",
        "-Dhive.version=4.0.0-SNAPSHOT",
        "-Dmapred.job.tracker=local",
        "-Dhive.test.console.log.level=INFO",
        "-Dlog4j.debug=true",
        "-Djava.net.preferIPv4Stack=true",
        "-Dtest.src.tables",
        "-Dhadoop.version=3.3.1",
        "-Xmx2048m",
        "-DJETTY_AVAILABLE_PROCESSORS=4",
        "--add-opens", "java.base/java.net=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.nio=ALL-UNNAMED"]


class Hive(Benchmark):

    def __init__(self):
        super().__init__(
            "hive-13410",
            "hive-it-unit-4.0.0-SNAPSHOT-tests.jar",
            "itests/hive-unit/target/lib",
            "org.apache.hive.jdbc.miniHS2.TestHs2Metrics",
            "Lorg/apache/hadoop:Lorg/apache/hive",
            False,
            ARGS,
            ["itests/hive-unit/target/testconf", "conf"],
            is_async=True)

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        args = ["mvn", "install", "-DskipTests",
                "-Dmaven.javadoc.skip=true", "-Drat.skip=true"]
        subprocess.call(args, cwd=self.work_dir)
        subprocess.call(["mvn", "dependency:copy-dependencies", "-DoutputDirectory=target/lib"],
                        cwd=os.path.join(self.work_dir, "itests", "hive-unit"))
