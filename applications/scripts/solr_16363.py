from benchmark import Benchmark
import subprocess
import time
import os


class Solr(Benchmark):

    def __init__(self):
        super().__init__(
            "solr-16363",
            "",
            "solr/benchmark/build/libs/",
            "org.apache.solr.bench.index.CloudIndexing",
            "Lorg/apache/solr",
            False,
            is_async=True)

    def build(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call(["./gradlew", ":solr:build", "-Pvalidation.git.failOnModified=false"], cwd=self.work_dir)
        subprocess.call(["./gradlew", ":solr:benchmark:copyDependencies"], cwd=self.work_dir)