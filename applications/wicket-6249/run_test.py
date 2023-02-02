#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import subprocess
import sys
import click
import requests
import time
from typing import List
DIR = os.path.dirname(os.path.realpath(__file__))
sys.path.append(os.path.join(DIR, ".."))
from commons import *

JAR_NAME = "myproject-1.0-SNAPSHOT-test-jar-with-dependencies.jar"
ORIGIN_JAR_PATH = "target"
TEST_CLASS = "org.apache.wicket.testapplication.Start"
APPLICATION_NAMESPACE = "Lorg/apache/wicket"
@click.group(name="mode")
def main():
    pass


@main.command(name="build")
def build():
    subprocess.call("jenv local 11", shell=True)
    subprocess.call(["mvn", "install", "-DskipTests"], cwd=DIR)


def post():
    time.sleep(10)
    requests.get("http://127.0.0.1:8080")
    requests.get("http://127.0.0.1:8080")
    

@main.command(name="instrument")
def instrument():
    subprocess.call("jenv local 16", shell=True)
    subprocess.call(["java",
                     f"-DPhosphor.INSTRUMENTATION_CLASSPATH={INSTRUMENTATION_CLASSPATH}",
                     f"-DPhosphor.ORIGIN_CLASSPATH={ORIGIN_CLASSPATH}",
                     "-cp", PHOSPHOR_JAR_PATH, "edu.columbia.cs.psl.phosphor.Instrumenter",
                     f"{ORIGIN_JAR_PATH}/{JAR_NAME}", INSTRUMENTATION_FOLDER_NAME,
                     "-taintTagFactory", "al.aoli.exchain.phosphor.instrumenter.DynamicSwitchTaintTagFactory"], cwd=DIR)
    subprocess.call(["java",
                     f"-DPhosphor.INSTRUMENTATION_CLASSPATH={HYBRID_CLASSPATH}",
                     "-cp", PHOSPHOR_JAR_PATH, "edu.columbia.cs.psl.phosphor.Instrumenter",
                     f"{ORIGIN_JAR_PATH}/{JAR_NAME}", HYBRID_FOLDER_NAME,
                     "-taintTagFactory", "al.aoli.exchain.phosphor.instrumenter.FieldOnlyTaintTagFactory",
                     "-postClassVisitor", "al.aoli.exchain.phosphor.instrumenter.UninstrumentedOriginPostCV"
                     ], cwd=DIR)

def run_command(cmd: List[str]):
    return subprocess.Popen(cmd)


@main.command(name="origin")
@click.option('--debug', default=False, help='Enable debugging.')
def origin(debug: bool):
    command = ["-cp", f"{ORIGIN_JAR_PATH}/{JAR_NAME}", TEST_CLASS]
    if debug:
        command.insert(0, "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")
    cmd = run_command(["java"] + command)
    post()
    cmd.kill()

@main.command(name="static")
def static():
    args = ["java",
            "-cp",
            f"{ORIGIN_JAR_PATH}/{JAR_NAME}",
            f"-javaagent:{RUNTIME_JAR_PATH}=static:{INSTRUMENTATION_CLASSPATH}",
            f"-agentpath:{NATIVE_LIB_PATH}=exchain:L{APPLICATION_NAMESPACE}",
            TEST_CLASS]
    cmd = run_command(args)
    post()
    cmd.kill()
    args = ["./gradlew", "static-analyzer:run", f"--args={ORIGIN_CLASSPATH} {DIR}/static-results {ORIGIN_CLASSPATH}"]
    subprocess.call(args, cwd=os.path.join(DIR, "../.."))


@main.command(name="hybrid")
@click.option('--debug', is_flag=True, default=False, help='Enable debugging.')
def hybrid(debug: bool):
    cmd = [HYBRID_JAVA_EXEC,
           "-DPhosphor.DEBUG=true",
           f"-DPhosphor.INSTRUMENTATION_CLASSPATH={HYBRID_CLASSPATH}",
           f"-DPhosphor.ORIGIN_CLASSPATH={ORIGIN_CLASSPATH}",
           "-cp", f"{HYBRID_FOLDER_NAME}/{JAR_NAME}",
           f"-javaagent:{PHOSPHOR_AGENT_PATH}=taintTagFactory=al.aoli.exchain.phosphor.instrumenter.FieldOnlyTaintTagFactory,postClassVisitor=al.aoli.exchain.phosphor.instrumenter.UninstrumentedOriginPostCV",
           f"-javaagent:{RUNTIME_JAR_PATH}=hybrid:{HYBRID_CLASSPATH}",
           f"-agentpath:{NATIVE_LIB_PATH}=exchain:Lorg/apache/hadoop/fs",
           TEST_CLASS]
    if debug:
        cmd.insert(
            1, "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")
    cmd = run_command(cmd)
    post()
    cmd.kill()

@main.command(name="dynamic")
@click.option('--debug', is_flag=True, default=False, help='Enable debugging.')
def dynamic(debug: bool):
    cmd = [INSTRUMENTED_JAVA_EXEC,
           f"-DPhosphor.INSTRUMENTATION_CLASSPATH={INSTRUMENTATION_CLASSPATH}",
           f"-DPhosphor.ORIGIN_CLASSPATH={ORIGIN_CLASSPATH}",
           "-cp", f"{INSTRUMENTATION_FOLDER_NAME}/{JAR_NAME}",
           f"-javaagent:{PHOSPHOR_AGENT_PATH}",
           f"-javaagent:{RUNTIME_JAR_PATH}=dynamic:{INSTRUMENTATION_CLASSPATH}",
           f"-agentpath:{NATIVE_LIB_PATH}=exchain:{APPLICATION_NAMESPACE}",
           TEST_CLASS]
    if debug:
        cmd.insert(1, "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")
    cmd = run_command(cmd)
    post()
    cmd.kill()


if __name__ == '__main__':
    main()
