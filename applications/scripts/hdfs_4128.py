from benchmark import Benchmark
import subprocess

class HDFS(Benchmark):
    def __init__(self):
        super().__init__(
            "hdfs-4128",
            "hadoop-hdfs-3.4.0-SNAPSHOT-test-jar-with-dependencies.jar",
            "hadoop-hdfs-project/hadoop-hdfs/target",
            "org.apache.hadoop.hdfs.server.namenode.TestCheckpoint",
            "Lorg/apache/",
            is_async=True
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        args = ["mvn", "install", "-DskipTests"]
        subprocess.call(args, cwd=self.work_dir)