Write-Host "========================================" -ForegroundColor Cyan
Write-Host "StructExam 快速测试脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Set-Location $PSScriptRoot

# 测试选项
$testOptions = @(
    @{ Name = "Playwright 功能测试(全部)"; Script = "npm test" }
    @{ Name = "JMeter 性能测试(全部)"; Script = "all-jmeter" }
    @{ Name = "登录性能基线测试"; Script = "jmeter -n -t login-performance-baseline.jmx -l results/login-baseline.jtl -e -o results/login-performance-baseline-report -JHOST=gateway -JPORT=8080" }
    @{ Name = "高并发考试测试"; Script = "jmeter -n -t exam_high_concurrency.jmx -l results/exam-high-concurrency.jtl -e -o results/exam_high_concurrency-report -JHOST=gateway -JPORT=8080 -JTHREADS=30 -JLOOP_COUNT=3" }
    @{ Name = "只读接口测试"; Script = "jmeter -n -t read-only.jmx -l results/read-only.jtl -e -o results/read-only-report -JHOST=gateway -JPORT=8080 -JTHREADS=20 -JLOOP_COUNT=5" }
    @{ Name = "API稳定性测试"; Script = "jmeter -n -t api_stability_test.jmx -l results/stability.jtl -e -o results/api_stability_test_report -JHOST=gateway -JPORT=8080 -JTHREADS=20 -JDURATION_SEC=300 -JUSE_SCHEDULER=true" }
    @{ Name = "教师端API测试"; Script = "jmeter -n -t teacher-api.jmx -l results/teacher-api.jtl -e -o results/teacher-api-report -JHOST=gateway -JPORT=8080 -JTHREADS=20 -JLOOP_COUNT=3" }
    @{ Name = "管理员端API测试"; Script = "jmeter -n -t admin-api.jmx -l results/admin-api.jtl -e -o results/admin-api-report -JHOST=gateway -JPORT=8080 -JTHREADS=20 -JLOOP_COUNT=3" }
    @{ Name = "并发一致性测试"; Script = "jmeter -n -t concurrency_consistency.jmx -l results/concurrency.jtl -e -o results/concurrency_consistency_report -JHOST=gateway -JPORT=8080 -JTHREADS=30 -JLOOP_COUNT=3" }
    @{ Name = "可靠性与容错测试"; Script = "jmeter -n -t reliability_fault_tolerance.jmx -l results/reliability.jtl -e -o results/reliability_fault_tolerance-report -JHOST=gateway -JPORT=8080 -JTHREADS=10 -JLOOP_COUNT=5" }
)

Write-Host "请选择要执行的测试（输入序号，多个用逗号分隔）：" -ForegroundColor Cyan
for ($i = 0; $i -lt $testOptions.Length; $i++) {
    Write-Host "$($i + 1). $($testOptions[$i].Name)" -ForegroundColor Yellow
}
Write-Host ""
$selection = Read-Host "输入选择"
$selectedIndices = $selection -split ',' | ForEach-Object { [int]$_.Trim() - 1 }

# 创建结果目录
mkdir -Force results | Out-Null

foreach ($index in $selectedIndices) {
    if ($index -ge 0 -and $index -lt $testOptions.Length) {
        $selectedTest = $testOptions[$index]
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "正在执行: $($selectedTest.Name)" -ForegroundColor Cyan
        Write-Host "========================================" -ForegroundColor Cyan
        
        if ($selectedTest.Script -eq "npm test") {
            docker-compose exec -T playwright npm test
        } elseif ($selectedTest.Script -eq "all-jmeter") {
            # 执行所有JMeter测试
            $jmeterTests = @(
                "jmeter -n -t login-performance-baseline.jmx -l results/login-baseline.jtl -e -o results/login-performance-baseline-report -JHOST=gateway -JPORT=8080",
                "jmeter -n -t read-only.jmx -l results/read-only.jtl -e -o results/read-only-report -JHOST=gateway -JPORT=8080 -JTHREADS=20 -JLOOP_COUNT=5",
                "jmeter -n -t exam_high_concurrency.jmx -l results/exam-high-concurrency.jtl -e -o results/exam_high_concurrency-report -JHOST=gateway -JPORT=8080 -JTHREADS=30 -JLOOP_COUNT=3",
                "jmeter -n -t api_stability_test.jmx -l results/stability.jtl -e -o results/api_stability_test_report -JHOST=gateway -JPORT=8080 -JTHREADS=20 -JDURATION_SEC=300 -JUSE_SCHEDULER=true",
                "jmeter -n -t reliability_fault_tolerance.jmx -l results/reliability.jtl -e -o results/reliability_fault_tolerance-report -JHOST=gateway -JPORT=8080 -JTHREADS=10 -JLOOP_COUNT=5"
            )
            
            foreach ($test in $jmeterTests) {
                docker-compose exec -T jmeter $test
                Write-Host ""
            }
        } else {
            docker-compose exec -T jmeter $selectedTest.Script
        }
        
        Write-Host "完成: $($selectedTest.Name)" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试执行完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "测试结果位置: ./results/" -ForegroundColor Green
Read-Host "按任意键退出"