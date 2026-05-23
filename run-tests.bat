@echo off
echo ========================================
echo StructExam 测试脚本
echo ========================================
echo.

cd /d "%~dp0"

echo [1/6] 检查 Docker 环境...
docker --version >nul 2>&1
if errorlevel 1 (
    echo 错误: Docker 未安装或未启动！
    pause
    exit /b 1
)
echo Docker 环境检查通过
echo.

echo [2/6] 停止并清理旧容器...
docker-compose down -v 2>nul
echo 清理完成
echo.

echo [3/6] 构建并启动所有服务...
docker-compose up -d --build mysql redis nacos
if errorlevel 1 (
    echo 错误: 启动基础服务失败！
    pause
    exit /b 1
)
echo.

echo [4/6] 等待 Nacos 就绪（约 60 秒）...
timeout /t 60 /nobreak >nul
echo.

echo [5/6] 启动所有应用服务...
docker-compose up -d --build gateway user-service exam-service code-service sandbox-node-1 sandbox-node-2 sandbox-node-3 frontend
if errorlevel 1 (
    echo 错误: 启动应用服务失败！
    pause
    exit /b 1
)
echo.

echo [6/6] 启动测试服务...
docker-compose up -d --build playwright jmeter
if errorlevel 1 (
    echo 错误: 启动测试服务失败！
    pause
    exit /b 1
)
echo.

echo ========================================
echo 所有服务已启动
echo ========================================
echo.
echo 服务状态:
docker-compose ps
echo.
echo ========================================
echo 等待服务完全就绪（约 120 秒）...
echo ========================================
timeout /t 120 /nobreak >nul
echo.

echo ========================================
echo 开始执行 Playwright 功能测试
echo ========================================
docker-compose exec -T playwright npm test
set PLAYWRIGHT_EXIT_CODE=%errorlevel%
echo.
echo Playwright 测试完成，退出码: %PLAYWRIGHT_EXIT_CODE%
echo.

echo ========================================
echo 开始执行 JMeter 非功能测试
echo ========================================

echo 正在执行学生端 API 测试...
docker-compose exec -T jmeter jmeter -n -t structexam-student-api.jmx -l results/student-api.jtl -e -o results/student-api-report -JHOST=gateway -JPORT=8080

echo.
echo 正在执行高并发一致性测试...
docker-compose exec -T jmeter jmeter -n -t concurrency_consistency.jmx -l results/concurrency.jtl -e -o results/concurrency-report -JHOST=gateway -JPORT=8080 -JTHREADS=30 -JRAMP_UP_SEC=10 -JLOOP_COUNT=3

echo.
echo 正在执行可用性测试...
docker-compose exec -T jmeter jmeter -n -t availability_test.jmx -l results/availability.jtl -e -o results/availability-report -JHOST=gateway -JPORT=8080 -JDURATION_SEC=120 -JUSE_SCHEDULER=true

echo.
echo 正在执行可靠性与容错测试...
docker-compose exec -T jmeter jmeter -n -t reliability_fault_tolerance.jmx -l results/reliability.jtl -e -o results/reliability-report -JHOST=gateway -JPORT=8080 -JTHREADS=10 -JLOOP_COUNT=3

echo.
echo 正在执行 API 稳定性测试...
docker-compose exec -T jmeter jmeter -n -t api_stability_test.jmx -l results/stability.jtl -e -o results/stability-report -JHOST=gateway -JPORT=8080 -JTHREADS=10 -JDURATION_SEC=180 -JUSE_SCHEDULER=true

echo.
echo ========================================
echo 所有测试执行完成
echo ========================================
echo.
echo 测试结果位置:
echo - Playwright 测试报告: ./tests/test-results/
echo - JMeter 测试结果: ./tests/jmeter/results/
echo.
echo 可使用以下命令查看服务日志:
echo   docker-compose logs -f
echo.
echo 可使用以下命令停止所有服务:
echo   docker-compose down
echo.
pause
