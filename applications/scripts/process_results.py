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

import re

def read_perf_result(path: str):
    with open(path) as f:
        result = {}
        for line in f:
            [url, time] = line.split(", ")
            re_result = re.search(r"^(([^:/?#]+):)?(//([^/?#]*))?(([^?#]*)(\?([^#]*))?(#(.*))?)", url)
            url = re_result.group(5)
            if url not in result:
                result[url] = []
            result[url].append(int(time))
    return result


def read_aggregate_perf_result_file(path):
    items = {}
    with open(path) as f:
        for line in f:
            [name, data] = line.split(", ")
            items[name] = float(data)
    return items

def read_separate_perf_result(sources: List[str]):
    result = [0]
    origin_result = None
    for source in sources:
        diff = []
        data = read_perf_result(source)
        if origin_result is None:
            origin_result = data
        else:
            for key in origin_result:
                if key not in data:
                    continue
                origin_avg = sum(origin_result[key]) / len(origin_result[key])
                data_avg = sum(data[key]) / len(data[key])
                diff.append((data_avg - origin_avg) / origin_avg)
            result.append(sum(diff) / len(diff))
    return result


def read_aggregate_perf_result(sources: List[str]):
    result = {}
    for source in sources:
        data = read_aggregate_perf_result_file(source)
        for name, value in data.items():
            if name not in result:
                result[name] = [value]
            else:
                diff = (value - result[name][0]) / result[name][0]
                result[name].append((value, diff))
    return result



def build_expected_dependencies():
    from runner import TEST_APPLICATIONS
    for name, app in TEST_APPLICATIONS.items():
        if app.is_benchmark:
            continue
        print(f"\n\n=================== Start processing {name}")
        expected_dependency= app.read_latest_dynamic_dependency()
        result = []
        for dependency in expected_dependency:
            type = LinkType.KEY
            if "phosphor" in dependency.src.message or "phosphor" in dependency.dst.message:
                type = LinkType.IGNORE
            if dependency.src.method in app.ignored_type:
                type = LinkType.IGNORE
            if dependency.dst.method in app.ignored_type:
                type = LinkType.IGNORE
            result.append((dependency, type))

        data = jsonpickle.encode(result, indent=2)
        open(app.ground_truth_path, "w").write(data)





if __name__ == "__main__":
    build_expected_dependencies()
