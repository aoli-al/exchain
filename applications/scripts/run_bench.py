import click
from runner import BENCHMARK_APPLICATIONS
from typing import List
from commons import *
import subprocess



def prepare_env():
    subprocess.call()


@click.command()
@click.option("--build/--no-build", default=True, help="Build the project")
@click.option("--instrument/--no-instrument", default=True, help="Instrument the project")
@click.option("--run/--no-run", default=True, help="Run the project")
@click.option("--analyze/--no-analyze", default=True, help="Run the project")
@click.option("--skip-app", multiple=True, help="Skip specific appliaction")
@click.option("--skip-type", multiple=True, help="Skip specific type")
def bench(build: bool, instrument: bool, run: bool, analyze: bool,
          skip_app: List[str], skip_type: List[str]):
    for name, cls in BENCHMARK_APPLICATIONS.items():
        if name in skip_app:
            continue
        app = cls()
        if build:
            app.build()
        if instrument:
            app.instrument()
        for t in DEFAULT_TYPES:
            if t not in skip_type:
                if run:
                    app.run_test(t, False)
                if analyze:
                    app.post_analysis(t)

if __name__ == "__main__":
    bench()