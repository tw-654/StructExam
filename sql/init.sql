-- StructExam 数据库初始化脚本
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS structexam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE structexam;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名/学号',
    password VARCHAR(255) NOT NULL COMMENT '密码(Bcrypt加密)',
    real_name VARCHAR(100) NOT NULL COMMENT '真实姓名',
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
