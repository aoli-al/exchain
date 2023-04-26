from test_case_base import SingleCommandTest
import subprocess
import shutil
import os


class NIFI(SingleCommandTest):

    def __init__(self):
        super().__init__(
            "nifi-8249",
            "nifi-standard-processors-1.20.0-SNAPSHOT-test-jar-with-dependencies.jar",
            "nifi-nar-bundles/nifi-standard-bundle/nifi-standard-processors/target",
            "org.apache.nifi.processors.standard.TestExecuteSQLRecord",
            "Lorg/apache/",
            additional_args=["-noverify"],
            is_async=True,
            is_implicit=False
        )

    # def pre(self):
    #     shutil.rmtree(os.path.join(self.work_dir, "target/db"), ignore_errors=True)

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        args = ["mvn", "install", "-DskipTests"]
        subprocess.call(args, cwd=self.work_dir)