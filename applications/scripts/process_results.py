from runner import BENCHMARK_APPLICATIONS
from typing import Dict, List, Tuple, Set
import json
import os
from commons import *

BASE_FOLDER = os.path.dirname(os.path.realpath(__file__))

def read_exceptions(path: str) -> Dict[int, Dict]:
    result = {}
    with open(path) as f:
        for line in f:
            data = json.loads(line)
            result[data['label']] = data
    return result

def read_dependencies(path: str) -> Set[Tuple[int, int]]:
    result = set()
    if not os.path.exists(path):
        return result
    with open(path) as f:
        for line in f:
            cause, exc = [int(x) for x in line.split(",")]
            result.add((cause, exc))
    return result

def build_expected_dependencies():
    for name, cls in BENCHMARK_APPLICATIONS.items():
        print(f"\n\n=================== Start processing {name}")
        app = cls()
        work_dir = app.get_latest_result("dynamic")
        exception_data = read_exceptions(os.path.join(work_dir, "exception.json"))
        dependencies = read_dependencies(os.path.join(work_dir, "dynamic_dependency.json"))
        expected_dependency = []
        for (cause, exec) in dependencies:
            e1 = exception_data[cause]
            e2 = exception_data[exec]
            src = {
                "type": e1["type"],
                "message": e1["message"] if 'message' in e1 else "",
            }

            dst = {
                "type": e2["type"],
                "message": e2["message"] if 'message' in e2 else "",
            }
            print("============src")
            print(src)
            print("============dst")
            print(dst)

            expected_dependency.append({
                "src": src,
                "dst": dst
            })
        json.dump(expected_dependency, open(os.path.join(BASE_FOLDER, "data", f"{app.test_name}.json"), "w"), indent=2)




if __name__ == "__main__":
    build_expected_dependencies()
