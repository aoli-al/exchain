from typing import Dict, List, Tuple, Set, Any
import itertools
import jsonpickle
import json
import os
from commons import *
import seaborn as sns
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
    return (tp > 0, fp)

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
    return max_distance + 1

def get_root_cause_location(result: List[Tuple[Link, LinkType]], path: str) -> int:
    exceptions = []
    with open(path) as f:
        for line in f:
            exceptions.append(data_to_message(json.loads(line)))
    min_loc = 9999999
    for item in result:
        link = item[0]
        loc = 0
        for exception in exceptions:
            loc += 1
            if link.src == exception:
                break
        min_loc = min(loc, min_loc)
    return min_loc



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
            name = map_name(name)
            items[name] = float(data.strip())
    return items

def map_type(t):
    if t == "static":
        return "SI+Static"
    if t == "dynamic":
        return "SI+Dynamic"
    if "debug" in t:
        return "JVMTi"
    if "noopt" in t:
        return "NoOpt"
    else:
        return "\sc{ExChain}"


import pandas as pd
def read_perf_result(app, perf_result: Dict[str, List[Any]], types = ["origin", "static", "hybrid", "dynamic"], append_origin=True, remap_keys=True):
    name = app.test_name.split("_")[0].upper()
    origin_result = {}
    for t in types:
        data = {}
        for i in range(10):
            path = app.perf_result_path(t, i, False)
            result = read_aggregate_perf_result(path)
            for key, value in  result.items():
                if key not in data:
                    data[key] = []
                data[key].append(value)
        for key, value in data.items():
            df = pd.DataFrame(value)
            mean = float(df.mean())
            if key not in perf_result:
                perf_result[key] = []
            if t == "origin":
                origin_result[key] = mean
            else:
                if append_origin:
                    title = f"{name}\n{origin_result[key]:.1f}({app.get_measure(key)})"
                else:
                    title = name
                # if title == "HDFS":
                #     print(title)
                #     print(t)
                #     print(sum(value) / len(value))
                #     print(origin_result[key])
                for v in value:
                    if remap_keys:
                        t_val = map_type(t)
                    else:
                        t_val = t
                    perf_result[key].append([title, t_val, v / origin_result[key]])



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
                elif isinstance(item, float):
                    formatted_result.append(f"{item:.1f}")
                else:
                    formatted_result.append(str(item))

            f.write(" & ".join(formatted_result))
            if row != data[-1]:
                f.write(" \\\\\n")


def map_name(input):
    if input == "exec_time":
        return "latency"
    return input

def save_stacked_perf_to_pdf(df, path, key):
    from matplotlib.patches import Patch
    colors = ["#5975A4", "#CC8963", '#5F9E6E', '#B52B2B']
    sns.set(rc={'figure.figsize':(8,4), "text.usetex": True})
    colors = sns.color_palette(colors, desat=1.0)
    axis = sns.histplot(
        data = df,
        x="Application",
        hue="System",
        weights=0,
        multiple="stack",
        shrink=0.8,
        hue_order=reversed(["JVMTi", "\sc{ExChain}"]),
        palette=colors,
        alpha=1.0
    )
    hatches = ['//', '+', 'o', 'O', '.']
    for i, bar in enumerate(axis.patches):
        hatch = hatches[i // 6]
        bar.set_hatch(hatch)
    if key == "Latency":
        ykey =f'{key} Overhead Breakdown (\\%)'
    else:
        ykey =f'{key} Degradation Breakdown (\\%)'
    axis.set(ylabel=ykey)
    # axis.legend()
    # patch_1 = Patch(label='Taint', hatch=hatches[3], facecolor=colors[3])
    # patch_2 = Patch(label='Logging', hatch=hatches[2], facecolor=colors[2])
    patch_3 = Patch(label='\sc{ExChain}', hatch=hatches[1], facecolor=colors[0])
    patch_4 = Patch(label='JVMTi', hatch=hatches[0], facecolor=colors[1])
    axis.legend(handles=list(reversed([patch_3, patch_4])),
                loc='upper center', bbox_to_anchor=(
        0.5, 1.1), ncol=4, fancybox=True, shadow=True)
    # axis.get_legnd()
    fig = axis.get_figure()
    fig.savefig(path, bbox_inches='tight', pad_inches=0.1)
    fig.clf()


def save_perf_data_to_pdf(data, path):
    for key, value in data.items():
        if key == "latency":
            y_key =key.capitalize() + " Overhead (\\%)"
        else:
            y_key =key.capitalize() + " Degradation (\\%)"
        df = pd.DataFrame(value, columns=["Application", "System", y_key])
        df[y_key] -= 1
        if key != "latency":
            df[y_key] = -df[y_key]
        df[y_key] *= 100

        sns.set(rc={'figure.figsize':(8,4), "text.usetex": True})
        axis = sns.barplot(
            data = df,
            x="Application",
            hue="System",
            y=y_key,
            hue_order=[
                "SI+Static",
                "SI+Dynamic",
                "\sc{ExChain}"]
        )
        if key == 'latency':
            axis.set_yscale("log")
        hatches = ['//', '+', 'o', 'O', '.']
        for i, bar in enumerate(axis.patches):
            hatch = hatches[i // 6]
            bar.set_hatch(hatch)
        axis.legend(loc='upper center', bbox_to_anchor=(
            0.5, 1.1), ncol=3, fancybox=True, shadow=True)
        fig = axis.get_figure()
        fig.savefig(os.path.join(path, key + ".pdf"), bbox_inches='tight', pad_inches=0.1)
        fig.clf()


def save_perf_data_to_latex_table(data, path):
    with open(path, "w") as f:
        keys = ['latency', 'throughput', ]
        for t in keys:
            print(data)
            print(t)
            value = data[t]
            first = True
            for v, item in value.items():
                formatted_result = []
                if first:
                    row = "\\multirow{" + str(len(value)) + "}{*}{\\rotatebox[origin=c]{90}{" + map_name(t) + "}}"
                    first = False
                else:
                    row = ""
                    f.write(" \\\\\n")
                for x in [v, row, *item]:
                    if isinstance(x, float):
                        x = f"{x:.1f}"
                    formatted_result.append(x)
                f.write(" & ".join(formatted_result))
            if t != keys[-1]:
                f.write(" \\\\\n")
                f.write("\\hline\n")


def draw_dist(data, path):
    sns.set(rc={'figure.figsize':(8,4)})
    axis = sns.barplot(
        data = data,
        x="Name",
        y="Ratio",
        hue="Type",
    )
    axis.set(xlabel = "Application")
    axis.set(ylabel = "Ratio (%)")
    hatches = ['//', '+', 'o', 'O', '.']
    for i, bar in enumerate(axis.patches):
        hatch = hatches[i // 8]
        bar.set_hatch(hatch)
    axis.legend(loc='upper center', bbox_to_anchor=(0.5, 1.1), ncol=4, fancybox=True, shadow=True)


    fig = axis.get_figure()
    fig.savefig(path, bbox_inches='tight', pad_inches=0.1)



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
