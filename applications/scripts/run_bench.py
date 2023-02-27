import click
from runner import *
from typing import List
from commons import *
import subprocess



@click.command()
@click.argument("dataset", type=click.Choice(["all", "bench", "test"]))
@click.option("--build/--no-build", default=True, help="Build the project")
@click.option("--instrument/--no-instrument", default=True, help="Instrument the project")
@click.option("--run/--no-run", default=True, help="Run the project")
@click.option("--analyze/--no-analyze", default=True, help="Run the project")
@click.option("--skip-app", multiple=True, help="Skip specific appliaction")
@click.option("--skip-type", multiple=True, help="Skip specific type")
@click.option('--iter', type=int, default=1)
def bench(dataset: str, build: bool, instrument: bool, run: bool, analyze: bool,
          skip_app: List[str], skip_type: List[str], iter: int):
    if dataset == "all":
        data = ALL_APPLICATIONS.items()
    elif dataset == "bench":
        data = BENCH_APPLICATIONS.items()
    else:
        data = TEST_APPLICATIONS.items()

    for name, app in data:
        print(f"Start processing {name}")
        if name in skip_app:
            continue
        if build:
            app.build()
        if instrument:
            app.instrument()
        for t in DEFAULT_TYPES:
            if t not in skip_type:
                if run:
                    for i in range(iter):
                        app.run_test(t, False, i)
                if analyze:
                    app.post_analysis(t)

if __name__ == "__main__":
    bench()