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
@click.option("--only-app", multiple=True, help="Only run specifc application",
              default=ALL_APPLICATIONS.keys())
@click.option('--iter', type=int, default=1)
@click.option("--naive/--no-naitve", default=True, help="Build the project")
def bench(dataset: str, build: bool, instrument: bool, run: bool, analyze: bool,
          skip_app: List[str], skip_type: List[str], only_app: List[str], iter: int,
          naive: bool):
    if dataset == "all":
        data = ALL_APPLICATIONS.items()
    elif dataset == "bench":
        data = BENCH_APPLICATIONS.items()
    else:
        data = TEST_APPLICATIONS.items()
    print(only_app)

    for name, app in data:
        print(f"Start processing {name}")
        if name in skip_app or name not in only_app:
            continue
        if build:
            app.build()
        if instrument:
            app.instrument()
        for t in DEFAULT_TYPES:
            if t not in skip_type:
                if run:
                    app.run_test(t, False, iter)
                if analyze:
                    app.post_analysis(t, naive=naive)

if __name__ == "__main__":
    bench()