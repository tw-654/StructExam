# JMeter：StructExam 学生端 API（经网关）

对应测试计划 **TP-STU-06（非功能）**；脚本仅压 **读多写少** 的混合场景，避免默认对写库路径加压。

## 使用 Docker 启动后端（可与 JMeter 联调）

**可以**：网关与微服务、MySQL、Redis、Nacos 已在仓库根目录 **`docker-compose.yml`** 中定义，Gateway 映射宿主 **`8080:8080`**。你在本机未起 Java 进程时，用 Docker 拉起整套后端后，再用 JMeter 打 `http://localhost:8080` 即可。

在项目根目录执行（首次建议带构建）：

```powershell
Set-Location d:\code\StructExam
docker compose up -d --build
```

等待 `mysql`、`nacos` 健康后，`gateway` / `user-service` / `exam-service` / `code-service` 会陆续就绪。查看状态：

```powershell
docker compose ps
docker compose logs -f gateway --tail 50
```

**数据说明**：`docker-compose` 默认挂载 **`sql/init.sql`** 初始化库表与用户。JMeter/联调建议再执行 **`sql/docker_test_seed.sql`**（插入 `exam_id=1` 与一道编程题），并使用 `data/student_accounts.csv` 中账号（默认 **`jmeter_docker_01` / `StructExam123`**，需先 **`POST /api/auth/register`** 注册一次，或自行改 CSV）。`init.sql` 内 `student01` 的 BCrypt 与 README 明文可能不一致，登录失败时优先用注册接口造数。

**与纯本地 Java 的差异**：容器内使用 `application.yml` 中的 `NACOS_HOST` / `MYSQL_HOST` 等环境变量；行为应与 README「Docker Compose 部署」一致。若本机已占用 3306/6379/8080，需先释放端口或改 compose 映射后再起。

---

## 前置条件

1. 已启动 **Gateway（8080）** 及 `user-service`、`exam-service`、`code-service`（**本地 Java 或 Docker 均可**），且路由与 `backend/gateway` 中 `application.yml` 一致。
2. 数据库已初始化，存在与 CSV 一致的 **学生账号**、**试卷 id**、**题目 id**（见 `sql/init.sql`、`sql/insert_test_data.sql` 等；默认示例账号见根目录 `README.md`）。
3. **JMeter**：安装 [Apache JMeter](https://jmeter.apache.org/download_jmeter.cgi) 5.5+ 并加入 `PATH`，或使用下文 **Docker 内 JMeter** 方式，无需本机安装。

### JMeter 是否必须也放在 Docker 里？

**不必。** 常见做法是：**后端用 Docker**，**JMeter 装在宿主机**，脚本里 `HOST=localhost`、`PORT=8080`（默认即可），请求经宿主端口进入 `structexam-gateway` 容器。

若希望 **JMeter 也跑在容器内**（机器上未装 GUI/CLI 时），在 **Docker Desktop（Windows/macOS）** 上可把网关地址写成宿主映射，例如使用镜像 **`justb4/jmeter`**（社区镜像，版本号以 Hub 为准）：

```powershell
New-Item -ItemType Directory -Force -Path "d:\code\StructExam\tests\jmeter\results" | Out-Null
docker run --rm `
  -v "d:/code/StructExam/tests/jmeter:/t" `
  -w /t `
  justb4/jmeter:5.6 `
  -JHOST=host.docker.internal -JPORT=8080 -JPROTOCOL=http `
  -n -t structexam-student-api.jmx -l results/run.jtl -e -o results/html `
  -JTHREADS=10 -JRAMP_UP_SEC=5 -JLOOP_COUNT=20
```

说明：`host.docker.internal` 让容器内进程访问 **宿主机** 上监听的 `8080`（即 compose 映射出来的 Gateway）。**Linux 原生 Docker** 若无该主机名，可改用 **`--network host`** 打 `127.0.0.1:8080`，或把 JMeter 作为 **与 gateway 同一 Docker Compose 工程中的服务** 加入同一网络后，将 `HOST` 设为服务名 `gateway`（需自行编写 `docker-compose` 片段，本仓库未内置该 sidecar）。

---

## 运行方式

在 **本目录** 下执行（保证 `data/student_accounts.csv` 相对路径正确；**后端已由 Docker 映射 8080 时，默认即打本机网关**）：

```powershell
Set-Location d:\code\StructExam\tests\jmeter
jmeter -n -t structexam-student-api.jmx -l results\run.jtl -e -o results\html -JTHREADS=10 -JRAMP_UP_SEC=5 -JLOOP_COUNT=20
```

常用 **JVM 属性**（覆盖 `user.properties.example` 中的同名项）：

| 属性 | 含义 | 默认 |
|------|------|------|
| `HOST` | 网关主机 | localhost |
| `PORT` | 网关端口 | 8080 |
| `PROTOCOL` | http / https | http |
| `THREADS` | 并发线程数 | 10 |
| `RAMP_UP_SEC` | 爬坡时间（秒） | 5 |
| `LOOP_COUNT` | 每线程循环次数（每次循环执行列表+详情+取代码） | 20 |

示例（HTTPS 与 50 并发）：

```powershell
jmeter -n -t structexam-student-api.jmx -l results\run.jtl -e -o results\html -JPROTOCOL=https -JHOST=api.example.com -JPORT=443 -JTHREADS=50 -JRAMP_UP_SEC=30
```

## 文件说明

| 文件 | 说明 |
|------|------|
| `structexam-student-api.jmx` | 主测试计划 |
| `data/student_accounts.csv` | 每行一组 `username,password,exam_id,question_id`；首行为表头 |
| `user.properties.example` | 属性示例，可复制为 `user.properties` 后配合 `jmeter -q` 使用 |
| `测试用例-JMeter学生端API.md` | 用例编号、步骤、预期与脚本对应关系 |

## 结果目录

`results/` 下 `*.jtl` 与 HTML 报告建议加入版本与日期，勿提交到 Git（见 `.gitignore`）。

## 安全提示

仅在 **授权** 的测试/预发环境加压；勿对生产无审批压测。写接口扩展见用例文档附录。
