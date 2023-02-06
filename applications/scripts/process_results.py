from typing import Dict, List, Tuple, Set
import jsonpickle
import json
import os
from commons import *
from objects import *

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

def process_dependency_result(result: List[Link], ground_truth: List[Link]) -> Tuple[int, int, int]:
    tp = 0
    fp = 0
    fn = 0
    for link in result:
        if link in ground_truth:
            tp += 1
        else:
            fp += 1
    for link in ground_truth:
        if link not in result:
            fn += 1
    return (tp, fp, fn)



def build_expected_dependencies():
    from runner import BENCHMARK_APPLICATIONS
    for name, cls in BENCHMARK_APPLICATIONS.items():
        print(f"\n\n=================== Start processing {name}")
        app = cls()
        work_dir = app.get_latest_result("dynamic")
        exception_data = read_exceptions(os.path.join(work_dir, "exception.json"))
        dependencies = read_dependencies(os.path.join(work_dir, "dynamic_dependency.json"))
        expected_dependency = set()
        for (cause, exec) in dependencies:
            e1 = exception_data[cause]
            e2 = exception_data[exec]
            src = Exception(e1["type"],
                            e1["message"] if 'message' in e1 else "")
            dst = Exception(e2["type"],
                            e2["message"] if 'message' in e2 else "")
            expected_dependency.add(Link(src, dst))
        data = jsonpickle.encode(list(expected_dependency), indent=2)
        open(app.ground_truth_path, "w").write(data)





if __name__ == "__main__":
    build_expected_dependencies()
