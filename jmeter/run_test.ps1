<#
JMeter 高并发测试启动脚本
测试场景：多用户同时提交代码
#>

Write-Host "=== JMeter 代码提交高并发测试 ==="
Write-Host ""

# 1. 创建必要的目录
if (-not (Test-Path "results")) { New-Item -ItemType Directory -Path "results" | Out-Null }
if (-not (Test-Path "scripts")) { New-Item -ItemType Directory -Path "scripts" | Out-Null }
if (-not (Test-Path "logs")) { New-Item -ItemType Directory -Path "logs" | Out-Null }

# 2. 启动 JMeter 分布式测试
Write-Host "启动 JMeter 分布式测试..."
Write-Host "Master 节点: jmeter-master"
Write-Host "Slave 节点: jmeter-slave1, jmeter-slave2"
Write-Host "并发用户数: 100"
Write-Host "循环次数: 5"
Write-Host "加速时间: 60秒"
Write-Host ""

docker-compose up -d

# 3. 等待测试完成（约2分钟）
Write-Host "测试进行中...请等待结果生成"
Start-Sleep -Seconds 120

# 4. 停止容器
Write-Host ""
Write-Host "停止 JMeter 容器..."
docker-compose down

Write-Host ""
Write-Host "测试完成！报告已生成在 results/report 目录"
Write-Host "结果文件: results/result.jtl"
