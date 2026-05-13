USE structexam;

INSERT IGNORE INTO t_exam (id, title, description, duration, total_score, start_time, end_time, status, creator_id) VALUES
(1, 'Docker JMeter Seed Exam', 'Automated test seed', 120, 100, NOW(), DATE_ADD(NOW(), INTERVAL 14 DAY), 'PUBLISHED', 1);

DELETE FROM t_question WHERE exam_id = 1;

INSERT INTO t_question (exam_id, type, title, content, options, score, sort_order) VALUES
(1, 'PROGRAMMING', 'Seed programming', 'For load test get-code API', NULL, 10, 1);
