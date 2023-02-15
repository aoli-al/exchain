from test_case_base import WrappedTest
import subprocess
import shutil
import os

DIR_PATH = os.path.dirname(os.path.realpath(__file__))

class HadoopDFSWrite(WrappedTest):
    def __init__(self):
        super().__init__(
            "hadoop_dfs_write",
            "Lorg/apache/",
            "hadoop-dist/target/hadoop-3.3.4",
            ["./bin/hadoop", "jar", "./share/hadoop/mapreduce/hadoop-mapreduce-client-jobclient-3.3.4-tests.jar",
             "TestDFSIO", "-Dtest.build.data=/tmp/dfs-write", "-write", "-nrFiles", "32", "-fileSize", "1000"],
            "HADOOP_OPTS",
            is_benchmark=True,
            work_dir=os.path.join(DIR_PATH, "..", "hadoop_bench")
        )

    def build(self):
        subprocess.call(
            "mvn package -Pdist -DskipTests  -Dmaven.javadoc.skip=true -DskipTests=true", shell=True, cwd=self.work_dir)

    def pre(self):
        shutil.rmtree("/tmp/dfs-write", ignore_errors=True)

    def post(self, type: str, debug: bool, cmd: subprocess.Popen):
        super().post(type, debug, cmd)
        shutil.rmtree("/tmp/dfs-write", ignore_errors=True)

