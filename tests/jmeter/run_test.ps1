$jmeterPath = "jmeter"
$testPlanPath = "code_submit_test.jmx"
$resultsPath = "results"

if (-not (Test-Path $resultsPath)) {
    New-Item -ItemType Directory -Path $resultsPath
}

& $jmeterPath -n -t $testPlanPath -l "$resultsPath\results.jtl" -e -o "$resultsPath\report"