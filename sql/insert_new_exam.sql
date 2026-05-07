USE structexam;

INSERT INTO t_exam (title, description, duration, total_score, start_time, end_time, status, creator_id) VALUES
('Java Programming Exam', 'Contains two programming questions to test Java programming skills', 120, 100, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 'PUBLISHED', 1);

SET @exam_id = LAST_INSERT_ID();

INSERT INTO t_question (exam_id, type, title, content, score, sort_order) VALUES
(@exam_id, 'PROGRAMMING', 'String Reverse', 'Write a method to reverse a given string', 50, 1),
(@exam_id, 'PROGRAMMING', 'Greatest Common Divisor', 'Write a method to calculate the GCD of two integers', 50, 2);

SELECT CONCAT('Exam ID created: ', @exam_id) AS result;