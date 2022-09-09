#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys

def process(data):
    result = {}
    for line in data:
        [url, time, method] = line.split(", ")
        key = f"{method}:{url}"
        if key not in result:
            result[key] = []
        result[key].append(int(time))
    return result

with open(sys.argv[1]) as origin:
    origin_result = process(origin)

with open(sys.argv[2]) as instrumented:
    instrumented_result = process(instrumented)

increase = []
for key in origin_result:
    if key not in instrumented_result:
        continue
    origin_avg = sum(origin_result[key]) / len(origin_result[key])
    instrumented_avg = sum(instrumented_result[key]) / len(instrumented_result[key])
    increase.append((instrumented_avg - origin_avg) / origin_avg)

print(sum(increase) / len(increase))
