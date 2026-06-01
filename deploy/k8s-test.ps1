<#
Kubernetes 部署测试脚本
验证 K8s 环境下的调度与网络策略
#>

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "StructExam K8s 部署测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Set-Location $PSScriptRoot

# 检查 kubectl
Write-Host "[1/4] 检查 kubectl 环境..." -ForegroundColor Yellow
try {
    $kubectlVersion = kubectl version --client -o json | ConvertFrom-Json
    Write-Host "kubectl 版本: $($kubectlVersion.clientVersion.gitVersion)" -ForegroundColor Green
} catch {
    Write-Host "错误: kubectl 未安装或未配置!" -ForegroundColor Red
    Read-Host "按任意键退出"
    exit 1
}
Write-Host ""

# 检查集群状态
Write-Host "[2/4] 检查 Kubernetes 集群状态..." -ForegroundColor Yellow
kubectl cluster-info
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 无法连接到 K8s 集群!" -ForegroundColor Red
    Read-Host "按任意键退出"
    exit 1
}
Write-Host ""

# 部署应用
Write-Host "[3/4] 部署 StructExam 到 K8s..." -ForegroundColor Yellow

# 创建命名空间
kubectl create namespace structexam 2>&1 | Out-Null

# 部署 MySQL
Write-Host "部署 MySQL..." -ForegroundColor Gray
kubectl apply -f mysql-deployment.yaml -n structexam
kubectl apply -f mysql-service.yaml -n structexam

# 部署 Redis
Write-Host "部署 Redis..." -ForegroundColor Gray
kubectl apply -f redis-deployment.yaml -n structexam
kubectl apply -f redis-service.yaml -n structexam

# 部署 Nacos
Write-Host "部署 Nacos..." -ForegroundColor Gray
kubectl apply -f nacos-deployment.yaml -n structexam
kubectl apply -f nacos-service.yaml -n structexam

# 等待基础服务就绪
Write-Host "等待基础服务就绪..." -ForegroundColor Gray
Start-Sleep -Seconds 120

# 部署应用服务
Write-Host "部署 Gateway..." -ForegroundColor Gray
kubectl apply -f gateway-deployment.yaml -n structexam
kubectl apply -f gateway-service.yaml -n structexam

Write-Host "部署 User Service..." -ForegroundColor Gray
kubectl apply -f user-service-deployment.yaml -n structexam

Write-Host "部署 Exam Service..." -ForegroundColor Gray
kubectl apply -f exam-service-deployment.yaml -n structexam

Write-Host "部署 Code Service..." -ForegroundColor Gray
kubectl apply -f code-service-deployment.yaml -n structexam

Write-Host "部署 Sandbox Nodes..." -ForegroundColor Gray
kubectl apply -f sandbox-node-deployment.yaml -n structexam

Write-Host "部署 Frontend..." -ForegroundColor Gray
kubectl apply -f frontend-deployment.yaml -n structexam
kubectl apply -f frontend-service.yaml -n structexam

Write-Host ""
Write-Host "部署完成" -ForegroundColor Green
Write-Host ""

# 验证部署
Write-Host "[4/4] 验证 K8s 部署..." -ForegroundColor Yellow

# 检查 Pod 状态
Write-Host "检查 Pod 状态:" -ForegroundColor Gray
kubectl get pods -n structexam

# 检查服务状态
Write-Host ""
Write-Host "检查服务状态:" -ForegroundColor Gray
kubectl get services -n structexam

# 检查部署状态
Write-Host ""
Write-Host "检查部署状态:" -ForegroundColor Gray
kubectl get deployments -n structexam

# 检查网络策略
Write-Host ""
Write-Host "检查网络策略:" -ForegroundColor Gray
kubectl get networkpolicy -n structexam 2>&1

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "K8s 部署测试完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "使用以下命令查看服务日志:" -ForegroundColor Cyan
Write-Host "  kubectl logs -n structexam <pod-name>" -ForegroundColor Gray
Write-Host ""
Write-Host "使用以下命令访问前端:" -ForegroundColor Cyan
Write-Host "  kubectl port-forward service/frontend 8080:80 -n structexam" -ForegroundColor Gray