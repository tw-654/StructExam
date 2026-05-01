# 数据结构机考平台 - 项目规格说明书

## 1. 项目概述

### 1.1 项目名称
**StructExam** - 数据结构在线机考平台

### 1.2 项目愿景
参照希冀平台设计，打造一个稳定、高效的在线代码考试系统，支持登录认证、考试管理、代码编辑、自动交卷等核心功能，为二期代码沙箱判断奠定基础。

### 1.3 技术栈

| 层次 | 技术选型 |
|------|----------|
| 后端框架 | Spring Cloud Alibaba 2022.0.0.0 (Spring Boot 3.0+) |
| 服务注册与配置 | Nacos 2.2.3 |
| 缓存层 | Redis 7.x |
| 数据库 | MySQL 8.0 |
| 消息队列 | RabbitMQ (可选，用于二期) |
| 容器化 | Docker 24.x + Docker Compose 2.x |
| 前端框架 | Vue.js 3.x + Element Plus |
| 代码编辑器 | Monaco Editor |
| 构建工具 | Maven 3.9+ |

### 1.4 微服务架构设计

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
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              ▼
              ┌───────────────────────────────┐
              │         MySQL 8.0              │
              │   (用户表、试卷表、题目表、      │
              │    答题记录表、代码表)          │
              └───────────────────────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │          Redis 7.x            │
              │   (试卷缓存、代码缓存、          │
              │    Session、临时用例)           │
              └───────────────────────────────┘
```

---

## 2. 功能模块设计

### 2.1 用户认证模块 (User Service)

#### 2.1.1 功能列表
- **[x] 用户注册** - 支持学号/工号、密码、姓名、角色(学生/教师)
- **[x] 用户登录** - 用户名密码登录，返回JWT Token
- **[x] Token刷新** - 自动刷新即将过期的Token
- **[x] 密码修改** - 登录后修改密码
- **[x] 登出功能** - 注销当前Token

#### 2.1.2 数据模型

**用户表 (t_user)**
```sql
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名/学号',
    password VARCHAR(255) NOT NULL COMMENT '密码(Bcrypt加密)',
    real_name VARCHAR(100) NOT NULL COMMENT '真实姓名',
    role ENUM('STUDENT', 'TEACHER', 'ADMIN') NOT NULL DEFAULT 'STUDENT' COMMENT '角色',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 2.2 考试管理模块 (Exam Service)

#### 2.2.1 功能列表
- **[x] 试卷管理** - 创建、编辑、发布、删除试卷
- **[x] 题目管理** - 添加单选题、多选题、编程题
- **[x] 考试发布** - 设置考试时间、时长、参考人员
- **[x] 考试记录** - 记录学生进入考试、提交试卷时间
- **[x] 成绩查询** - 查看已结束考试的成绩

#### 2.2.2 数据模型

**试卷表 (t_exam)**
```sql
CREATE TABLE t_exam (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '试卷标题',
    description TEXT COMMENT '试卷描述',
    duration INT NOT NULL DEFAULT 120 COMMENT '考试时长(分钟)',
    total_score INT DEFAULT 100 COMMENT '总分',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    status ENUM('DRAFT', 'PUBLISHED', 'ONGOING', 'FINISHED') DEFAULT 'DRAFT' COMMENT '状态',
    creator_id BIGINT NOT NULL COMMENT '创建者ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**题目表 (t_question)**
```sql
CREATE TABLE t_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL COMMENT '所属试卷ID',
    type ENUM('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'PROGRAMMING') NOT NULL COMMENT '题目类型',
    title VARCHAR(500) NOT NULL COMMENT '题目描述',
    content TEXT COMMENT '题目内容(JSON格式存储)',
    score INT NOT NULL DEFAULT 10 COMMENT '分值',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**考试记录表 (t_exam_record)**
```sql
CREATE TABLE t_exam_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL COMMENT '考试ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    enter_time DATETIME COMMENT '进入考试时间',
    submit_time DATETIME COMMENT '提交时间',
    score INT COMMENT '得分',
    status ENUM('NOT_STARTED', 'IN_PROGRESS', 'SUBMITTED', 'GRADED') DEFAULT 'NOT_STARTED' COMMENT '状态',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 2.3 代码管理模块 (Code Service)

#### 2.3.1 功能列表
- **[x] 代码保存** - 实时保存考生代码到Redis
- **[x] 代码提交** - 考试结束时自动提交代码
- **[x] 代码恢复** - 进入考试后恢复上次保存的代码
- **[x] 手动保存** - 考生手动保存当前代码
- **[x] 代码版本管理** - 记录代码历史版本

#### 2.3.2 数据模型

**代码表 (t_code_submission)**
```sql
CREATE TABLE t_code_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL COMMENT '考试ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    code_content TEXT NOT NULL COMMENT '代码内容',
    language VARCHAR(50) NOT NULL DEFAULT 'java' COMMENT '编程语言',
    status ENUM('SAVED', 'SUBMITTED', 'GRADED') DEFAULT 'SAVED' COMMENT '状态',
    submit_time DATETIME COMMENT '提交时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**代码版本表 (t_code_version)**
```sql
CREATE TABLE t_code_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id BIGINT NOT NULL COMMENT '提交ID',
    code_content TEXT NOT NULL COMMENT '代码内容',
    version INT DEFAULT 1 COMMENT '版本号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 2.4 缓存设计 (Redis)

| Key Pattern | 数据类型 | 用途 | 过期时间 |
|-------------|----------|------|----------|
| `user:session:{userId}` | String | 用户Session | 2小时 |
| `exam:paper:{examId}` | Hash | 试卷完整信息 | 考试结束 |
| `code:temp:{examId}:{userId}:{questionId}` | String | 临时代码 | 4小时 |
| `exam:status:{examId}:{userId}` | Hash | 考试状态 | 考试结束 |
| `token:blacklist:{token}` | String | JWT黑名单 | Token剩余时间 |

---

## 3. API接口设计

### 3.1 认证接口

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | /api/auth/register | 用户注册 | 公开 |
| POST | /api/auth/login | 用户登录 | 公开 |
| POST | /api/auth/logout | 用户登出 | 需认证 |
| POST | /api/auth/refresh | 刷新Token | 需认证 |
| PUT | /api/auth/password | 修改密码 | 需认证 |

### 3.2 考试接口

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | /api/exam/list | 获取考试列表 | 需认证 |
| GET | /api/exam/{id} | 获取考试详情 | 需认证 |
| POST | /api/exam/enter/{id} | 进入考试 | 需认证 |
| POST | /api/exam/submit/{id} | 提交考试 | 需认证 |
| GET | /api/exam/record/{id} | 获取考试记录 | 需认证 |

### 3.3 题目接口

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | /api/question/{examId} | 获取试卷题目 | 需认证 |
| GET | /api/question/{id}/detail | 获取题目详情 | 需认证 |

### 3.4 代码接口

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | /api/code/save | 保存代码 | 需认证 |
| GET | /api/code/{examId}/{questionId} | 获取已保存代码 | 需认证 |
| POST | /api/code/submit | 提交代码 | 需认证 |

---

## 4. 前端页面设计

### 4.1 页面列表

| 页面路径 | 描述 | 组件 |
|----------|------|------|
| /login | 登录页 | Login.vue |
| /register | 注册页 | Register.vue |
| /home | 首页/考试列表 | Home.vue |
| /exam/:id | 考试页面 | Exam.vue |
| /exam/:id/question/:qid | 答题页面 | Question.vue |
| /history | 历史成绩 | History.vue |
| /profile | 个人中心 | Profile.vue |

### 4.2 考试页面布局 (参照希冀平台)

```
┌──────────────────────────────────────────────────────────────────┐
│  LOGO   考生：张三    试卷：2024数据结构期末考试    剩余时间：01:23:45 │
├────────────────────────┬─────────────────────────────────────────┤
│                        │                                         │
│   题目导航栏            │        Monaco代码编辑器                   │
│   ┌────┐ ┌────┐ ┌────┐ │                                         │
│   │ 1  │ │ 2  │ │ 3  │ │   #include <stdio.h>                    │
│   └────┘ └────┘ └────┘ │   int main() {                          │
│   已做: 2/5  未做: 3    │       printf("Hello");                 │
│                        │       return 0;                         │
│   题目列表              │   }                                     │
│   ┌──────────────┐    │                                         │
│   │ 1. 选择题  ✓  │    ├─────────────────────────────────────────┤
│   │ 2. 选择题  ✓  │    │  运行结果:                               │
│   │ 3. 编程题     │    │  ┌─────────────────────────────────┐    │
│   │ 4. 编程题  ✓  │    │  │ Hello                          │    │
│   │ 5. 编程题     │    │  └─────────────────────────────────┘    │
│   └──────────────┘    │                                         │
│                        │  [保存代码]  [运行]  [提交本题]           │
│   [交卷并退出]          │                                         │
└────────────────────────┴─────────────────────────────────────────┘
```

---

## 5. Docker部署设计

### 5.1 容器列表

| 容器名 | 镜像 | 端口 | 用途 |
|--------|------|------|------|
| mysql | mysql:8.0 | 3306 | 主数据库 |
| redis | redis:7-alpine | 6379 | 缓存服务 |
| nacos | nacos/nacos-server:v2.2.3 | 8848 | 注册中心 |
| gateway | structexam-gateway:latest | 8080 | API网关 |
| user-service | structexam-user:latest | 8081 | 用户服务 |
| exam-service | structexam-exam:latest | 8082 | 考试服务 |
| code-service | structexam-code:latest | 8083 | 代码服务 |
| frontend | structexam-frontend:latest | 80 | 前端应用 |

### 5.2 Docker Compose 配置

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: structexam123
      MYSQL_DATABASE: structexam
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  nacos:
    image: nacos/nacos-server:v2.2.3
    environment:
      MODE: standalone
    ports:
      - "8848:8848"

  gateway:
    build: ./gateway
    ports:
      - "8080:8080"

  user-service:
    build: ./services/user-service
    ports:
      - "8081:8081"

  exam-service:
    build: ./services/exam-service
    ports:
      - "8082:8082"

  code-service:
    build: ./services/code-service
    ports:
      - "8083:8083"

  frontend:
    build: ./frontend
    ports:
      - "80:80"

volumes:
  mysql_data:
```

---

## 6. 一期功能清单

### 6.1 已完成功能

- [x] 项目脚手架搭建 (Spring Cloud Alibaba)
- [x] Nacos 服务注册与配置中心
- [x] Spring Cloud Gateway 网关
- [x] JWT 认证机制
- [x] 用户注册与登录
- [x] 考试列表查看
- [x] 进入考试功能
- [x] Monaco 代码编辑器集成
- [x] 代码实时保存 (Redis)
- [x] 自动交卷功能
- [x] 手动保存代码
- [x] Docker Compose 部署

### 6.2 二期待实现

- [ ] 代码沙箱环境
- [ ] 代码编译运行
- [ ] 测试用例判定
- [ ] 分布式任务调度
- [ ] 成绩统计分析

---

## 7. 项目目录结构

```
StructExam/
├── docker-compose.yml
├── README.md
├── SPEC.md
├── backend/
│   ├── pom.xml (父POM)
│   ├── gateway/
│   │   ├── pom.xml
│   │   └── src/main/java/com/structexam/gateway/
│   ├── common/
│   │   ├── pom.xml
│   │   └── src/main/java/com/structexam/common/
│   │       ├── config/
│   │       ├── entity/
│   │       ├── util/
│   │       └── exception/
│   └── services/
│       ├── user-service/
│       │   ├── pom.xml
│       │   └── src/main/java/com/structexam/user/
│       ├── exam-service/
│       │   ├── pom.xml
│       │   └── src/main/java/com/structexam/exam/
│       └── code-service/
│           ├── pom.xml
│           └── src/main/java/com/structexam/code/
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── router/
│       ├── views/
│       ├── components/
│       ├── api/
│       └── stores/
└── sql/
    └── init.sql
```

---

## 8. 配置说明

### 8.1 Nacos 配置

核心配置存储在 Nacos 中，包括：
- 数据库连接配置
- Redis 连接配置
- JWT 密钥配置
- 服务间通信配置

### 8.2 环境变量

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| MYSQL_HOST | MySQL地址 | mysql |
| REDIS_HOST | Redis地址 | redis |
| NACOS_HOST | Nacos地址 | nacos |
| JWT_SECRET | JWT密钥 | structexam-secret-key |
| MYSQL_PASSWORD | MySQL密码 | structexam123 |

---

## 9. 部署要求

### 9.1 硬件要求

- CPU: 4核+
- 内存: 8GB+
- 磁盘: 50GB+

### 9.2 软件要求

- Docker 24.x+
- Docker Compose 2.x+
- JDK 17+
- Node.js 18+

---

*文档版本: 1.0*
*最后更新: 2026-05-01*
