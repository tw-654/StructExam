-- StructExam 数据库初始化脚本

CREATE DATABASE IF NOT EXISTS structexam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE structexam;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    real_name VARCHAR(50) COMMENT '真实姓名',
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色: STUDENT/TEACHER/ADMIN',
    email VARCHAR(100) COMMENT '邮箱',
    status INT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 试卷表
CREATE TABLE IF NOT EXISTS t_exam (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '试卷ID',
    title VARCHAR(200) NOT NULL COMMENT '试卷标题',
    description TEXT COMMENT '试卷描述',
    duration INT NOT NULL DEFAULT 60 COMMENT '考试时长(分钟)',
    total_score INT NOT NULL DEFAULT 100 COMMENT '总分',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/ENDED',
    creator_id BIGINT COMMENT '创建者ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷表';

-- 题目表
CREATE TABLE IF NOT EXISTS t_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '题目ID',
    exam_id BIGINT NOT NULL COMMENT '试卷ID',
    type VARCHAR(20) NOT NULL COMMENT '题目类型: SINGLE/MULTIPLE/PROGRAMMING',
    title VARCHAR(500) NOT NULL COMMENT '题目标题',
    content TEXT COMMENT '题目内容',
    options TEXT COMMENT '选项(JSON格式)',
    score INT NOT NULL DEFAULT 10 COMMENT '分值',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_exam (exam_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目表';

-- 考试记录表
CREATE TABLE IF NOT EXISTS t_exam_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    exam_id BIGINT NOT NULL COMMENT '试卷ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    enter_time DATETIME COMMENT '进入考试时间',
    submit_time DATETIME COMMENT '提交时间',
    score INT COMMENT '得分',
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态: IN_PROGRESS/SUBMITTED/ENDED',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_exam_user (exam_id, user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试记录表';

-- 代码提交表
CREATE TABLE IF NOT EXISTS t_code_submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '提交ID',
    exam_id BIGINT NOT NULL COMMENT '试卷ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    code_content TEXT COMMENT '代码内容',
    language VARCHAR(50) COMMENT '编程语言',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/JUDGING/ACCEPTED/WRONG_ANSWER/ERROR',
    submit_time DATETIME COMMENT '提交时间',
    score INT COMMENT '得分',
    judge_status VARCHAR(50) COMMENT '判题状态',
    time_used_ms BIGINT COMMENT '耗时(毫秒)',
    memory_used_kb BIGINT COMMENT '内存使用(KB)',
    judge_time DATETIME COMMENT '判题时间',
    judge_message TEXT COMMENT '判题信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_exam_question_user (exam_id, question_id, user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码提交表';

-- 测试用例表
CREATE TABLE IF NOT EXISTS t_question_test_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用例ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    input_data TEXT COMMENT '输入数据',
    expected_output TEXT COMMENT '预期输出',
    is_sample BOOLEAN DEFAULT FALSE COMMENT '是否为样例',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试用例表';

-- 判题记录表
CREATE TABLE IF NOT EXISTS t_judge_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    submission_id BIGINT NOT NULL COMMENT '提交ID',
    test_case_id BIGINT COMMENT '测试用例ID',
    status VARCHAR(50) COMMENT '判题状态',
    input_data TEXT COMMENT '输入数据',
    expected_output TEXT COMMENT '预期输出',
    actual_output TEXT COMMENT '实际输出',
    error_message TEXT COMMENT '错误信息',
    time_used_ms BIGINT COMMENT '耗时(毫秒)',
    memory_used_kb BIGINT COMMENT '内存使用(KB)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_submission (submission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='判题记录表';

-- 插入测试数据

-- BCrypt加密的 "StructExam123" (cost=10)
INSERT INTO t_user (username, password, real_name, role, email, status) VALUES
('admin', '$2a$10$HFvtoQ1Ud7sbXfJsQzHP2eQwT80KXyWvHHColl5rCicNZkK.hy8UW', '管理员', 'ADMIN', 'admin@structexam.com', 1),
('admin01', '$2a$10$HFvtoQ1Ud7sbXfJsQzHP2eQwT80KXyWvHHColl5rCicNZkK.hy8UW', '管理员01', 'ADMIN', 'admin01@structexam.com', 1),
('teacher01', '$2a$10$HFvtoQ1Ud7sbXfJsQzHP2eQwT80KXyWvHHColl5rCicNZkK.hy8UW', '张老师', 'TEACHER', 'teacher01@structexam.com', 1),
('student01', '$2a$10$HFvtoQ1Ud7sbXfJsQzHP2eQwT80KXyWvHHColl5rCicNZkK.hy8UW', '学生张三', 'STUDENT', 'student01@structexam.com', 1),
('student02', '$2a$10$HFvtoQ1Ud7sbXfJsQzHP2eQwT80KXyWvHHColl5rCicNZkK.hy8UW', '学生李四', 'STUDENT', 'student02@structexam.com', 1),
('jmeter_docker_01', '$2a$10$HFvtoQ1Ud7sbXfJsQzHP2eQwT80KXyWvHHColl5rCicNZkK.hy8UW', '测试用户', 'STUDENT', 'jmeter@structexam.com', 1);

-- 插入测试试卷
INSERT INTO t_exam (title, description, duration, total_score, start_time, end_time, status, creator_id) VALUES
('数据结构基础测试', '测试学生对数据结构基础知识的掌握情况', 60, 100,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), 'PUBLISHED', 2),
('算法入门测试', '基础算法知识测试', 90, 100,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), 'PUBLISHED', 2);

-- 插入测试题目
INSERT INTO t_question (exam_id, type, title, content, options, score, sort_order) VALUES
(1, 'SINGLE', '栈的特点是什么？', '以下哪个选项正确描述了栈的数据结构特点？',
 '["先进先出","先进后出","随机访问","顺序访问"]', 10, 1),
(1, 'SINGLE', '队列的特点是什么？', '以下哪个选项正确描述了队列的数据结构特点？',
 '["先进先出","先进后出","随机访问","顺序访问"]', 10, 2),
(1, 'PROGRAMMING', '实现冒泡排序', '请用Python实现冒泡排序算法，对输入的数组进行升序排序。\n\n输入示例：\n[3, 1, 4, 1, 5, 9, 2, 6]\n\n输出示例：\n[1, 1, 2, 3, 4, 5, 6, 9]', NULL, 50, 3),
(1, 'MULTIPLE', '以下哪些是线性数据结构？', '选择所有正确的答案。',
 '["数组","链表","栈","树","图"]', 30, 4),
(2, 'PROGRAMMING', '实现二分查找', '请用Python实现二分查找算法，在有序数组中查找目标值。', NULL, 100, 1);

-- 插入测试用例
INSERT INTO t_question_test_case (question_id, input_data, expected_output, is_sample, sort_order) VALUES
(3, '[3,1,4,1,5,9,2,6]', '[1,1,2,3,4,5,6,9]', TRUE, 1),
(3, '[5,4,3,2,1]', '[1,2,3,4,5]', FALSE, 2),
(5, '{"arr":[1,3,5,7,9],"target":5}', '{"index":2,"found":true}', TRUE, 1),
(5, '{"arr":[1,3,5,7,9],"target":2}', '{"index":-1,"found":false}', FALSE, 2);