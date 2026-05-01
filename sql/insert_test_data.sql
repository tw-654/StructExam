USE structexam;

INSERT INTO t_question (exam_id, type, title, content, options, score, sort_order) VALUES
(1, 'SINGLE_CHOICE', '栈的特点是', '栈的特点是什么？', '["A. 先进先出", "B. 后进先出", "C. 随机存取", "D. 顺序存取"]', 10, 1),
(1, 'SINGLE_CHOICE', '在长度为n的顺序表中删除第i个元素需要移动', '在长度为n的顺序表中，删除第i个元素（1<=i<=n）需要向前移动多少个元素？', '["A. n-i", "B. n-i+1", "C. n-i-1", "D. i"]', 10, 2),
(1, 'MULTIPLE_CHOICE', '以下属于线性结构的是', '以下数据结构中属于线性结构的是？', '["A. 栈", "B. 队列", "C. 二叉树", "D. 图"]', 10, 3),
(1, 'PROGRAMMING', '实现栈的入栈操作', '请实现一个顺序栈的push操作，当栈满时返回false，否则将元素入栈并返回true。\n\n结构体定义：\ntypedef struct {\n    int data[MAX_SIZE];\n    int top;\n} Stack;\n\n函数签名：bool push(Stack *s, int value)', NULL, 35, 4),
(1, 'PROGRAMMING', '实现二叉树的中序遍历', '请实现二叉树的中序遍历（非递归方式），将遍历结果存入result数组中。\n\n结构体定义：\ntypedef struct TreeNode {\n    int val;\n    struct TreeNode *left;\n    struct TreeNode *right;\n} TreeNode;\n\n函数签名：void inorderTraversal(TreeNode *root, int *result, int *returnSize)', NULL, 35, 5);
