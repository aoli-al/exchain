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

def read_static_dependencies(path: str, exceptions: Dict[int, Dict]) -> Set[Link]:
    dependencies = set()
    if not os.path.exists(path):
        return dependencies
    print(path)
    data = json.load(open(path))
    dependency = set()
    for src, dsts in data["exceptionGraph"].items():
        e1 = exceptions[int(src)]
        src = Exception(e1["type"],
                        e1["message"] if 'message' in e1 else "")
        for dst in dsts:
            e2 = exceptions[dst["first"]]
            dst = Exception(e2["type"],
                            e2["message"] if 'message' in e2 else "")
            dependency.add(Link(src, dst))
    return dependency



def read_dynamic_dependencies(path: str, exceptions: Dict[int, Dict]) -> Set[Link]:
    dependencies = set()
    if not os.path.exists(path):
        return dependencies
    with open(path) as f:
        for line in f:
            cause, exc = [int(x) for x in line.split(",")]
            dependencies.add((cause, exc))
    dependency = set()
    for (cause, exec) in dependencies:
        e1 = exceptions[cause]
        e2 = exceptions[exec]
        src = Exception(e1["type"],
                        e1["message"] if 'message' in e1 else "")
        dst = Exception(e2["type"],
                        e2["message"] if 'message' in e2 else "")
        dependency.add(Link(src, dst))
    return dependency

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

def check_root_cause_in_log(result: List[Link], log_path: str) -> bool:
    with open(log_path) as f:
        log_data = f.read()
        for link in result:
            if link.src.type not in log_data:
                return False
            if link.src.message not in log_data:
                return False
    return True



def build_expected_dependencies():
    from runner import BENCHMARK_APPLICATIONS
    for name, cls in BENCHMARK_APPLICATIONS.items():
        print(f"\n\n=================== Start processing {name}")
        app = cls()
        expected_dependency= app.read_latest_dynamic_dependency()
        data = jsonpickle.encode(list(expected_dependency), indent=2)
        open(app.ground_truth_path, "w").write(data)





if __name__ == "__main__":
    build_expected_dependencies()
