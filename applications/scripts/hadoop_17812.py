from test_case_base import SingleCommandTest
import subprocess


class Hadoop(SingleCommandTest):

    def __init__(self):
        super().__init__(
            "hadoop-17812",
            "hadoop-aws-3.4.0-SNAPSHOT-test-jar-with-dependencies.jar",
            "hadoop-tools/hadoop-aws/target",
            "org.apache.hadoop.fs.s3a.TestS3AInputStreamRetry",
            "Lorg/apache/"
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        args = ["mvn", "install", "-DskipTests"]
        subprocess.call(args, cwd=self.work_dir)