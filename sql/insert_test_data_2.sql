USE structexam;

INSERT INTO t_question (exam_id, type, title, content, options, score, sort_order) VALUES
(2, 'SINGLE_CHOICE', '二叉树的性质', '对于一棵具有n个结点的二叉树，若采用二叉链表存储结构，则空指针域的个数为？', '["A. n", "B. n+1", "C. n-1", "D. 2n"]', 10, 1),
(2, 'SINGLE_CHOICE', 'AVL树旋转', '在AVL树中，当插入一个结点导致不平衡时，最少需要进行几次旋转操作？', '["A. 1次", "B. 2次", "C. 3次", "D. 不确定"]', 10, 2),
(2, 'MULTIPLE_CHOICE', '图的遍历', '以下关于图的深度优先遍历（DFS）和广度优先遍历（BFS）的说法，正确的是？', '["A. DFS可以用栈实现", "B. BFS可以用队列实现", "C. DFS和BFS都能用于有向图", "D. BFS不能用于加权图"]', 15, 3),
(2, 'PROGRAMMING', '实现快速排序', '请实现快速排序算法，对给定数组进行升序排序。\n\n函数签名：void quickSort(int[] arr, int low, int high)', NULL, 55, 4),
(2, 'PROGRAMMING', '实现最小生成树-Prim算法', '请实现Prim算法，求无向图的最小生成树。\n\n输入：邻接矩阵表示的图\n输出：最小生成树的权值总和', NULL, 60, 5);
