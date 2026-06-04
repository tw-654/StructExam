param(
    [string]$OutputFile = "docker_stats.csv",
    [int]$IntervalSeconds = 5,
    [int]$DurationSeconds = 300
)

$startTime = Get-Date
$endTime = $startTime.AddSeconds($DurationSeconds)

Write-Host "开始监控Docker容器资源使用..."
Write-Host "输出文件: $OutputFile"
Write-Host "采样间隔: $IntervalSeconds 秒"
Write-Host "监控时长: $DurationSeconds 秒"

"timestamp,container,cpu_percent,memory_usage,memory_limit,memory_percent,network_rx,network_tx" | Out-File -FilePath $OutputFile -Encoding utf8

while ((Get-Date) -lt $endTime) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    
    try {
        $stats = docker stats --no-stream --format "{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.MemLimit}},{{.MemPerc}},{{.NetIO}}" 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            foreach ($line in $stats) {
                if ($line -match '^([^,]+),(\d+\.\d+)%,(\d+\.\d+\s+\w+)/(\d+\.\d+\s+\w+),(\d+\.\d+)%,(\d+\.\d+\s+\w+)/(\d+\.\d+\s+\w+)$') {
                    $container = $matches[1]
                    $cpu = $matches[2]
                    $memUsage = $matches[3]
                    $memLimit = $matches[4]
                    $memPercent = $matches[5]
                    $netRx = $matches[6]
                    $netTx = $matches[7]
                    
                    "$timestamp,$container,$cpu,$memUsage,$memLimit,$memPercent,$netRx,$netTx" | Out-File -FilePath $OutputFile -Encoding utf8 -Append
                }
            }
            Write-Host "$timestamp - 采样成功"
        } else {
            Write-Host "$timestamp - Docker stats 命令执行失败: $stats"
        }
    } catch {
        Write-Host "$timestamp - 监控异常: $_"
    }
    
    Start-Sleep -Seconds $IntervalSeconds
}

Write-Host "监控结束，结果已保存到 $OutputFile"