# JMeter 性能测试

## 测试计划说明

### 1. 代码提交测试 (`code_submit_test.jmx`)
- 测试目标：代码提交接口 `/api/code/submit`
- 并发用户数：5
- 用于验证代码提交功能的正确性和基本性能

### 2. 学生端API测试 (`structexam-student-api.jmx`)
- 测试目标：学生端核心API接口
- 包含登录、考试列表、考试详情、代码草稿等接口
- 支持参数化配置

### 3. 高并发综合测试 (`exam_high_concurrency.jmx`)
- 测试目标：模拟大量学生同时参加考试的场景
- 包含完整的考试流程：登录 → 进入考试 → 获取试卷 → 提交代码 → 提交考试
- 支持配置并发数、循环次数、压测时长

### 4. API稳定性测试 (`api_stability_test.jmx`)
- 测试目标：验证系统在持续负载下的稳定性
- 长时间运行测试（默认1小时）
- 随机访问不同接口，模拟真实用户行为

### 5. 高并发一致性测试 (`concurrency_consistency.jmx`)
- 测试目标：验证高并发场景下的数据一致性
- 模拟100+并发用户同时提交代码
- 验证多用户同时操作时数据正确性和业务完整性

### 6. 可用性测试 (`availability_test.jmx`)
- 测试目标：验证系统长时间运行的可用性
- 包含健康检查线程（每10秒检查一次）
- API随机访问验证，确保系统持续可用
- 可配置连接超时和响应超时时间

### 7. 可靠性与容错测试 (`reliability_fault_tolerance.jmx`)
- 测试目标：验证系统的容错能力和可靠性
- **故障场景模拟**：
  - 正常请求处理
  - 代码提交超时（沙箱超时）
  - 高频请求限流场景
  - 分布式判题服务异常
  - 大请求体边界测试
- 验证系统在各种故障场景下的优雅降级能力

## 运行方式

### 方式一：使用 JMeter GUI
```bash
jmeter -t <测试计划文件>.jmx
```

### 方式二：使用命令行模式
```bash
jmeter -n -t <测试计划文件>.jmx -l results/<结果文件>.jtl -e -o results/<报告目录>
```

### 方式三：使用 Docker
```bash
docker-compose up
```

## 配置参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| HOST | 目标服务器主机 | localhost |
| PORT | 目标服务器端口 | 8080 |
| PROTOCOL | 协议 (http/https) | http |
| THREADS | 并发线程数 | 10 |
| RAMP_UP_SEC | 启动时间（秒） | 5 |
| LOOP_COUNT | 循环次数 | 20 |
| DURATION_SEC | 持续时间（秒） | 120 |
| THINK_TIME_MS | 思考时间（毫秒） | 2000 |
| USE_SCHEDULER | 是否使用调度器 | false |
| CONNECT_TIMEOUT | 连接超时（毫秒） | 5000 |
| RESPONSE_TIMEOUT | 响应超时（毫秒） | 30000 |
| HEALTH_INTERVAL_MS | 健康检查间隔（毫秒） | 10000 |

## 运行示例

```bash
# 运行高并发测试，50个并发用户，30秒启动，运行2分钟
jmeter -n -t exam_high_concurrency.jmx -l results/high_concurrency.jtl -e -o results/high_concurrency_report -JTHREADS=50 -JRAMP_UP_SEC=30 -JDURATION_SEC=120

# 运行稳定性测试，20个并发用户，运行1小时
jmeter -n -t api_stability_test.jmx -l results/stability.jtl -e -o results/stability_report -JTHREADS=20 -JDURATION_SEC=3600 -JUSE_SCHEDULER=true

# 运行一致性测试，100个并发用户
jmeter -n -t concurrency_consistency.jmx -l results/concurrency.jtl -e -o results/concurrency_report -JTHREADS=100 -JLOOP_COUNT=5

# 运行可用性测试，持续2小时
jmeter -n -t availability_test.jmx -l results/availability.jtl -e -o results/availability_report -JDURATION_SEC=7200 -JUSE_SCHEDULER=true

# 运行容错测试，30个并发用户
jmeter -n -t reliability_fault_tolerance.jmx -l results/reliability.jtl -e -o results/reliability_report -JTHREADS=30 -JLOOP_COUNT=20
```

## 测试数据

学生账号数据存放在 `data/student_accounts.csv` 文件中，格式如下：
```csv
username,password,exam_id,question_id
student1,password123,1,1
student2,password123,1,1
```

## 非功能测试场景说明

### 高并发一致性测试
- **测试目标**：验证100+用户同时提交代码时的数据一致性
- **测试指标**：成功率、响应时间、数据完整性
- **验证点**：确保所有提交都被正确处理，不会丢失数据

### 可用性测试
- **测试目标**：验证系统在长时间运行（24小时+）下的可用性
- **测试指标**：可用性百分比（目标99.9%）、平均响应时间
- **验证点**：健康检查持续通过，API响应正常

### 可靠性与容错测试
- **测试目标**：验证系统在故障场景下的容错能力
- **测试场景**：
  1. 沙箱服务超时 - 验证超时处理机制
  2. 高频请求限流 - 验证限流策略
  3. 分布式节点故障 - 验证服务降级
  4. 大请求体处理 - 验证请求大小限制
- **验证点**：系统不会崩溃，返回合理的错误码（429、503、504）

## 注意事项

1. 运行测试前请确保目标服务已启动
2. 建议在测试环境运行，避免影响生产环境
3. 根据服务器性能调整并发数
4. 测试数据需与数据库中的实际数据一致
5. 建议配合监控工具（如Prometheus+Grafana）进行实时监控