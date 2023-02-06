from benchmark import Benchmark
import subprocess


class MapReduce(Benchmark):
    def __init__(self):
        super().__init__(
            "mapreduce-6654",
            "hadoop-mapreduce-client-app-3.4.0-SNAPSHOT-test-jar-with-dependencies.jar",
            "hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/target",
            "org.apache.hadoop.mapreduce.v2.app.TestMRAppMaster",
            "Lorg/apache/"
        )

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        args = ["mvn", "install", "-DskipTests"]
        subprocess.call(args, cwd=self.work_dir)