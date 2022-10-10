import sys
import json

def process(path: str):
    dependency = json.load(open(f"{path}/dependency.json"))
    exceptions = {}
    with open(f"{path}/exception.json") as f:
        for line in f:
            obj = json.loads(line)
            if 'message' not in obj:
                obj['message'] = 'null'
            exceptions[obj['label']] = obj
    for label in dependency:
        obj = exceptions[int(label)]
        print(f"Exception({label}): {obj['type']}, message: {obj['message']} is caused by:")
        for dep in dependency[label]:
            exception = exceptions[dep]
            print(f"------>    Exception({exception['label']}): {exception['type']}, message: {exception['message']}.")


if __name__ == "__main__":
    process(sys.argv[1])