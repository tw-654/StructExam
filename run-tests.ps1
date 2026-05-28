Write-Host "========================================" -ForegroundColor Cyan
Write-Host "StructExam 完整测试脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Set-Location $PSScriptRoot

Write-Host "[1/7] 检查 Docker 环境..." -ForegroundColor Yellow
try {
    $dockerVersion = docker --version
    Write-Host "Docker 环境检查通过: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "错误: Docker 未安装或未启动！" -ForegroundColor Red
    Read-Host "按任意键退出"
    exit 1
}
Write-Host ""

Write-Host "[2/7] 停止并清理旧容器..." -ForegroundColor Yellow
docker-compose down -v 2>$null
Write-Host "清理完成" -ForegroundColor Green
Write-Host ""

Write-Host "[3/7] 构建并启动基础服务..." -ForegroundColor Yellow
docker-compose up -d --build mysql redis nacos
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 启动基础服务失败！" -ForegroundColor Red
    Read-Host "按任意键退出"
    exit 1
}
Write-Host ""

Write-Host "[4/7] 等待 Nacos 就绪（约 60 秒）..." -ForegroundColor Yellow
Start-Sleep -Seconds 60
Write-Host ""

Write-Host "[5/7] 启动所有应用服务..." -ForegroundColor Yellow
docker-compose up -d --build gateway user-service exam-service code-service sandbox-node-1 sandbox-node-2 sandbox-node-3 frontend
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 启动应用服务失败！" -ForegroundColor Red
    Read-Host "按任意键退出"
    exit 1
}
Write-Host ""

Write-Host "[6/7] 启动测试服务..." -ForegroundColor Yellow
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

# 创建结果目录
mkdir -Force results | Out-Null

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "阶段一：执行 Playwright 功能测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "正在执行登录流程测试..." -ForegroundColor Yellow
docker-compose exec -T playwright npx playwright test login-flow.spec.js --reporter=html
Write-Host ""

Write-Host "正在执行学生路由测试..." -ForegroundColor Yellow
docker-compose exec -T playwright npx playwright test student-routing.spec.js --reporter=html
Write-Host ""

Write-Host "正在执行考试列表测试..." -ForegroundColor Yellow
docker-compose exec -T playwright npx playwright test exam-list.spec.js --reporter=html
Write-Host ""

Write-Host "正在执行考试页面测试..." -ForegroundColor Yellow
docker-compose exec -T playwright npx playwright test exam-page.spec.js --reporter=html
Write-Host ""

Write-Host "正在执行代码编辑器测试..." -ForegroundColor Yellow
docker-compose exec -T playwright npx playwright test code-editor.spec.js --reporter=html
Write-Host ""

Write-Host "正在执行历史记录测试..." -ForegroundColor Yellow
docker-compose exec -T playwright npx playwright test history.spec.js --reporter=html
Write-Host ""

Write-Host "正在执行用户资料测试..." -ForegroundColor Yellow
docker-compose exec -T playwright npx playwright test profile.spec.js --reporter=html
Write-Host ""

Write-Host "正在执行教师仪表板测试..." -ForegroundColor Yellow
docker-compose exec -T playwright npx playwright test teacher-dashboard.spec.js --reporter=html
Write-Host ""

Write-Host "正在执行管理员仪表板测试..." -ForegroundColor Yellow
docker-compose exec -T playwright npx playwright test admin-dashboard.spec.js --reporter=html
Write-Host ""

Write-Host "正在执行分布式测试..." -ForegroundColor Yellow
docker-compose exec -T playwright npx playwright test distributed.spec.js --reporter=html
Write-Host ""

Write-Host "正在执行弹性测试..." -ForegroundColor Yellow
docker-compose exec -T playwright npx playwright test resilience.spec.js --reporter=html
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "阶段二：执行 JMeter 性能测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "正在执行登录性能基线测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t login-performance-baseline.jmx -l results/login-baseline.jtl -e -o results/login-performance-baseline-report -JHOST=gateway -JPORT=8080

Write-Host ""
Write-Host "正在执行只读接口测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t read-only.jmx -l results/read-only.jtl -e -o results/read-only-report -JHOST=gateway -JPORT=8080 -JTHREADS=20 -JLOOP_COUNT=5

Write-Host ""
Write-Host "正在执行读链测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t read-chain.jmx -l results/read-chain.jtl -e -o results/read-chain-report -JHOST=gateway -JPORT=8080 -JTHREADS=10 -JLOOP_COUNT=3

Write-Host ""
Write-Host "正在执行只写接口测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t write-only.jmx -l results/write-only.jtl -e -o results/write-only-report -JHOST=gateway -JPORT=8080 -JTHREADS=10 -JLOOP_COUNT=3

Write-Host ""
Write-Host "正在执行教师端API测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t teacher-api.jmx -l results/teacher-api.jtl -e -o results/teacher-api-report -JHOST=gateway -JPORT=8080 -JTHREADS=20 -JLOOP_COUNT=3

Write-Host ""
Write-Host "正在执行管理员端API测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t admin-api.jmx -l results/admin-api.jtl -e -o results/admin-api-report -JHOST=gateway -JPORT=8080 -JTHREADS=20 -JLOOP_COUNT=3

Write-Host ""
Write-Host "正在执行高并发考试测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t exam_high_concurrency.jmx -l results/exam-high-concurrency.jtl -e -o results/exam_high_concurrency-report -JHOST=gateway -JPORT=8080 -JTHREADS=30 -JLOOP_COUNT=3

Write-Host ""
Write-Host "正在执行并发一致性测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t concurrency_consistency.jmx -l results/concurrency.jtl -e -o results/concurrency_consistency_report -JHOST=gateway -JPORT=8080 -JTHREADS=30 -JLOOP_COUNT=3

Write-Host ""
Write-Host "正在执行API稳定性测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t api_stability_test.jmx -l results/stability.jtl -e -o results/api_stability_test_report -JHOST=gateway -JPORT=8080 -JTHREADS=20 -JDURATION_SEC=300 -JUSE_SCHEDULER=true

Write-Host ""
Write-Host "正在执行可用性测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t availability_test.jmx -l results/availability.jtl -e -o results/availability_report -JHOST=gateway -JPORT=8080 -JDURATION_SEC=180 -JUSE_SCHEDULER=true

Write-Host ""
Write-Host "正在执行可靠性与容错测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t reliability_fault_tolerance.jmx -l results/reliability.jtl -e -o results/reliability_fault_tolerance-report -JHOST=gateway -JPORT=8080 -JTHREADS=10 -JLOOP_COUNT=5

Write-Host ""
Write-Host "正在执行提交轮询测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t submit-poll.jmx -l results/submit-poll.jtl -e -o results/submit-poll-report -JHOST=gateway -JPORT=8080 -JTHREADS=15 -JLOOP_COUNT=3

Write-Host ""
Write-Host "正在执行登录专项测试..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t login-only.jmx -l results/login-only.jtl -e -o results/login-only-report -JHOST=gateway -JPORT=8080 -JTHREADS=30 -JLOOP_COUNT=5

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "阶段三：大规模考试场景模拟" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "正在执行大规模考试场景测试（100学生并发）..." -ForegroundColor Yellow
docker-compose exec -T jmeter jmeter -n -t mass-exam-simulation.jmx -l results/mass-exam.jtl -e -o results/mass-exam-report -JHOST=gateway -JPORT=8080 -JTHREADS=100 -JRAMP_UP_SEC=60

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "阶段四：安全扫描测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "正在执行安全扫描（SQL注入、XSS检测）..." -ForegroundColor Yellow
cd tests
python security-scan.py --url http://gateway:8080
cd ..

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "所有测试执行完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "测试结果位置:" -ForegroundColor Cyan
Write-Host "- Playwright 测试报告: ./tests/test-results/" -ForegroundColor Green
Write-Host "- JMeter 测试结果: ./results/" -ForegroundColor Green
Write-Host ""
Write-Host "可使用以下命令查看服务日志:" -ForegroundColor Cyan
Write-Host "  docker-compose logs -f" -ForegroundColor Gray
Write-Host ""
Write-Host "可使用以下命令停止所有服务:" -ForegroundColor Cyan
Write-Host "  docker-compose down" -ForegroundColor Gray
Write-Host ""
Read-Host "按任意键退出"