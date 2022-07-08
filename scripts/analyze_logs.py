#!/usr/bin/env python3
# -*- coding: utf-8 -*-

unaffected_count = 0
affected_count = 0
with open("/tmp/data-flow.txt") as f:
    for line in f:
        if "EMPTY" in line:
            unaffected_count += 1
        else:
            affected_count += 1

print(unaffected_count)
print(affected_count)

