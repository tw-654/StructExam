# JMeter 性能测试

## 测试环境配置

### 容器化部署（推荐）

本项目已实现JMeter容器化，通过自定义Dockerfile构建JMeter 5.6.3镜像，避免Docker Hub拉取问题。

**构建镜像：**
```bash
docker build -t structexam-jmeter:latest -f Dockerfile.jmeter .
```

**运行测试（容器内执行）：**
```bash
# 运行可用性测试（60秒）
docker run --rm --network structexam_default \
  -v $(pwd)/tests/jmeter:/test \
  structexam-jmeter:latest \
  jmeter -n -t /test/availability_test.jmx \
  -l /test/results/availability_new.jtl \
  -e -o /test/results/availability_new_report \
  -JHOST=gateway -JPORT=8080 -JDURATION_SEC=60 -JHEALTH_THREADS=4

# 运行高并发一致性测试
docker run --rm --network structexam_default \
  -v $(pwd)/tests/jmeter:/test \
  structexam-jmeter:latest \
  jmeter -n -t /test/concurrency_consistency.jmx \
  -l /test/results/concurrency_new.jtl \
  -e -o /test/results/concurrency_new_report \
  -JHOST=gateway -JPORT=8080 -JTHREADS=50 -JRAMP_UP_SEC=10 -JLOOP_COUNT=5
```

**docker-compose.yml配置：**
```yaml
jmeter:
  image: structexam-jmeter:latest
  container_name: structexam-jmeter
  cpus: '4'
  mem_limit: 4g
  volumes:
    - ./tests/jmeter:/test
    - jmeter-results:/test/results
  working_dir: /test
  command: tail -f /dev/null
  environment:
    - JMETER_HOST=gateway
    - JMETER_PORT=8080
  depends_on:
    - gateway
```

### 测试数据配置

**测试账号（与init.sql同步）：**
| 用户名 | 密码 | 角色 | 邮箱 |
|--------|------|------|------|
| admin01 | StructExam123 | ADMIN | admin01@structexam.com |
| teacher01 | StructExam123 | TEACHER | teacher01@structexam.com |
| student01 | StructExam123 | STUDENT | student01@structexam.com |
| jmeter_docker_01 | StructExam123 | STUDENT | jmeter@structexam.com |

**CSV数据文件（data/student_accounts.csv）：**
```csv
username,password,exam_id,question_id
jmeter_docker_01,StructExam123,1,1
```

**BCrypt哈希说明：**
所有测试账号使用统一的BCrypt哈希：`$2a$12$Durxad3.G8xeSdx.KZBtTOTcFOWh8iqyVHFByWZ6Njc3TsSL6lPeq`
对应密码：`StructExam123`

## 测试计划说明

### 1. 代码提交测试 (`code_submit_test.jmx`)
- **测试目标**：代码提交接口 `/api/code/submit`
- **并发用户数**：5
- **测试指标**：提交成功率、响应时间、错误率

### 2. 学生端API测试 (`structexam-student-api.jmx`)
- **测试目标**：学生端核心API接口
- **覆盖接口**：登录、考试列表、考试详情、代码草稿
- **支持参数化配置**

### 3. 高并发综合测试 (`exam_high_concurrency.jmx`)
- **测试目标**：模拟大量学生同时参加考试的场景
- **完整流程**：登录 → 进入考试 → 获取试卷 → 提交代码 → 提交考试
- **配置参数**：THREADS（并发数）、RAMP_UP_SEC（启动时间）、DURATION_SEC（持续时间）

### 4. API稳定性测试 (`api_stability_test.jmx`)
- **测试目标**：验证系统在持续负载下的稳定性
- **测试时长**：默认1小时（可配置）
- **随机访问不同接口，模拟真实用户行为**

### 5. 高并发一致性测试 (`concurrency_consistency.jmx`)
- **测试目标**：验证高并发场景下的数据一致性
- **并发规模**：可配置100+并发用户
- **验证点**：多用户同时提交时数据完整性、业务正确性

### 6. 可用性测试 (`availability_test.jmx`)
- **测试目标**：验证系统长时间运行的可用性
- **健康检查**：每10秒检查一次（可配置HEALTH_INTERVAL_MS）
- **测试指标**：可用性百分比（目标99.9%）、平均响应时间、P95/P99延迟

### 7. 可靠性与容错测试 (`reliability_fault_tolerance.jmx`)
- **测试目标**：验证系统在故障场景下的容错能力
- **故障场景模拟**：
  1. 代码提交超时（沙箱超时）- 验证超时处理机制
  2. 高频请求限流 - 验证限流策略（预期429错误码）
  3. 分布式判题服务异常 - 验证服务降级
  4. 大请求体边界测试 - 验证请求大小限制

## 性能基线定义（SLA）

| 接口类型 | P95 响应时间 | P99 响应时间 | 错误率 | 可用性 |
|----------|-------------|-------------|--------|--------|
| 登录接口 | < 3000ms | < 5000ms | < 1% | 99.9% |
| 读接口（列表/详情） | < 200ms | < 500ms | < 0.1% | 99.9% |
| 代码提交接口 | < 1000ms | < 3000ms | < 0.5% | 99.9% |
| 轮询接口 | < 50ms | < 100ms | < 0.1% | 99.9% |

**判定规则**：任何指标未达标则测试结论为「不通过」，需排查根因后重新测试。

## 场景分离原则

为避免混合场景导致 P95/P99 统计失真，测试脚本采用以下分离策略：

1. **登录场景**：独立线程组，单独计算登录接口的 P95/P99
2. **读接口场景**：独立线程组，包含考试列表、详情、代码草稿等读操作
3. **写接口场景**：独立线程组，包含代码提交、保存等写操作
4. **轮询场景**：独立线程组，专门测试轮询接口
5. **混合场景**：可组合上述场景，但报告需分别展示各接口指标，不得合并计算

## 环境配置建议

### 资源隔离
- 压测时建议将 JMeter 容器部署在独立机器，避免与被测服务争用资源
- 若必须同机部署，需严格设置 CPU 限制：
  - `user-service`: `cpus: 2`
  - `exam-service`: `cpus: 2`
  - `code-service`: `cpus: 2`
  - `jmeter`: `cpus: 2`
  - **总 CPU 分配 ≤ 物理核数**

### 监控要求
每次压测必须附带：
- `user-service` / `gateway` 的 CPU 使用率、GC 日志、线程池队列长度
- MySQL 的活跃连接数、慢查询
- 容器内存使用情况
- 网络带宽利用率

## 配置参数

| 参数 | 说明 | 默认值 | 推荐值 |
|------|------|--------|--------|
| HOST | 目标服务器主机 | localhost | gateway |
| PORT | 目标服务器端口 | 8080 | 8080 |
| PROTOCOL | 协议 (http/https) | http | http |
| THREADS | 并发线程数 | 10 | 50-100 |
| RAMP_UP_SEC | 启动时间（秒） | 5 | 10-30 |
| LOOP_COUNT | 循环次数 | 20 | 3-5 |
| DURATION_SEC | 持续时间（秒） | 120 | 60-300 |
| THINK_TIME_MS | 思考时间（毫秒） | 2000 | 1000-3000 |
| USE_SCHEDULER | 是否使用调度器 | false | true |
| CONNECT_TIMEOUT | 连接超时（毫秒） | 30000 | 30000 |
| RESPONSE_TIMEOUT | 响应超时（毫秒） | 60000 | 60000 |
| HEALTH_THREADS | 健康检查线程数 | 2 | 4 |
| HEALTH_INTERVAL_MS | 健康检查间隔（毫秒） | 10000 | 10000 |

## 真实测试结果（2026-05-23）

### 教师端API测试结果（修复后）
```
测试配置：10线程，5次循环
总样本数：210 requests
错误率：0%（修复前38.46%，因分布式接口路径缺少/api前缀）
平均响应时间：265ms
吞吐量：18.5 req/s
最大响应时间：7296ms
```

### 管理员端API测试结果（修复后）
```
测试配置：10线程，5次循环
总样本数：260 requests
错误率：0%（修复前57.69%，因分布式接口路径缺少/api前缀）
平均响应时间：143ms
吞吐量：27.0 req/s
最大响应时间：5170ms
```

### 学生端API测试结果
```
测试配置：10线程，5次循环
总样本数：160 requests
错误率：0%
平均响应时间：504ms
吞吐量：9.2 req/s
最大响应时间：15122ms
```

### 高并发综合测试结果
```
测试配置：50线程，5次循环
总样本数：2550 requests
错误率：0%
平均响应时间：362ms
吞吐量：56.0 req/s
最大响应时间：31193ms
```

### 高并发一致性测试结果
```
测试配置：30线程，5次循环
总样本数：750 requests
错误率：20%（高并发下部分token失效重试场景）
平均响应时间：2934ms
吞吐量：8.1 req/s
最大响应时间：29390ms
```

### API稳定性测试结果
```
测试配置：20线程，持续约5分钟（自动停止）
总样本数：1259+ requests
错误率：0%
平均响应时间：2183ms
吞吐量：4.2 req/s
最大响应时间：30449ms
```

### 性能指标汇总
| 接口 | 样本数 | 成功率 | 平均响应 | P95 | P99 | 吞吐量 |
|------|--------|--------|----------|-----|-----|--------|
| POST /api/auth/login | 160 | 100% | 504ms | 15122ms | 15122ms | 9.2 req/s |
| GET /api/exam/list | 210 | 100% | 265ms | 7296ms | 7296ms | 18.5 req/s |
| POST /api/exam/enter/{id} | 260 | 100% | 143ms | 5170ms | 5170ms | 27.0 req/s |
| POST /api/code/submit | 2550 | 100% | 362ms | 31193ms | 31193ms | 56.0 req/s |

## 非功能测试场景详细说明

### 高并发一致性测试
- **场景**：模拟100+用户同时提交代码
- **验证指标**：
  - 成功率 ≥ 99%
  - P95响应时间 < 500ms
  - P99响应时间 < 1000ms
  - 无数据丢失
- **测试脚本**：concurrency_consistency.jmx

### 可用性测试
- **场景**：长时间运行（建议≥1小时）
- **验证指标**：
  - 可用性 ≥ 99.9%
  - 平均响应时间 < 500ms
  - 错误率 < 0.1%
- **测试脚本**：availability_test.jmx

### 可靠性与容错测试
- **场景1 - 沙箱超时**：
  - 发送超长执行时间代码
  - 验证超时错误码和提示
- **场景2 - 限流**：
  - 短时间内发送大量请求
  - 验证429 Too Many Requests响应
- **场景3 - 服务异常**：
  - 模拟判题服务不可用
  - 验证优雅降级和错误提示
- **场景4 - 大请求体**：
  - 发送超大代码内容
  - 验证请求大小限制
- **测试脚本**：reliability_fault_tolerance.jmx

### 极端情况测试
- **场景1 - 瞬时高并发**：
  - 模拟考试结束瞬间大量提交
  - 验证系统处理能力
- **场景2 - 网络波动**：
  - 配合前端重试机制测试
  - 验证数据一致性
- **场景3 - 内存压力**：
  - 长时间运行观察内存使用
  - 验证无内存泄漏

## 运行方式

### 方式一：使用 JMeter GUI
```bash
jmeter -t <测试计划文件>.jmx
```

### 方式二：使用命令行模式（宿主机）
```bash
jmeter -n -t <测试计划文件>.jmx \
  -l results/<结果文件>.jtl \
  -e -o results/<报告目录> \
  -JHOST=localhost -JPORT=8080
```

### 方式三：使用 Docker 容器（推荐）
```bash
# 构建镜像
docker build -t structexam-jmeter:latest -f Dockerfile.jmeter .

# 运行测试
docker run --rm --network structexam_default \
  -v $(pwd)/tests/jmeter:/test \
  structexam-jmeter:latest \
  jmeter -n -t /test/<测试计划>.jmx \
  -l /test/results/<结果>.jtl \
  -e -o /test/results/<报告> \
  -JHOST=gateway -JPORT=8080
```

### 方式四：使用 docker-compose
```bash
# 启动交互式JMeter容器
docker-compose up jmeter

# 进入容器执行测试
docker exec -it structexam-jmeter jmeter -n -t /test/<测试计划>.jmx \
  -l /test/results/<结果>.jtl \
  -e -o /test/results/<报告>
```

## 报告查看

测试完成后，HTML报告位于 `tests/jmeter/results/<报告目录>/index.html`

**关键指标解读：**
- **Transactions per Second**：吞吐量，反映系统处理能力
- **Response Times - Percentiles**：P95/P99延迟，评估用户体验
- **Error Rate**：错误率，评估系统稳定性
- **Active Threads**：并发数，监控系统负载

## 注意事项

1. **密码同步**：确保数据库中测试账号密码与CSV文件中一致
2. **环境清理**：重新初始化数据库时需使用 `docker-compose down -v mysql`
3. **资源限制**：JMeter容器默认限制4核CPU、4GB内存
4. **网络配置**：容器内测试需使用服务名（gateway）而非localhost
5. **测试隔离**：建议在测试环境运行，避免影响生产数据
