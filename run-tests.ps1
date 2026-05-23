Write-Host "========================================" -ForegroundColor Cyan
Write-Host "StructExam 测试脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Set-Location $PSScriptRoot

Write-Host "[1/6] 检查 Docker 环境..." -ForegroundColor Yellow
try {
    $dockerVersion = docker --version
    Write-Host "Docker 环境检查通过: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "错误: Docker 未安装或未启动！" -ForegroundColor Red
    Read-Host "按任意键退出"
    exit 1
}
Write-Host ""

Write-Host "[2/6] 停止并清理旧容器..." -ForegroundColor Yellow
docker-compose down -v 2>$null
Write-Host "清理完成" -ForegroundColor Green
Write-Host ""

Write-Host "[3/6] 构建并启动基础服务..." -ForegroundColor Yellow
docker-compose up -d --build mysql redis nacos
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 启动基础服务失败！" -ForegroundColor Red
    Read-Host "按任意键退出"
    exit 1
}
Write-Host ""

Write-Host "[4/6] 等待 Nacos 就绪（约 60 秒）..." -ForegroundColor Yellow
Start-Sleep -Seconds 60
Write-Host ""

Write-Host "[5/6] 启动所有应用服务..." -ForegroundColor Yellow
docker-compose up -d --build gateway user-service exam-service code-service sandbox-node-1 sandbox-node-2 sandbox-node-3 frontend
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 启动应用服务失败！" -ForegroundColor Red
    Read-Host "按任意键退出"
    exit 1
}
Write-Host ""

Write-Host "[6/6] 启动测试服务..." -ForegroundColor Yellow
docker-compose up -d --build playwright jmeter
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 启动测试服务失败！" -ForegroundColor Red
    Read-Host "按任意键退出"
    exit 1
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "所有服务已启动" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "服务状态:" -ForegroundColor Cyan
docker-compose ps
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "等待服务完全就绪（约 120 秒）..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Start-Sleep -Seconds 120
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "开始执行 Playwright 功能测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
docker-compose exec -T playwright npm test
$playwrightExitCode = $LASTEXITCODE
Write-Host ""
Write-Host "Playwright 测试完成，退出码: $playwrightExitCode" -ForegroundColor Cyan
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "开始执行 JMeter 非功能测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "正在执行学生端 API 测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t structexam-student-api.jmx -l results/student-api.jtl -e -o results/student-api-report -JHOST=gateway -JPORT=8080

Write-Host ""
Write-Host "正在执行高并发一致性测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t concurrency_consistency.jmx -l results/concurrency.jtl -e -o results/concurrency-report -JHOST=gateway -JPORT=8080 -JTHREADS=30 -JRAMP_UP_SEC=10 -JLOOP_COUNT=3

Write-Host ""
Write-Host "正在执行可用性测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t availability_test.jmx -l results/availability.jtl -e -o results/availability-report -JHOST=gateway -JPORT=8080 -JDURATION_SEC=120 -JUSE_SCHEDULER=true

Write-Host ""
Write-Host "正在执行可靠性与容错测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t reliability_fault_tolerance.jmx -l results/reliability.jtl -e -o results/reliability-report -JHOST=gateway -JPORT=8080 -JTHREADS=10 -JLOOP_COUNT=3

Write-Host ""
Write-Host "正在执行 API 稳定性测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t api_stability_test.jmx -l results/stability.jtl -e -o results/stability-report -JHOST=gateway -JPORT=8080 -JTHREADS=10 -JDURATION_SEC=180 -JUSE_SCHEDULER=true

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "所有测试执行完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "测试结果位置:" -ForegroundColor Cyan
Write-Host "- Playwright 测试报告: ./tests/test-results/" -ForegroundColor Green
Write-Host "- JMeter 测试结果: ./tests/jmeter/results/" -ForegroundColor Green
Write-Host ""
Write-Host "可使用以下命令查看服务日志:" -ForegroundColor Cyan
Write-Host "  docker-compose logs -f" -ForegroundColor Gray
Write-Host ""
Write-Host "可使用以下命令停止所有服务:" -ForegroundColor Cyan
Write-Host "  docker-compose down" -ForegroundColor Gray
Write-Host ""
Read-Host "按任意键退出"
