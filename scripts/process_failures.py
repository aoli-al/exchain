#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
from typing import Set, Dict, List

def read_exception_data(path: str) -> Dict[str, List[str]]:
    data = {}
    with open(os.path.join(path, "caught.txt")) as caught:
        for line in caught:
            [exception, trace] = line.split("thrown at")
            if exception not in data:
                data[exception] = []
            data[exception].append(trace)
    return data

if __name__=="__main__":
    normal = read_exception_data("data/no_injection")
    #  for i in range(20):
    i = 0
    case = read_exception_data(f"data/injection_{i}")
    diff = case.keys() - normal.keys()
    print(diff)

