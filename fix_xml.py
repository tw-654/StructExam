# -*- coding: utf-8 -*-

file_path = r'd:\code\StructExam\tests\jmeter\exam_high_concurrency.jmx'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

print("Full OnceOnlyController section (lines 39-76):")
for i, line in enumerate(lines[38:76], start=39):
    stripped = line.rstrip('\n')
    spaces = len(stripped) - len(stripped.lstrip())
    content = stripped.lstrip()
    print(f"{i} ({spaces:2d}): {repr(stripped)}")