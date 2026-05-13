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
