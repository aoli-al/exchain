from benchmark import Benchmark
import subprocess
import time
import os


class Fineract(Benchmark):

    def __init__(self):
        super().__init__(
            "fineract-1211",
            "fineract-provider.jar",
            "fineract-provider/build/libs",
            "org.springframework.boot.loader.JarLauncher",
            "Lorg/apache/fineract:Lorg/springframework/core")

    def build(self):
        subprocess.call("jenv local 11", shell=True)
        args = ["./gradlew", "bootJar"]
        subprocess.call(args, cwd=self.work_dir)

    def pre(self):
        subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
        subprocess.call("docker rm -f mysql-5.7", shell=True)
        subprocess.call(
            "docker run --name mysql-5.7 -p 3306:3306 -e MYSQL_ROOT_PASSWORD=mysql -d mysql:5.7", shell=True)
        time.sleep(10)
        subprocess.call(
            "./gradlew createDB -PdbName=fineract_tenants", shell=True, cwd=self.work_dir)
        subprocess.call(
            "./gradlew createDB -PdbName=fineract_default", shell=True, cwd=self.work_dir)

    def post(self, type: str, debug: bool, cmd: subprocess.Popen):
        if type == "dynamic":
            time.sleep(300)
        else:
            time.sleep(60)
        if not debug:
            print(self.work_dir)
            subprocess.call("jenv local 11", shell=True, cwd=self.work_dir)
            subprocess.call("./gradlew :fineract-provider:triggerBug --tests org.apache.fineract.integrationtests.HookIntegrationTest.shouldSendOfficeCreationNotification",
                            env={"PERF_OUT_FILE": "/tmp/out",
                                **os.environ
                                },
                            cwd=self.work_dir, shell=True)
            cmd.kill()
        else:
            cmd.communicate()
