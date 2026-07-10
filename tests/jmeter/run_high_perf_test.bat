@echo off
chcp 65001 > nul
echo ==============================================
echo 沙箱节点高性能压测脚本（无限流限制）
echo ==============================================
echo.
echo 测试参数配置：
echo - 线程数: %THREADS% (默认50)
echo - 循环次数: %LOOP_COUNT% (默认10)
echo - 启动时间: %RAMP_UP% (默认10秒)
echo.
echo 按任意键开始压测...
pause > nul

if not defined THREADS set THREADS=50
if not defined LOOP_COUNT set LOOP_COUNT=10
if not defined RAMP_UP set RAMP_UP=10

echo 正在执行高并发压测...
echo 线程数: %THREADS%, 循环次数: %LOOP_COUNT%, 启动时间: %RAMP_UP%秒

mkdir results 2>nul

jmeter -n -t sandbox_high_perf_test.jmx ^
  -JTHREADS=%THREADS% ^
  -JLOOP_COUNT=%LOOP_COUNT% ^
  -JRAMP_UP=%RAMP_UP% ^
  -JHOST=gateway ^
  -JPORT=8080 ^
  -l results\sandbox_high_perf_result.jtl ^
  -e -o results\sandbox_high_perf_report

if %ERRORLEVEL% equ 0 (
  echo.
  echo ==============================================
  echo 压测完成！报告已生成：
  echo - 结果文件: results\sandbox_high_perf_result.jtl
  echo - HTML报告: results\sandbox_high_perf_report\index.html
  echo ==============================================
) else (
  echo.
  echo ==============================================
  echo 压测失败，请检查JMeter是否正确安装
  echo ==============================================
)

pause