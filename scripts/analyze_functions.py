#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import matplotlib.pyplot as plt

method_exception_map = {}
exception_method_map = {}
exception_counter = {}
method_with_exception = 0
with open('/tmp/enter.txt') as f:
    for line in f:
        [method, exceptions] = [obj.strip() for obj in line.split("throws:")]
        method = method[7:-1]
        exceptions = exceptions[1:-1]
        exceptions = exceptions.split(",") if exceptions else []
        method_exception_map[method] = exceptions 
        if exceptions:
            method_with_exception += 1
        for exception in exceptions:
            if exception not in exception_method_map:
                exception_method_map[exception] = set()
                exception_counter[exception] = 0
            exception_method_map[exception].add(method)
            exception_counter[exception] += 1

plot_data = []
plot_key = []
for k, v in sorted(exception_counter.items(), key=lambda item: item[1], reverse=True):
    print(k, v)
    plot_key.append(k)
    plot_data.append(v)


print(plot_data)
plt.bar(plot_key, plot_data)
plt.xlabel("Exception")
plt.ylabel("# Methods Throw")
plt.xticks([])
plt.savefig("pic.png")
        
print(method_with_exception)
print(len(method_exception_map))
