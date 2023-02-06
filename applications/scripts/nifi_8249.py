from benchmark import Benchmark
import subprocess


class NIFI(Benchmark):

    def __init__(self):
        super().__init__(
            "nifi-8249",
            "nifi-standard-processors-1.20.0-SNAPSHOT-test-jar-with-dependencies.jar",
            "nifi-nar-bundles/nifi-standard-bundle/nifi-standard-processors/target",
            "org.apache.nifi.processors.standard.TestExecuteSQLRecord",
            "Lorg/apache/",
            additional_args=["-noverify"]
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        args = ["mvn", "install", "-DskipTests"]
        subprocess.call(args, cwd=self.work_dir)