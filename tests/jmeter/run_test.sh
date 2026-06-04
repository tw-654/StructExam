#!/bin/bash
jmeter_path="jmeter"
test_plan_path="code_submit_test.jmx"
results_path="results"

if [ ! -d "$results_path" ]; then
    mkdir -p "$results_path"
fi

$jmeter_path -n -t $test_plan_path -l "$results_path/results.jtl" -e -o "$results_path/report"