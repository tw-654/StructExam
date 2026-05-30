-- StructExam 数据库初始化脚本
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS structexam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE structexam;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名/学号',
    password VARCHAR(255) NOT NULL COMMENT '密码(Bcrypt加密)',
    real_name VARCHAR(255) NOT NULL COMMENT '真实姓名',
    role ENUM('STUDENT', 'TEACHER', 'ADMIN') NOT NULL DEFAULT 'STUDENT' COMMENT '角色',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 试卷表
CREATE TABLE IF NOT EXISTS t_exam (
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
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    INDEX idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 题目表
CREATE TABLE IF NOT EXISTS t_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL COMMENT '所属试卷ID',
    type ENUM('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'PROGRAMMING') NOT NULL COMMENT '题目类型',
    title VARCHAR(500) NOT NULL COMMENT '题目标题',
    content TEXT COMMENT '题目内容(JSON格式存储)',
    options JSON COMMENT '选项(JSON格式)',
    score INT NOT NULL DEFAULT 10 COMMENT '分值',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_exam_id (exam_id),
    INDEX idx_type (type),
    FOREIGN KEY (exam_id) REFERENCES t_exam(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 考试记录表
CREATE TABLE IF NOT EXISTS t_exam_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL COMMENT '考试ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    enter_time DATETIME COMMENT '进入考试时间',
    submit_time DATETIME COMMENT '提交时间',
    score INT COMMENT '得分',
    status ENUM('NOT_STARTED', 'IN_PROGRESS', 'SUBMITTED', 'GRADED') DEFAULT 'NOT_STARTED' COMMENT '状态',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_exam_user (exam_id, user_id),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    FOREIGN KEY (exam_id) REFERENCES t_exam(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录表';

-- 代码提交表
CREATE TABLE IF NOT EXISTS t_code_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL COMMENT '考试ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    code_content TEXT NOT NULL COMMENT '代码内容',
    language VARCHAR(50) NOT NULL DEFAULT 'java' COMMENT '编程语言',
    status ENUM('SAVED', 'SUBMITTED', 'GRADED') DEFAULT 'SAVED' COMMENT '状态',
    submit_time DATETIME COMMENT '提交时间',
    score INT COMMENT '判题得分',
    judge_status VARCHAR(20) COMMENT '判题状态',
    time_used_ms BIGINT COMMENT '运行耗时(ms)',
    memory_used_kb BIGINT COMMENT '运行内存(KB)',
    judge_time DATETIME COMMENT '判题完成时间',
    judge_message TEXT COMMENT '判题信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_exam_user_question (exam_id, user_id, question_id),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    FOREIGN KEY (exam_id) REFERENCES t_exam(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES t_question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码提交表';

-- 代码版本表
CREATE TABLE IF NOT EXISTS t_code_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id BIGINT NOT NULL COMMENT '提交ID',
    code_content TEXT NOT NULL COMMENT '代码内容',
    version INT DEFAULT 1 COMMENT '版本号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_submission (submission_id),
    FOREIGN KEY (submission_id) REFERENCES t_code_submission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码版本表';

-- 插入测试数据
INSERT INTO t_user (username, password, real_name, role, email) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '管理员', 'ADMIN', 'admin@structexam.com'),
('teacher01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张老师', 'TEACHER', 'teacher01@structexam.com'),
('student01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李同学', 'STUDENT', 'student01@structexam.com');

-- 密码统一为: StructExam123 (BCrypt加密后)
-- 注意: 上述密码是占位符,实际使用时需要正确的BCrypt加密

-- ============================================================
-- 测试用例判定功能 (新增表，不改动既有表)
-- ============================================================

-- 题目测试用例表
CREATE TABLE IF NOT EXISTS t_question_test_case (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id     BIGINT       NOT NULL                COMMENT '题目ID',
    case_name       VARCHAR(100)                         COMMENT '用例名称(可选)',
    input_data      MEDIUMTEXT   NOT NULL                COMMENT '输入数据',
    expected_output MEDIUMTEXT                           COMMENT '期望输出(允许空，例如对拍特判)',
    is_sample       TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '是否为题面样例(题目描述展示)',
    is_public       TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '是否对学生公开(失败时是否返回 input/expected)',
    weight          INT          NOT NULL DEFAULT 1      COMMENT '本用例权重',
    time_limit_ms   INT                                  COMMENT '时间限制(ms),NULL 则用题目默认',
    memory_limit_kb INT                                  COMMENT '内存限制(KB),NULL 则用题目默认',
    sort_order      INT          NOT NULL DEFAULT 0      COMMENT '排序',
    status          TINYINT      NOT NULL DEFAULT 1      COMMENT '1启用 0禁用',
    create_time     DATETIME              DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_question_sort   (question_id, sort_order),
    INDEX idx_question_status (question_id, status),
    FOREIGN KEY (question_id) REFERENCES t_question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目测试用例表';

-- 判题记录表(每次"运行/提交本题"产生一条)
CREATE TABLE IF NOT EXISTS t_judge_record (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         VARCHAR(64)  NOT NULL                COMMENT '分布式任务ID',
    submission_id   BIGINT                               COMMENT '关联 t_code_submission.id(RUN 可空)',
    exam_id         BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    question_id     BIGINT       NOT NULL,
    language        VARCHAR(50)  NOT NULL,
    code_snapshot   MEDIUMTEXT                           COMMENT '本次判题代码快照',
    trigger_type    VARCHAR(20)  NOT NULL DEFAULT 'SUBMIT' COMMENT 'RUN/SUBMIT/SUBMIT_ALL/REJUDGE',
    judge_status    VARCHAR(20)  NOT NULL DEFAULT 'JUDGING' COMMENT 'AC/WA/CE/RE/TLE/MLE/PE/FAILED/JUDGING',
    total_cases     INT          NOT NULL DEFAULT 0,
    passed_cases    INT          NOT NULL DEFAULT 0,
    score           INT          NOT NULL DEFAULT 0,
    max_score       INT          NOT NULL DEFAULT 0,
    time_used_ms    BIGINT                               COMMENT '所有用例最大耗时',
    memory_used_kb  BIGINT                               COMMENT '所有用例最大内存',
    compile_error   TEXT,
    runtime_error   TEXT,
    judge_message   TEXT,
    sandbox_node    VARCHAR(128)                         COMMENT '执行节点URI',
    started_time    DATETIME,
    finished_time   DATETIME,
    create_time     DATETIME              DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_id           (task_id),
    INDEX idx_submission            (submission_id),
    INDEX idx_exam_user_question    (exam_id, user_id, question_id, create_time),
    INDEX idx_judge_status          (judge_status),
    FOREIGN KEY (submission_id) REFERENCES t_code_submission(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='判题记录表';

-- 判题用例明细表(每条用例一行)
CREATE TABLE IF NOT EXISTS t_judge_case_result (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    judge_record_id BIGINT       NOT NULL                COMMENT '关联 t_judge_record.id',
    test_case_id    BIGINT                               COMMENT '关联 t_question_test_case.id(自定义输入可空)',
    case_index      INT          NOT NULL                COMMENT '当次判题中的序号(0,1,2...)',
    case_name       VARCHAR(100)                         COMMENT '用例名称快照',
    status          VARCHAR(20)  NOT NULL                COMMENT 'AC/WA/TLE/MLE/RE/PE/SKIP',
    passed          TINYINT(1)   NOT NULL DEFAULT 0,
    is_public       TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '是否对学生公开 input/expected',
    input_data      MEDIUMTEXT                           COMMENT '输入快照',
    expected_output MEDIUMTEXT                           COMMENT '期望输出快照',
    actual_output   MEDIUMTEXT                           COMMENT '实际输出',
    error_message   TEXT                                 COMMENT '本条错误信息',
    time_used_ms    BIGINT,
    memory_used_kb  BIGINT,
    weight          INT          NOT NULL DEFAULT 0      COMMENT '本用例权重快照',
    create_time     DATETIME              DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_record_index (judge_record_id, case_index),
    INDEX idx_test_case    (test_case_id),
    FOREIGN KEY (judge_record_id) REFERENCES t_judge_record(id)        ON DELETE CASCADE,
    FOREIGN KEY (test_case_id)   REFERENCES t_question_test_case(id)   ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='判题用例明细表';
