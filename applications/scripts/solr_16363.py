from benchmark import SingleCommandTest
import subprocess
import time
import os


class Solr(SingleCommandTest):

    def __init__(self):
        super().__init__(
            "solr-16363",
            "",
            "solr/benchmark/build/libs/",
            "org.apache.solr.bench.index.CloudIndexing",
            "Lorg/apache/solr",
            False,
            is_async=True,
            ignored_type= [
            "org.apache.solr.cloud.ZkTestServer$ZKServerMain/getLocalPort:398",
            "org.apache.zookeeper.KeeperException/create:118"
            ])

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call(["./gradlew", ":solr:build", "-Pvalidation.git.failOnModified=false"], cwd=self.work_dir)
        subprocess.call(["./gradlew", ":solr:benchmark:jar"], cwd=self.work_dir)
        subprocess.call(["./gradlew", ":solr:benchmark:copyDependencies"], cwd=self.work_dir)