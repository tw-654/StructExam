USE structexam;

INSERT INTO t_exam (title, description, duration, total_score, start_time, end_time, status, creator_id)
VALUES
('2026年春季数据结构考试', '本试卷包含3道编程题，主要考察数据结构基础知识', 120, 100, '2026-05-20 09:00:00', '2026-05-25 23:59:59', 'PUBLISHED', 1);

SET @exam_id = LAST_INSERT_ID();

INSERT INTO t_question (exam_id, type, title, content, score, sort_order)
VALUES
(@exam_id, 'PROGRAMMING', '两数之和', '给定一个整数数组 nums 和一个整数目标值 target，请你在数组中找出和为目标值 target 的那两个整数，并返回它们的数组下标。\n\n示例：\n输入: nums = [2,7,11,15], target = 9\n输出: [0,1]\n解释: 因为 nums[0] + nums[1] == 9 ，返回 [0, 1] 。', 30, 1);

INSERT INTO t_question (exam_id, type, title, content, score, sort_order)
VALUES
(@exam_id, 'PROGRAMMING', '有效的括号', '给定一个只包含字符 (、)、{、}、[、] 的字符串 s ，判断字符串是否有效。\n\n有效字符串需满足：\n1. 左括号必须用相同类型的右括号闭合。\n2. 左括号必须以正确的顺序闭合。\n\n示例：\n输入: s = ()[]{}\n输出: true', 35, 2);

INSERT INTO t_question (exam_id, type, title, content, score, sort_order)
VALUES
(@exam_id, 'PROGRAMMING', '合并两个有序链表', '将两个升序链表合并为一个新的升序链表并返回。新链表是通过拼接给定的两个链表的所有节点组成的。\n\n示例：\n输入: l1 = [1,2,4], l2 = [1,3,4]\n输出: [1,1,2,3,4,4]', 35, 3);

SELECT id, title, duration, total_score, status FROM t_exam;

SELECT id, type, title, score FROM t_question WHERE exam_id = @exam_id;
