# JMeter 测试用例：StructExam 学生端 API（网关）

| 文档 | 内容 |
|------|------|
| 关联测试计划 | `../测试计划-StructExam学生端.md` **TP-STU-06** |
| 关联脚本 | `structexam-student-api.jmx` |
| 被测系统 | Gateway → User / Exam / Code 服务（学生 Token 经 `Authorization: Bearer`） |

---

## 1 测试范围与假设

- **在测**：登录 → 考试分页列表 → 考试详情 → 读取编程题草稿（GET `/api/code/{examId}/{questionId}`）。  
- **不在默认脚本中**：交卷、进入考试、写 Redis/DB 的保存代码等 **写路径**（避免压测污染与锁竞争）；若需扩展，见 **第 5 节附录**。  
- **假设**：`data/student_accounts.csv` 中的 `exam_id`、`question_id` 在数据库中真实存在，且该学生有权访问该试卷（否则详情接口可能返回业务错误，与实现一致）。

---

## 2 测试用例一览

| 用例编号 | 用例名称 | 优先级 | 类型 | JMeter 节点（取样器/控制器） | 预期结果 |
|----------|----------|--------|------|------------------------------|----------|
| TC-JM-001 | 网关可达与登录成功 | P0 | 冒烟 | `01 POST /api/auth/login`（位于「每线程仅登录一次」） | HTTP 200；响应体含 `"code":200`；JSONPath `$.data.token` 可提取非空 token |
| TC-JM-002 | 登录失败（负例，可选） | P2 | 负例 | 需自建副本计划或改 CSV 为错误密码 | HTTP 200 且 `code!=200`，或网关/服务返回 4xx/5xx；**当前主脚本不包含** |
| TC-JM-003 | 学生考试列表 | P0 | 功能+性能 | `02 GET /api/exam/list` | HTTP 200；列表结构可解析（`code=200`） |
| TC-JM-004 | 学生考试详情 | P0 | 功能+性能 | `03 GET /api/exam/{id}` | HTTP 200；返回详情 JSON |
| TC-JM-005 | 读取已保存代码 | P1 | 功能+性能 | `04 GET /api/code/{exam}/{question}` | HTTP 200；`data` 中含代码字段（可为空串） |
| TC-JM-006 | 多线程共享账号爬坡 | P1 | 性能 | 线程组 + `THREADS` / `RAMP_UP_SEC` | 无大面积连接拒绝；错误率在组织阈值内 |
| TC-JM-007 | 循环内重复读接口 | P1 | 性能 | `LOOP_COUNT` > 1 | Token 在循环内仍有效；若 JWT 过期导致 401，应缩短循环时间或增加刷新 Token 逻辑（脚本外处理） |
| TC-JM-008 | 多账号 CSV 轮询 | P1 | 并发 | `data/student_accounts.csv` 多行、`recycle=true` | 每线程按行取数；无串用户（各线程独立变量） |

---

## 3 详细步骤与预期（与脚本一一对应）

### TC-JM-001 登录并提取 Token

| 项 | 内容 |
|----|------|
| 前置条件 | 网关与用户服务正常；CSV 中用户名、密码正确（默认 `student01` / `StructExam123`，见根目录 `README.md`） |
| 请求 | `POST /api/auth/login`，`Content-Type: application/json`，Body：`{"username":"${username}","password":"${password}"}` |
| 后置 | JSON Extractor：`$.data.token` → 变量 `access_token` |
| 预期 | 响应码 200；响应数据包含 `"code":200`；`access_token` ≠ `TOKEN_EXTRACT_FAILED` |

### TC-JM-003～TC-JM-005 认证后只读链

| 项 | 内容 |
|----|------|
| 前置条件 | TC-JM-001 已成功；**HTTP 头** `Authorization: Bearer ${access_token}` 作用于后续取样器（脚本中位于登录 Once Only 之后） |
| TC-JM-003 | `GET /api/exam/list?pageNum=1&pageSize=10` → HTTP 200 |
| TC-JM-004 | `GET /api/exam/${exam_id}` → HTTP 200 |
| TC-JM-005 | `GET /api/code/${exam_id}/${question_id}` → HTTP 200 |

### TC-JM-006 / TC-JM-007 负载参数

| 项 | 内容 |
|----|------|
| 目的 | 观察延迟与错误率随并发、循环次数变化 |
| 操作 | 修改 `-JTHREADS`、`-JRAMP_UP_SEC`、`-JLOOP_COUNT`，保留 `-l` 与 `-e -o` 生成 HTML 报告 |
| 记录 | 归档 `run.jtl`、HTML 报告、当时 Git 提交号与数据库种子脚本版本 |

---

## 4 通过 / 失败判定（建议）

- **通过**：TC-JM-001、003、004、005 在约定并发与循环下 **失败率 0**（或低于项目阈值）；抽样检查响应时间分布（HTML 报告中的百分位）。  
- **失败**：出现大量 401/500、连接超时、或 `code` 非 200 的业务错误；需区分环境故障与缺陷。  
- **Token 过期**：若 `LOOP_COUNT` 极大导致 JWT 过期，属 **脚本边界**，可拆分为「短循环读压测」或扩展「刷新 Token」取样器（不在本仓库默认脚本中）。

---

## 5 附录：扩展写接口（慎用）

以下 **会写 Redis/DB**，仅建议在隔离环境、小并发下手工添加取样器，勿并入默认 CI 压测。

### A. 保存代码 `POST /api/code/save`

- **Header**：`Authorization: Bearer ${access_token}`，`Content-Type: application/json`  
- **Path**：`/api/code/save`  
- **Body 示例**：

```json
{
  "examId": ${exam_id},
  "questionId": ${question_id},
  "language": "java",
  "code": "// jmeter load ${__threadNum} ${__time(,)}"
}
```

- **预期**：HTTP 200，业务 `code=200`（与 `ApiResponse` 一致）。

### B. 进入考试 `POST /api/exam/enter/{id}`

- 路径：`/api/exam/enter/${exam_id}`，POST，需 Token。  
- 副作用：创建/更新考试记录；多线程重复进入需接受数据层行为（唯一约束等）以实际实现为准。

### C. 纯登录压测

- 可复制当前「线程组」，**去掉** Once Only 与后续取样器，仅保留登录取样器并增大 `LOOP_COUNT`，用于观察 **认证链路** 瓶颈；注意与账户锁定策略（若有）的关系。

---

**文档结束**
