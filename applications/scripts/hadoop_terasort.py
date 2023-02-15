from test_case_base import WrappedTest
import subprocess
import shutil
import os

DIR_PATH = os.path.dirname(os.path.realpath(__file__))

class HadoopTerasort(WrappedTest):
    def __init__(self):
        super().__init__(
            "hadoo_terasort",
            "Lorg/apache/",
            "hadoop-dist/target/hadoop-3.3.4",
            ["./bin/hadoop", "jar", "./share/hadoop/mapreduce/hadoop-mapreduce-examples-3.3.4.jar",
                "terasort", "/tmp/workload-gen", "/tmp/workload-out"],
            "HADOOP_OPTS",
            is_benchmark=True,
            work_dir=os.path.join(DIR_PATH, "..", "hadoop_bench")
        )

    def build(self):
        subprocess.call(
            "mvn package -Pdist -DskipTests  -Dmaven.javadoc.skip=true -DskipTests=true", shell=True, cwd=self.work_dir)

    def pre(self):
        shutil.rmtree("/tmp/workload-gen", ignore_errors=True)
        shutil.rmtree("/tmp/workload-out", ignore_errors=True)
        print(self.origin_dist)

        subprocess.call(["./bin/hadoop jar ./share/hadoop/mapreduce/hadoop-mapreduce-examples-3.3.4.jar teragen 10000000 /tmp/workload-gen"],
                        cwd=self.origin_dist, shell=True,
                        env={
                            "JAVA_HOME": os.path.join(os.path.expanduser("~"), ".jenv", "versions", "11")
        })

