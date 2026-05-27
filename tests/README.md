# tests 目录说明

本目录存放 **StructExam 学生端** 的测试计划与自动化测试代码。

## 文档

- `测试计划-StructExam学生端.md`：测试范围、计划、设计与评价准则（含 **非功能** TP-STU-06、**分布式与韧性** TP-STU-07、**并发一致性** TP-STU-08）。
- `测试分析报告-StructExam学生端.md`：测试执行结果汇总、结论与建议（国标式结构）。

当前仓库 **未** 内置 k6（JMeter 见下）。

**JMeter（学生端 API）**：见 **`tests/jmeter/`**（`structexam-student-api.jmx`、用例说明 `测试用例-JMeter学生端API.md`、运行说明 `README.md`）。

## 自动化（Playwright）

**前置**：在 `frontend` 目录执行 `npm install`，再执行 `npm run dev`，保证 `http://localhost:3000` 可访问（见 `frontend/vite.config.js`）。登录类用例会请求 `/api` 代理到网关；若仅跑路由冒烟，可不启后端。

在 `tests` 目录安装依赖并运行：

```bash
cd tests
npm install
npx playwright install chromium
npm run test:e2e
```

**CI 中由 Playwright 自动安装依赖并启动前端**（耗时较长）：

```bash
set E2E_START_WEB_SERVER=1
cd tests
npm run test:e2e
```

### 环境变量（可选）

配置后执行「真实账号登录 → 进入首页」类用例（见 `e2e/login-flow.spec.js`）：

| 变量 | 说明 |
|------|------|
| `E2E_BASE_URL` | 前端地址，默认 `http://localhost:3000` |
| `E2E_STUDENT_USERNAME` | 学生用户名 |
| `E2E_STUDENT_PASSWORD` | 学生密码 |

未配置用户名密码时，仅运行不依赖账号的冒烟用例（路由与登录页元素）。

## 测试最佳实践

### 性能测试

1. **场景分离原则**：登录、读接口、写接口、轮询应分别压测，避免混合场景导致 P95/P99 统计失真
2. **性能基线定义**：
   - 登录接口：P95 < 3秒，P99 < 5秒
   - 读接口：P95 < 200ms，P99 < 500ms
   - 代码提交：P95 < 1秒，P99 < 3秒
3. **环境隔离**：压测时 JMeter 容器应与被测服务隔离，避免资源争用
4. **超时配置**：连接超时设为 30秒，响应超时设为 60秒
5. **监控要求**：每次压测需记录 CPU、内存、GC、慢查询等监控数据

### 业务断言覆盖

1. 所有接口必须验证业务码 `"code":200`
2. 登录接口需验证 token 非空
3. 列表接口需验证 data 数组存在
4. 写操作需验证返回的任务ID或状态

### 测试数据隔离

1. 并发测试使用独立考试ID，避免数据冲突
2. 使用 SetUp 线程组动态创建测试数据
3. 测试完成后清理测试数据

### 容错与韧性测试

1. 模拟服务延迟、超时、限流等故障场景
2. 验证系统优雅降级能力
3. 测量故障检测时间和恢复时间

### 安全测试

1. 验证 JWT 权限控制
2. 检查敏感信息泄露
3. 建议使用 OWASP ZAP 进行自动化安全扫描
