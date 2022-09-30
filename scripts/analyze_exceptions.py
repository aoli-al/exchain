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

if __name__ == "__main__":
    process(sys.argv[1])