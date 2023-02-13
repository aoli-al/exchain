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

def data_to_message(data: Dict) -> Exception:
    return Exception(
        data["type"],
        data["stack"][0] if "stack" in data and data["stack"] else "",
        data["message"] if "message" in data else "",
    )

def read_static_dependencies(path: str, exceptions: Dict[int, Dict]) -> Set[Link]:
    dependencies = set()
    if not os.path.exists(path):
        return dependencies
    data = json.load(open(path))
    dependency = set()
    for dst, srcs in data["exceptionGraph"].items():
        e1 = exceptions[int(dst)]
        dst = data_to_message(e1)
        for src in srcs:
            e2 = exceptions[src["first"]]
            src= data_to_message(e2)
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
        src = data_to_message(e1)
        dst = data_to_message(e2)
        dependency.add(Link(src, dst))
    return dependency

def process_dependency_result(result: List[Link], ground_truth: List[Tuple[Link, LinkType]]) -> Tuple[int, int, int]:
    tp = 0
    fp = 0
    fn = 0
    for link in result:
        identified = False
        for expected in ground_truth:
            if link == expected[0]:
                identified = True
                if expected[1] != LinkType.IGNORE:
                    tp += 1
        if not identified:
            fp += 1
    for link in ground_truth:
        if link[1] == LinkType.IGNORE:
            continue
        if link[0] not in result:
            fn += 1
    return (tp, fp, fn)

def check_root_cause_in_log(result: List[Tuple[Link, LinkType]], log_path: str) -> bool:
    with open(log_path) as f:
        log_data = f.read()
        for link in result:
            if link[0].src.type not in log_data:
                return False
            if link[0].src.message not in log_data:
                return False
    return True

def get_exception_distance(result: List[Tuple[Link, LinkType]], path: str) -> int:
    exceptions = []
    with open(path) as f:
        for line in f:
            exceptions.append(data_to_message(json.loads(line)))
    max_distance = 0
    for item in result:
        link = item[0]
        distance = 0
        src_found = False
        for exception in exceptions:
            if link.dst == exception:
                max_distance = max(distance, max_distance)
                break
            if src_found:
                distance += 1
            if link.src == exception:
                src_found = True
    return max_distance








def build_expected_dependencies():
    from runner import BENCHMARK_APPLICATIONS
    for name, cls in BENCHMARK_APPLICATIONS.items():
        print(f"\n\n=================== Start processing {name}")
        app = cls()
        expected_dependency= app.read_latest_dynamic_dependency()
        result = []
        for dependency in expected_dependency:
            type = LinkType.KEY
            if "phosphor" in dependency.src.message or "phosphor" in dependency.dst.message:
                type = LinkType.IGNORE
            result.append((dependency, type))

        data = jsonpickle.encode(result, indent=2)
        open(app.ground_truth_path, "w").write(data)





if __name__ == "__main__":
    build_expected_dependencies()
