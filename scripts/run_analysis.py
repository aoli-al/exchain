import subprocess
import sys


def run(path: str):
    command = ["./doop", "-a", "context-insensitive"]
    with open(path) as f:
        data = f.read()
        for jar in data.split(":"):
            command.extend(["-i", jar.strip()])
    print(command)
    subprocess.call(command, cwd="/Users/aoli/repos/doop")

if __name__ == "__main__":
    run(sys.argv[1])
