# StructExam - 数据结构机考平台

基于 Spring Cloud Alibaba 的在线代码考试平台，支持登录认证、考试管理、代码编辑、自动交卷等功能。

## 技术栈

| 技术 | 说明 |
|------|------|
| 后端框架 | Spring Cloud Alibaba 2022.0.0.0 |
| 服务注册与配置 | Nacos 2.2.3 |
| 缓存 | Redis 7.x |
| 数据库 | MySQL 8.0 |
| 前端框架 | Vue.js 3 + Element Plus |
| 代码编辑器 | Monaco Editor |
| 容器化 | Docker + Docker Compose |

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        Gateway (端口 8080)                   │
│                    Spring Cloud Gateway                      │
│                  (统一入口, JWT 路由鉴权)                     │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  User Service │   │ Exam Service  │   │ Code Service  │
│   (端口 8081)  │   │  (端口 8082)  │   │  (端口 8083)  │
│               │   │               │   │               │
│ - 用户管理     │   │ - 试卷管理     │   │ - 代码存储     │
│ - 登录认证     │   │ - 题目管理     │   │ - 代码提交     │
│ - JWT Token   │   │ - 考试流程     │   │ - 自动阅卷     │
└───────────────┘   └───────────────┘   └───────────────┘
```

## 快速启动

### 方式一：Docker Compose 部署 (推荐)

```bash
# 克隆项目后，进入项目目录
cd StructExam

# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

服务启动后：
- 前端应用: http://localhost
- Nacos 控制台: http://localhost:8848/nacos (用户名/密码: nacos/nacos)
- API 网关: http://localhost:8080

### 方式二：本地开发环境

#### 前置条件

- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8.0
- Redis 7.x
- Nacos 2.2.3

#### 启动顺序

1. **启动 MySQL**
   ```bash
   mysql -u root -p < sql/init.sql
   ```

2. **启动 Nacos**
   ```bash
   # 下载 Nacos Server
   # 解压后运行
   cd nacos/bin
   ./startup.sh -m standalone  # Linux/Mac
   # 或
   startup.cmd -m standalone  # Windows
   ```

3. **启动后端服务**
   ```bash
   cd backend
   mvn clean install
   # 分别启动各个服务
   cd gateway && mvn spring-boot:run
   cd services/user-service && mvn spring-boot:run
   cd services/exam-service && mvn spring-boot:run
   cd services/code-service && mvn spring-boot:run
   ```

4. **启动前端**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## 功能列表

### 一期已完成功能

- [x] 用户注册与登录 (JWT 认证)
- [x] 考试列表查看
- [x] 进入考试功能
- [x] Monaco 代码编辑器
- [x] 代码实时保存 (Redis)
- [x] 手动保存代码
- [x] 单题提交
- [x] 自动交卷功能
- [x] 个人中心
- [x] 考试记录查看

### 二期待实现

- [ ] 代码沙箱环境
- [ ] 代码编译运行
- [ ] 测试用例判定
- [ ] 分布式任务调度
- [ ] 成绩统计分析

## 项目结构

```
StructExam/
├── docker-compose.yml          # Docker Compose 配置
├── Dockerfile.backend          # 后端服务 Dockerfile
├── Dockerfile.frontend         # 前端应用 Dockerfile
├── README.md                   # 项目说明
├── SPEC.md                    # 详细设计规格
├── backend/                    # 后端代码
│   ├── pom.xml                # 父 POM
│   ├── common/               # 公共模块
│   ├── gateway/              # API 网关
│   └── services/            # 业务服务
│       ├── user-service/    # 用户服务
│       ├── exam-service/    # 考试服务
│       └── code-service/    # 代码服务
├── frontend/                  # 前端代码
│   ├── package.json
│   ├── vite.config.js
│   ├── nginx.conf
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── router/
│       ├── views/
│       ├── api/
│       └── stores/
└── sql/
    └── init.sql              # 数据库初始化脚本
```

## API 接口

### 认证接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/auth/login | 用户登录 |
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/logout | 用户登出 |
| GET | /api/auth/userinfo | 获取用户信息 |
| PUT | /api/auth/password | 修改密码 |

### 考试接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/exam/list | 获取考试列表 |
| GET | /api/exam/{id} | 获取考试详情 |
| POST | /api/exam/enter/{id} | 进入考试 |
| POST | /api/exam/submit/{id} | 提交考试 |
| GET | /api/exam/record/{id} | 获取考试记录 |

### 代码接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/code/save | 保存代码 |
| GET | /api/code/{examId}/{questionId} | 获取代码 |
| POST | /api/code/submit | 提交代码 |
| POST | /api/code/submitAll/{examId} | 提交所有代码 |

## 数据库表结构

- `t_user` - 用户表
- `t_exam` - 试卷表
- `t_question` - 题目表
- `t_exam_record` - 考试记录表
- `t_code_submission` - 代码提交表
- `t_code_version` - 代码版本表

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | StructExam123 | 管理员 |
| teacher01 | StructExam123 | 教师 |
| student01 | StructExam123 | 学生 |

> 注意：上述密码为占位符，实际密码需要通过 BCrypt 加密生成。

## 配置说明

### Nacos 配置

核心配置存储在 Nacos 中，包括数据库连接、Redis 连接、JWT 密钥等。

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| NACOS_HOST | Nacos 服务地址 | localhost |
| MYSQL_HOST | MySQL 地址 | localhost |
| MYSQL_PASSWORD | MySQL 密码 | structexam123 |
| REDIS_HOST | Redis 地址 | localhost |
| JWT_SECRET | JWT 密钥 | structexam-secret-key |

## 开发指南

### 后端开发

```bash
# 编译项目
cd backend
mvn clean compile

# 运行测试
mvn test

# 打包
mvn package -DskipTests

# 单独启动某个服务
cd services/user-service
mvn spring-boot:run
```

### 前端开发

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

## 部署要求

### 硬件要求

- CPU: 4核+
- 内存: 8GB+
- 磁盘: 50GB+

### 软件要求

- Docker 24.x+
- Docker Compose 2.x+

## 许可证

MIT License

## 联系方式

如有问题，请提交 Issue。
