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
    result = [0, 0, 0, 0, 0]
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
            if flag:
                if name not in exceptions:
                    exceptions[name] = 0
                exceptions[name] += 1
                count += 1
    print(count)
    for i in range(5):
        print(result[i] / count)
    for name, count in exceptions.items():
        print(name, count)

if __name__ == "__main__":
    process_csv(sys.argv[1])