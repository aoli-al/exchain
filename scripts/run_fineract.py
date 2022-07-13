#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import subprocess
import shutil
from pathlib import Path

FINERACT_DIR = "/home/aoli/repos/fineract"
INSTRUMENTATION_DIR = "/home/aoli/repos/exception"
START_PATTERN = "Tomcat started on port(s): 8080 (http) 8443 (https) with context path '/fineract-provider'"
DEFAULT_ENV = {
    "JAVA_HOME": "/usr/lib/jvm/java-1.11.0-openjdk-amd64"
}

run_fineract = ["./gradlew", "bootRun"]
run_test = ["./gradlew", "--rerun-tasks", "integrationTest", "--tests",
            "org.apache.fineract.integrationtests."
            "AccountingScenarioIntegrationTest"
            ".checkUpfrontAccrualAccountingFlow"]
start_instrumentation = ["./gradlew", "startInstrumentation"]

def run_fault_injector():
    with subprocess.Popen(run_fineract,
                          cwd=FINERACT_DIR,
                          stdout=subprocess.PIPE,
                          stderr=subprocess.PIPE,
                          universal_newlines=True,
                          env=DEFAULT_ENV) as fineract:
        while True:
            data = fineract.stdout.readline()
            if START_PATTERN in data:
                break
            print(data)
        print("Fineract started!")
        subprocess.call(start_instrumentation, cwd=INSTRUMENTATION_DIR)
        print("Instrumentation enabled!")
        subprocess.call(run_test, cwd=FINERACT_DIR, env=DEFAULT_ENV)
        print("Integration test finished!")
        fineract.kill()

if __name__ == "__main__":
    for i in range(500):
        loc = f"data/injection_{i}"
        Path(loc).mkdir(parents=True, exist_ok=True)
        run_fault_injector()
        shutil.copy("/tmp/caught.txt", f"{loc}/caught.txt")
        shutil.copy("/tmp/injection.txt", f"{loc}/injection.txt")
