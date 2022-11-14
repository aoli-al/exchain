import sys
import os
import json

def find_exception_without_source_vars(path: str):
    exceptions = set()
    exception_with_source_vars = set()
    file_path = os.path.join(path, "affected-var-results.json")
    exception_label_map = {}
    with open(file_path) as f:
        for line in f:
            data = json.loads(line)
            loc = f'{data["clazz"]}:{data["method"]}:{data["throwIndex"]}'

            exception_label = data["label"]
            if exception_label not in exceptions:
                exception_label_map[exception_label] = loc

            exceptions.add(exception_label)
            source_lines = data["sourceLines"]
            if source_lines:
                exception_with_source_vars.add(exception_label)

    visited = set()
    for lb in exceptions - exception_with_source_vars:
        loc = exception_label_map[lb]
        if loc not in visited:
            print(lb, loc)
        visited.add(loc)



if __name__ == "__main__":
    find_exception_without_source_vars(sys.argv[1])