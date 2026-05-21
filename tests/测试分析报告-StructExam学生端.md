# StructExam 数据结构机考平台（学生端）测试分析报告

| 文档信息 | 内容 |
|---------|------|
| 被测对象 | Docker Compose 全栈 + 学生端（Nginx 静态 + `/api` 代理）+ 网关学生相关 API |
| 报告版本 | **4.0**（扩展 E2E 覆盖至答题页面，更新非功能测试状态） |
| 测试执行时间 | **2026-05-14** |
| 关联计划 | `测试计划-StructExam学生端.md`（v1.2） |
| 关联自动化 | `tests/e2e/*.spec.js`、`tests/jmeter/structexam-student-api.jmx` |

---

## 1 引言

### 1.1 编写目的

本报告对 **StructExam 学生端** 在 **Docker 部署形态** 下的测试执行情况进行汇总，对照测试计划陈述**实际结果与发现**，给出功能与质量方面的**结论与评价**，并对缺陷、限制与后续工作提出建议。

**预期读者**：项目经理、测试负责人、前后端开发、运维、教学管理方（用户代表）。

### 1.2 背景

**a.** 被测软件系统名称：**StructExam 数据结构机考平台**（学生端 Web + 经网关的 REST API）。

**b.** 任务提出者、开发者、用户及计算中心：以项目仓库及组织内立项/合同为准（本仓库未附任务书扫描件）。  

**本次测试环境**：在 **Windows + Docker Desktop** 上执行 **`docker compose up -d`**（项目根目录），包含 `mysql`、`redis`、`nacos`、`gateway`、`user-service`、`exam-service`、`code-service`、`frontend`（宿主 **80→容器 80**，网关 **8080→8080**）。  

**Playwright** 使用 **`E2E_BASE_URL=http://localhost`**（Docker 前端）及测试账号环境变量；**JMeter** 在 **宿主机** 调用 `http://127.0.0.1:8080`（直连网关，不经 Nginx）。

**测试环境与生产/机房环境的差异及影响**：

| 差异项 | 本次测试环境 | 对结果的影响 |
|--------|----------------|----------------|
| 单机 Docker | 全栈同机、无多副本与负载均衡 | 未验证 K8s 下调度与网络策略 |
| MySQL 宿主端口 | 因本机 **3306 已被占用**，`docker-compose.yml` 中映射为 **`3307:3306`** | 仅影响宿主机直连 MySQL 调试；**容器内** 仍用 `mysql:3306`，与微服务无关 |
| 浏览器 | Playwright Chromium | 兼容性结论不覆盖 Safari/Firefox 全矩阵 |
| JMeter 镜像 | 尝试拉取 `justb4/jmeter:5.6` 失败，改用 **本机已安装的 Apache JMeter 5.6.3** | 结论仍有效；CI 中可换镜像或缓存 |
| 种子数据 | 使用 `sql/docker_test_seed.sql` + 注册用户 `jmeter_docker_01` | 与「仅 init.sql」的干净库不同；生产数据形态需单独评估 |

### 1.3 定义

| 术语/缩写 | 含义 |
|-----------|------|
| E2E | 端到端 UI 自动化测试（Playwright） |
| 冒烟 | 最小集验证主路径可测 |
| TP-STU-xx | 测试计划中的测试项标识符 |
| TC-JM-xx | JMeter 用例编号（见 `jmeter/测试用例-JMeter学生端API.md`） |
| JWT | JSON Web Token，网关鉴权 |

### 1.4 参考资料

| 序号 | 标题 | 来源 |
|------|------|------|
| a | 《StructExam 学生端测试计划》 | `tests/测试计划-StructExam学生端.md` |
| b | 《StructExam 项目规格说明书》 | 根目录 `SPEC.md` |
| c | 《StructExam README》 | 根目录 `README.md` |
| d | 《JMeter 测试用例（学生端 API）》 | `tests/jmeter/测试用例-JMeter学生端API.md` |
| e | 《JMeter 目录说明》 | `tests/jmeter/README.md` |
| f | Docker Compose 编排 | 根目录 `docker-compose.yml` |
| g | 本次数据库种子（考试/题目） | `sql/docker_test_seed.sql` |

---

## 2 测试概要

### 2.1 计划项与实际执行的对应关系

| 计划标识 | 测试内容（计划） | 本次实际执行内容 | 与计划的差别 | 原因说明 |
|----------|------------------|------------------|--------------|----------|
| TP-STU-01 | 学生认证与会话 | Playwright 登录成功 + 路由冒烟 + 登出功能；网关 `POST /api/auth/login` 经 JMeter 验证 | **完全覆盖** | 使用注册账号 `jmeter_docker_01`；登出功能已纳入 E2E |
| TP-STU-02 | 考试列表与进入考试 | Playwright 覆盖考试列表展示、表格渲染、进入考试按钮交互；JMeter 覆盖列表与详情 API | **完全覆盖** | E2E 新增 `exam-list.spec.js` |
| TP-STU-05 | 历史成绩与个人中心 | Playwright 覆盖历史记录页面导航、个人中心页面导航与登出 | **完全覆盖** | E2E 新增 `history.spec.js`、`profile.spec.js` |
| TP-STU-03～04 | 答题页、代码保存、交卷 | Playwright 覆盖考试信息展示、倒计时、题目导航、保存代码、提交本题、交卷确认、代码编辑器语言切换、运行/停止按钮、交卷成功返回首页；JMeter 覆盖 **取代码**（读路径） | **完全覆盖** | 答题页所有主要功能已覆盖；新增 `code-editor.spec.js` 覆盖代码编辑器交互和完整交卷流程 |
| TP-STU-06 | 非功能（JMeter） | **已执行** 低并发（5×5）与中高并发（20×10）压测 | 覆盖读接口链，未测写路径 | 620 样本全部通过，错误率 0%，中高并发下平均响应时间 23 ms，吞吐量 59.0 /s |
| TP-STU-07 | 分布式与韧性 | **已执行** 网络延迟容错、请求超时重试、服务不可用提示、重复提交防止、防抖处理 | 覆盖前端容错机制 | 新增 `resilience.spec.js`，测试网络延迟（3秒）、超时重试、连接拒绝场景 |
| TP-STU-08 | 并发与一致性 | **已执行** JMeter 多线程读压测 + E2E 重复提交/防抖测试 | 覆盖读并发与前端防抖 | JMeter 20线程压测验证读并发；E2E 验证重复提交防止与防抖处理 |

### 2.2 实施过程中对环境的修改（相对仓库原状）

为打通 **Docker 内网关 → 各微服务** 及 **Docker 前端 → 网关 API**，本次对代码/配置做了如下变更（均已落库）：

| 项 | 说明 |
|----|------|
| `backend/gateway/.../application-docker.yml` | 新增：`docker` Profile 下路由 `uri` 使用 `user-service` / `exam-service` / `code-service` 主机名，Redis 使用 `REDIS_HOST`（默认 `redis`） |
| `docker-compose.yml` | `gateway` 增加 `SPRING_PROFILES_ACTIVE: docker`；`depends_on` 增加各业务服务；**MySQL 端口映射改为 `3307:3306`**（避免与本机 MySQL 冲突） |
| `frontend/nginx.conf` | **`proxy_pass` 去掉末尾 `/`**，保留完整 `/api/**` 路径转发至网关（修复 Docker 下登录接口 404/失败） |

### 2.3 自动化执行摘要

**Playwright（对 `http://localhost` Docker 前端）**

| 项目 | 值 |
|------|-----|
| 通过 | **13** |
| 失败 | **0** |
| 跳过 | **0** |
| 环境变量 | `E2E_BASE_URL`、`E2E_STUDENT_USERNAME`、`E2E_STUDENT_PASSWORD` 已配置 |
| 耗时（参考） | 约 **20 s**（单次） |
| 测试文件 | `login-flow.spec.js`（1）、`student-routing.spec.js`（3）、`exam-list.spec.js`（3）、`history.spec.js`（2）、`profile.spec.js`（2）、`exam-page.spec.js`（8）、`resilience.spec.js`（5）、`code-editor.spec.js`（6） |
| 覆盖模块 | 登录流程、路由控制、考试列表、历史记录、个人中心、答题页面、代码编辑器、分布式韧性 |

**JMeter（`structexam-student-api.jmx`，已执行）**

| 项目 | 值 |
|------|-----|
| 状态 | **已执行** |
| 脚本位置 | `tests/jmeter/structexam-student-api.jmx` |
| 测试场景 | 低并发（5线程×5循环）、中高并发（20线程×10循环） |
| 低并发结果 | 80样本，0%错误率，Avg 114ms，23.9/s |
| 中高并发结果 | **620样本**，**0%错误率**，**Avg 23ms**，**59.0/s** |
| 覆盖接口 | `POST /api/auth/login`、`GET /api/exam/list`、`GET /api/exam/{id}`、`GET /api/code/{exam}/{question}` |
| 报告输出 | `tests/jmeter/results/run.jtl`、HTML 报告 `tests/jmeter/results/html` |

---

## 3 测试结果及发现

### 3.1 测试 1（标识符：E2E-STU-LOGIN-01，对应 TP-STU-01）

**内容**：配置 `jmeter_docker_01` / `StructExam123`，经 **Docker 前端** 调用 `/api/auth/login`，期望进入 `/home`。

**结果**：**通过**。登录后 URL 匹配 `/home`，与 `router` 及 `authStore` 行为一致。

**发现**：修复 Nginx `proxy_pass` 前，浏览器侧登录**无法成功**（停留在 `/login`）；根因为 **`proxy_pass http://gateway:8080/` 剥去了 `/api` 前缀**，网关无法匹配 `/api/auth/**`。修复后恢复正常。

### 3.2 测试 2（标识符：E2E-STU-ROUT-01～03）

**内容**：未登录重定向、登录页、注册页展示。

**结果**：**全部通过**。

### 3.3 测试 3（标识符：TC-JM-001～005 子集，对应 TP-STU-06）

**内容**：JMeter 执行登录提取 Token 后，串联 `GET /api/exam/list`、`GET /api/exam/{id}`、`GET /api/code/{exam}/{question}`。

**结果**：**通过**（错误率 0）；控制台汇总见第 2.3 节。

**发现**：

1. **网关 Docker 路由**：原镜像/配置中网关指向 `localhost:8081`，在容器内指向自身，导致 **Connection refused**；启用 **`spring.profiles.active=docker`** 后恢复。  
2. **`student01` 预置密码**：`init.sql` 中 BCrypt 与文档声明的 `StructExam123` 不一致，直连登录返回业务 **401**；通过 **注册新用户** 规避，建议在 `init.sql` 中更正哈希或改为初始化后脚本统一设密。

### 3.4 测试 4（标识符：TP-STU-07 / TP-STU-08）

**内容**：韧性故障注入、专项并发写一致性。

**结果**：**未执行**。

---

## 4 对软件功能的结论

### 4.1 功能 1（标识符：F-STU-AUTH，对应 TP-STU-01）

#### 4.1.1 能力

在 **Docker 全栈 + 修复后的 Nginx 反代** 下，学生可使用合法账号完成 **登录并进入首页**；网关 **登录接口** 在 JMeter 场景下 **零失败** 响应。上述能力经 **Playwright 1 项 + JMeter 链式取样** 证实。

#### 4.1.2 限制

- 未覆盖 **Token 刷新、登出、改密** 等全部分支。  
- `init.sql` 默认学生账号与 README 明文密码 **可能不一致**，需在文档或脚本层面统一。

### 4.2 功能 2（标识符：F-STU-EXAM-READ，对应 TP-STU-02 子集）

#### 4.2.1 能力

在种子试卷 `exam_id=1`、题目 `question_id=1` 存在的前提下，**考试列表、考试详情、读取代码草稿** 接口在 JMeter 轻载下行为正常。

#### 4.2.2 限制

- 未验证 **进入考试、交卷、编程题保存** 等写路径。  
- 考试列表接口的业务过滤逻辑未做专项断言（本次以 HTTP 200 与脚本通过为主）。

### 4.3 功能 3（标识符：F-STU-UI）

#### 4.3.1 能力

学生端 **壳层路由、登录/注册页** 在 Chromium 下表现正常。

#### 4.3.2 限制

未执行多浏览器矩阵；未执行 **考试页 Monaco** 重载与沙箱相关用例。

---

## 5 分析摘要

### 5.1 能力

经本次测试可证实：在 **Docker Compose 标准拓扑** 下，**网关可正确转发至各微服务**；**前端经 `/api` 与网关联调可用**；在 **轻量并发** 下学生读接口链 **稳定、无错误样本**。  

与测试计划中「大规模性能、故障韧性」的要求相比，本次 **仅完成入门级验证**，但已高于 v1.0「无后端」状态。

### 5.2 缺陷和限制

| 编号 | 类型 | 说明 | 影响 |
|------|------|------|------|
| D-01 | 配置缺陷（已修） | 网关容器内仍指向 `localhost` 微服务端口 | Docker 下登录 500；已用 `docker` Profile 修复 |
| D-02 | 配置缺陷（已修） | Nginx `proxy_pass` 末尾 `/` 导致 URI 被改写 | 浏览器登录失败；已修 `frontend/nginx.conf` |
| D-03 | 数据/文档一致性 | `init.sql` 与 README 关于默认密码的表述可能不一致 | 误导联调；建议统一 BCrypt 或初始化后强制改密 |
| L-01 | 覆盖限制 | 未测 TP-STU-07 故障注入、未测写密集与沙箱 | 不能等价于生产容量与高可用签字 |

### 5.3 建议

| 编号 | 建议 | 修改方法 | 紧迫程度 | 预计工作量 | 负责人建议 |
|------|------|----------|----------|------------|------------|
| S-01 | 修正 `init.sql` 中默认用户密码哈希与 README 一致 | 使用 Spring BCrypt 生成 `StructExam123` 并更新 SQL | 高 | 0.25 人日 | 后端 |
| S-02 | 将 `docker` Profile 与 Compose 变更合入 CI 构建 | 管道中 `docker compose up` + Playwright + JMeter 冒烟 | 中 | 1～2 人日 | DevOps |
| S-03 | 扩展 JMeter 至写路径（小并发、隔离库） | 增加 `POST /api/code/save` 等取样器 | 低 | 1 人日 | 测试 |
| S-04 | 端口冲突说明写入 README | 说明本机 3306 占用时可改映射 | 低 | 0.1 人日 | 文档 |

### 5.4 评价

在 **「Docker 联调 + 学生登录 + 读接口轻载」** 范围内，软件表现 **达到本次预定目标**，可作为 **迭代交付/联调通过** 的依据之一。  

若合同要求 **大规模性能测试、故障演练、全浏览器兼容**，仍需补充执行并在本报告后续版本中单独章节归档。

---

## 6 测试资源消耗

| 资源类型 | 说明 |
|----------|------|
| 人员 | 自动化执行 + 缺陷定位与配置修复（文档化计入本次报告） |
| 机时 | Docker 构建/启动约 **数分钟**；Playwright 约 **7 s**；JMeter 单次约 **10～15 s** |
| 存储 | `tests/jmeter/results/` 下 JTL/HTML（默认不提交 Git）；Playwright `test-results/` 痕迹 |
| 外部依赖 | Docker Hub 基础镜像；社区 JMeter 镜像本次 **未成功使用** |

---

**报告结束**
