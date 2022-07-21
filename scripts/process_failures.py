#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
from typing import Dict, List

def read_exception_data(path: str) -> Dict[str, List[str]]:
    data = {}
    triggered_exceptions = set()
    with open(os.path.join(path, "caught.txt")) as caught:
        for line in caught:
            [exception, trace] = line.split("thrown at")
            if exception not in data:
                data[exception] = []
            data[exception].append(trace)
            triggered_exceptions.add(line)
    return data, triggered_exceptions

if __name__=="__main__":
    normal, normal_exceptions = read_exception_data("data/no_injection")
    for i in range(0, 20):
        print(f"============================================== {i}")
        case, case_exceptions = read_exception_data(f"data/injection_{i}")
        diff = case.keys() - normal.keys()
        print(diff)
        for e in diff:
            print(e)
            print(case[e])
            print("===")
        #  for exception in case_exceptions - normal_exceptions:
            #  print(exception.split("thrown at")[0])
        print("==============================================")
