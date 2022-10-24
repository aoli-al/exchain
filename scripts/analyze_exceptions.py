from logging import exception
import matplotlib.pyplot as plt
import json
import sys

def process(path: str):
    result = {}
    interesting = []
    prefix = "org.apache.hadoop.hdfs.server.namenode"
    with open(path) as f:
        for line in f:
            obj = json.loads(line)
            type = obj['type']
            if "ClassNotFoundException" in type:
                continue
            type = type.split(".")[-1]
            if type not in result:
                result[type] = 0
            result[type] += 1
            for element in obj['stack']:
                if prefix in element and "TestCheckpoint/main" not in element:
                    interesting.append(obj)

    for (k, v) in result.items():
        print(k, v)
    print(len(interesting))

def process_csv(path: str):
    # obj, array, primitive, null, fields.
    result = [0, 0, 0, 0, 0]
    field_only_exceptions = 0
    local_only_exceptions = 0
    both_exceptions = 0
    count = 0
    exceptions = {}
    total = 0
    with open(path) as f:
        for line in f:
            name, *rest = line.split(",")
            stats = [int(item) for item in rest]
            flag = False
            for idx in range(5):
                result[idx] += stats[idx]
                if stats[idx] != 0:
                    flag = True
            local = stats[0] + stats[2] > 0
            field = stats[-1] > 0
            if local and not field:
                local_only_exceptions += 1
            if field and not local:
                field_only_exceptions += 1
            if local and field:
                both_exceptions += 1
            if flag:
                if name not in exceptions:
                    exceptions[name] = 0
                exceptions[name] += 1
                count += 1
    print(count)
    print("Local only", local_only_exceptions)
    print("Field only", field_only_exceptions)
    print("Both", both_exceptions)
    for i in range(5):
        print(result[i] / count)
    for name, count in exceptions.items():
        print(name, count)

if __name__ == "__main__":
    process_csv(sys.argv[1])
