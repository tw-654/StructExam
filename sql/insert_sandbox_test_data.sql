USE structexam;

INSERT INTO t_exam (title, description, duration, total_score, start_time, end_time, status, creator_id) VALUES
('数据结构代码测试考试', '用于测试代码沙箱环境的编程考试', 90, 100, '2024-01-01 08:00:00', '2030-12-31 23:59:59', 'PUBLISHED', 2);

SET @exam_id = LAST_INSERT_ID();

INSERT INTO t_question (exam_id, type, title, content, score, sort_order) VALUES
(@exam_id, 'PROGRAMMING', '数组反转', '{\"description\":\"编写一个方法，将给定的整数数组反转。\",\"template\":\"public class Main {\\n    public static void reverseArray(int[] arr) {\\n        // 在此处编写你的代码\\n    }\\n    public static void main(String[] args) {\\n        java.util.Scanner scanner = new java.util.Scanner(System.in);\\n        int n = scanner.nextInt();\\n        int[] arr = new int[n];\\n        for (int i = 0; i < n; i++) {\\n            arr[i] = scanner.nextInt();\\n        }\\n        reverseArray(arr);\\n        for (int num : arr) {\\n            System.out.print(num + \\\" \\\");\\n        }\\n    }\\n}\"}', 50, 1),
(@exam_id, 'PROGRAMMING', '斐波那契数列', '{\"description\":\"编写一个方法，计算第n个斐波那契数。\",\"template\":\"public class Main {\\n    public static int fibonacci(int n) {\\n        // 在此处编写你的代码\\n        return 0;\\n    }\\n    public static void main(String[] args) {\\n        java.util.Scanner scanner = new java.util.Scanner(System.in);\\n        int n = scanner.nextInt();\\n        System.out.println(fibonacci(n));\\n    }\\n}\"}', 50, 2);

SELECT CONCAT('创建的考试ID: ', @exam_id) AS result;