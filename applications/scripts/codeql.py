
import click
from runner import *
from typing import List
from commons import *
import subprocess

@click.command()
def codeql():
    for name, app in TEST_APPLICATIONS.items():
        app.analyze()

if __name__ == "__main__":
    codeql()
