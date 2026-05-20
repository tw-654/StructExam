#!/bin/bash

# JMeter 高并发测试启动脚本
# 测试场景：多用户同时提交代码

echo "=== JMeter 代码提交高并发测试 ==="
echo ""

# 1. 创建必要的目录
mkdir -p results scripts logs

# 2. 启动 JMeter 分布式测试
echo "启动 JMeter 分布式测试..."
echo "Master 节点: jmeter-master"
echo "Slave 节点: jmeter-slave1, jmeter-slave2"
echo "并发用户数: 100"
echo "循环次数: 5"
echo "加速时间: 60秒"
echo ""

docker-compose up -d

# 3. 等待测试完成
echo "测试进行中...请等待结果生成"
sleep 30

# 4. 查看测试结果
echo ""
echo "=== 测试结果 ==="
cat results/result.jtl | head -20

# 5. 停止容器
echo ""
echo "停止 JMeter 容器..."
docker-compose down

echo ""
echo "测试完成！报告已生成在 results/report 目录"
