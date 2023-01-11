#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys

def process(data):
    result = {}
    for line in data:
        [url, time] = line.split(", ")
        if url not in result:
            result[url] = []
        result[url].append(int(time))
    return result

with open(sys.argv[1]) as origin:
    origin_result = process(origin)

with open(sys.argv[2]) as instrumented:
    instrumented_result = process(instrumented)

increase = []
increase_map = {}
for key in origin_result:
    if key not in instrumented_result:
        continue
    origin_avg = sum(origin_result[key]) / len(origin_result[key])
    instrumented_avg = sum(instrumented_result[key]) / len(instrumented_result[key])
    increase.append((instrumented_avg - origin_avg) / origin_avg)
    increase_map[key] = increase[-1]
#  print(origin_result)
#  print(instrumented_result)
#  print(increase)
print(increase_map)

print(sum(increase) / len(increase))
