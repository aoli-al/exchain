import click
import os
from glob import glob
import inspect
from benchmark import Benchmark
from commons import *
from typing import Dict


# Load all python3 files in the current directory

BENCHMARK_APPLICATIONS: Dict[str, Benchmark] = {}

for file in glob(os.path.join(os.path.dirname(os.path.abspath(__file__)), "*.py")):
    name = os.path.splitext(os.path.basename(file))[0]
    # add package prefix to name, if required
    module = __import__(name)
    for _, obj in inspect.getmembers(module):
        if isinstance(obj, type) and issubclass(obj, Benchmark) and obj != Benchmark:
            BENCHMARK_APPLICATIONS[name] = obj


@click.group(name="app")
@click.pass_context
def application(ctx, app: str):
    ctx.obj = BENCHMARK_APPLICATIONS[app]()
    print("123")


@click.group(name="mode")
@click.argument("application", type=click.Choice([app for app in BENCHMARK_APPLICATIONS.keys()]))
# @click.argument("mode", type=click.Choice(["build", "instrument", "run"]))
@click.pass_context
def main(ctx, application: str):
    ctx.obj = BENCHMARK_APPLICATIONS[application]()


@main.command(name="build")
@click.pass_obj
def build(app: Benchmark):
    app.build()


@main.command(name="instrument")
@click.pass_obj
def instrument(app: Benchmark):
    app.instrument()


@main.command(name="run")
@click.option('--type', type=click.Choice(DEFAULT_TYPES), default="origin", help='Type of run.')
@click.option('--debug/--no-debug', default=False, help='Enable debugging.')
@click.pass_obj
def run(app: Benchmark, type: str, debug: bool):
    app.run_test(type, debug)


@main.command(name="analyze")
@click.option('--type', type=click.Choice(["hybrid", "static"]), help='Type of analysis.')
@click.pass_obj
def analyze(app: Benchmark, type: str):
    app.post_analysis(type)


if __name__ == '__main__':
    main(None, None)
