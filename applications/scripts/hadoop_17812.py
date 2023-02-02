from benchmark import Benchmark
import subprocess


class Hadoop(Benchmark):

    def __init__(self):
        super().__init__(
            "hadoop-17812",
            "hadoop-aws-3.4.0-SNAPSHOT-test-jar-with-dependencies.jar",
            "hadoop-tools/hadoop-aws/target",
            "org.apache.hadoop.fs.s3a.TestS3AInputStreamRetry",
            "Lorg/apache/hadoop/hdfs"
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        args = ["mvn", "install", "-DskipTests"]
        subprocess.call(args, cwd=self.work_dir)