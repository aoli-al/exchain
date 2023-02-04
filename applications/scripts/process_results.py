from runner import BENCHMARK_APPLICATIONS
from typing import Dict, List, Tuple
import json
import os
from commons import *

def read_exceptions(path: str) -> Dict[int, Dict]:
    result = {}
    with open(path) as f:
        for line in f:
            data = json.loads(line)
            result[data['label']] = data
    return result

def read_dependencies(path: str) -> List[Tuple[int, int]]:
    result = []
    with open(path) as f:
        for line in f:
            cause, exc = [int(x) for x in line.split(",")]
            print(cause, exc)

def analyze_dynamic_results():
    for name, cls in BENCHMARK_APPLICATIONS.items():
        print(f"Start processing {name}")
        app = cls()
        type = "dynamic"
        base_dir = os.path.join(app.out_path, f"{type}-results")
        latest = open(os.path.join(base_dir, "latest")).read().strip()
        work_dir = os.path.join(base_dir, latest)
        exception_data = read_exceptions(os.path.join(work_dir, "exception.json"))
        read_dependencies(os.path.join(work_dir, "dynamic_dependency.json"))




if __name__ == "__main__":
    analyze_dynamic_results()
