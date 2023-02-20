import click
import os
from glob import glob
import inspect
from test_case_base import *
from commons import *
from typing import Dict, Type


# Load all python3 files in the current directory

TEST_APPLICATIONS: Dict[str, Test] = {}
BENCH_APPLICATIONS: Dict[str, Test] = {}
ALL_APPLICATIONS: Dict[str, Test] = {}
FILE_PATH = os.path.dirname(os.path.abspath(__file__))

for file in glob.glob(os.path.join(FILE_PATH, "*.py")):
    name = os.path.splitext(os.path.basename(file))[0]
    # add package prefix to name, if required
    module = __import__(name)
    for _, obj in inspect.getmembers(module):
        if isinstance(obj, type) and issubclass(obj, Test) and obj != Test and obj != SingleCommandTest and obj != WrappedTest:
            app = obj()
            if app.is_benchmark:
                BENCH_APPLICATIONS[name] = app
            else:
                TEST_APPLICATIONS[name] = app
            ALL_APPLICATIONS[name] = app



@click.group(name="app")
@click.pass_context
def application(ctx, app: str):
    ctx.obj = ALL_APPLICATIONS[app]


@click.group(name="mode")
@click.argument("application", type=click.Choice([app for app in ALL_APPLICATIONS.keys()]))
# @click.argument("mode", type=click.Choice(["build", "instrument", "run"]))
@click.pass_context
def main(ctx, application: str):
    ctx.obj = ALL_APPLICATIONS[application]


@main.command(name="build")
@click.pass_obj
def build(app: SingleCommandTest):
    app.build()


@main.command(name="instrument")
@click.option('--debug/--no-debug', default=False, help='Enable debugging.')
@click.pass_obj
def instrument(app: SingleCommandTest, debug: bool):
    app.instrument(debug)


@main.command(name="run")
@click.option('--type', type=click.Choice(DEFAULT_TYPES), default="origin", help='Type of run.')
@click.option('--debug/--no-debug', default=False, help='Enable debugging.')
@click.pass_obj
def run(app: SingleCommandTest, type: str, debug: bool):
    app.run_test(type, debug)


@main.command(name="analyze")
@click.option('--type', type=click.Choice(["hybrid", "static"]), help='Type of analysis.')
@click.option('--debug/--no-debug', default=False, help='Enable debugging.')
@click.pass_obj
def analyze(app: SingleCommandTest, type: str, debug: bool):
    app.post_analysis(type, debug)


if __name__ == '__main__':
    main(None, None)
