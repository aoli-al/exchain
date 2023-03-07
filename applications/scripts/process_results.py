from typing import Dict, List, Tuple, Set
import jsonpickle
import json
import os
from commons import *
from objects import *
from statsmodels.stats.diagnostic import lilliefors

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
    interesting_dst = set()
    for link in ground_truth:
        interesting_dst.add(link[0].dst)
    for link in set(result):
        identified = False
        should_identify = False
        if link.dst in interesting_dst:
            should_identify = True
        for expected in ground_truth:
            if link == expected[0]:
                identified = True
                tp += 1
        if not identified and should_identify:
            fp += 1
            print(link)
    for link in ground_truth:
        if link[0] not in result:
            fn += 1
    return (tp > 0, fp > 0)

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

def read_separate_perf_result(path: str):
    with open(path) as f:
        result = []
        for line in f:
            [url, time] = line.split(", ")
            re_result = re.search(r"^(([^:/?#]+):)?(//([^/?#]*))?(([^?#]*)(\?([^#]*))?(#(.*))?)", url)
            url = re_result.group(5)
            result.append(int(time))
    return {"tpr": sum(result) / len(result)}


def read_aggregate_perf_result(path):
    items = {}
    with open(path) as f:
        for line in f:
            [name, data] = line.split(", ")
            items[name] = float(data.strip())
            break
    return items


import pandas as pd
def read_perf_result(app):
    types = ["origin", "static", "hybrid", "dynamic"]
    perf_result = [app.test_name.split("_")[0].upper()]
    origin_result = 0
    for t in types:
        data = {}
        for i in range(10):
            path = app.perf_result_path(t, i)
            result = read_aggregate_perf_result(path)
            for key, value in  result.items():
                if key not in data:
                    data[key] = []
                data[key].append(app.convert_measurement(value))
        df = pd.DataFrame(data)
        for key in result.keys():
            mean = df[key].mean()
            perf_result.append(f"{mean:.1f}")
            perf_result.append(f"{df[key].std():.1f}")
            if t == "origin":
                origin_result = mean
            else:
                perf_result.append(
                    f"{(mean - origin_result) / origin_result * 100:.1f}" + "\%")
    return perf_result


def save_as_latex_table(data, path):
    with open(path, "w") as f:
        for row in data:
            formatted_result = []
            for item in row:
                if isinstance(item, bool):
                    if item:
                        formatted_result.append("\\cmark")
                    else:
                        formatted_result.append("\\xmark")
                else:
                    formatted_result.append(str(item))
            f.write(" & ".join(formatted_result))
            if row != data[-1]:
                f.write(" \\\\\n")






# def read_separate_perf_result(sources: List[str]):
#     result = []
#     origin_result = None
#     for source in sources:
#         diff = []
#         data = read_perf_result(source)
#         if origin_result is None:
#             origin_result = data
#             origin_avg = sum(origin_result) / len(origin_result)
#             result.append(map_data(origin_avg))
#         else:
#             data_avg = sum(data) / len(data)
#             result.append(map_data((data_avg, (data_avg - origin_avg) / origin_avg)))
#     return {"tpr": result}


# def read_aggregate_perf_result(sources: List[str]):
#     result = {}
#     origin_result = {}
#     for source in sources:
#         data = read_aggregate_perf_result_file(source)
#         for name, value in data.items():
#             if name not in result:
#                 result[name] = [map_data(value)]
#                 origin_result[name] = value
#             else:
#                 diff = (value - origin_result[name]) / origin_result[name]
#                 result[name].append(map_data((value, diff)))
#     return result

def map_data(data):
    if isinstance(data, tuple):
        return f"{data[0]:.1f}, {data[1] * 100:.1f}%"
    else:
        return f"{data:.1f}"

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
                continue
            if dependency.src.method in app.ignored_type:
                continue
            if dependency.dst.method in app.ignored_type:
                continue
            result.append((dependency, type))

        data = jsonpickle.encode(result, indent=2)
        open(app.ground_truth_path, "w").write(data)





if __name__ == "__main__":
    build_expected_dependencies()
