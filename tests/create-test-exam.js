const http = require('http');

const BASE_URL = 'localhost';
const PORT = 8088;

let teacherToken = '';
let examId = '';

function makeRequest(options, data = null) {
    return new Promise((resolve, reject) => {
        const req = http.request(options, (res) => {
            let body = '';
            res.on('data', (chunk) => body += chunk);
            res.on('end', () => {
                try {
                    const json = JSON.parse(body);
                    resolve(json);
                } catch (e) {
                    reject(new Error(`Invalid JSON: ${body}`));
                }
            });
        });
        req.on('error', reject);
        if (data) {
            req.write(JSON.stringify(data));
        }
        req.end();
    });
}

async function login(username, password) {
    const options = {
        hostname: BASE_URL,
        port: PORT,
        path: '/auth/login',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    };
    const response = await makeRequest(options, { username, password });
    return response.data.token;
}

async function createExam(token) {
    const now = new Date();
    const startTime = new Date(now.getTime() - 1000 * 60 * 60).toISOString();
    const endTime = new Date(now.getTime() + 1000 * 60 * 60 * 2).toISOString();

    const options = {
        hostname: BASE_URL,
        port: PORT,
        path: '/exam/teacher',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            'X-User-Role': 'TEACHER',
            'X-User-Id': '2'
        }
    };

    const response = await makeRequest(options, {
        title: '性能测试考试',
        description: '用于测试前端性能的考试',
        duration: 120,
        totalScore: 100,
        startTime,
        endTime,
        status: 'DRAFT'
    });
    return response.data.id;
}

async function createQuestion(token, examId, index) {
    const type = 'PROGRAMMING';
    
    const options = {
        hostname: BASE_URL,
        port: PORT,
        path: '/exam/question',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            'X-User-Role': 'TEACHER',
            'X-User-Id': '2'
        }
    };

    const response = await makeRequest(options, {
        examId,
        type,
        title: `编程题 ${index + 1}`,
        content: `请实现一个简单的求和功能。\n\n题目描述：编写程序计算两个数的和。`,
        options: null,
        score: 20,
        sortOrder: index
    });
    return response.data;
}

async function addTestCase(token, questionId, input, expectedOutput, isHidden = false) {
    const options = {
        hostname: BASE_URL,
        port: PORT,
        path: '/exam/question/testcase',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            'X-User-Role': 'TEACHER',
            'X-User-Id': '2'
        }
    };

    await makeRequest(options, {
        questionId,
        input,
        expectedOutput,
        isHidden
    });
}

async function publishExam(token, examId) {
    const options = {
        hostname: BASE_URL,
        port: PORT,
        path: `/exam/teacher/${examId}/publish`,
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            'X-User-Role': 'TEACHER',
            'X-User-Id': '2'
        }
    };

    await makeRequest(options, {});
}

async function main() {
    try {
        console.log('1. 教师登录...');
        teacherToken = await login('teacher01', 'StructExam123');
        console.log('登录成功');

        console.log('\n2. 创建考试...');
        examId = await createExam(teacherToken);
        console.log(`考试创建成功，ID: ${examId}`);

        console.log('\n3. 创建题目...');
        for (let i = 0; i < 5; i++) {
            const question = await createQuestion(teacherToken, examId, i);
            console.log(`题目 ${i + 1} 创建成功，ID: ${question.id}`);
            
            console.log(`   添加测试用例...`);
            await addTestCase(teacherToken, question.id, '1 2', '3', false);
            await addTestCase(teacherToken, question.id, '10 20', '30', false);
            await addTestCase(teacherToken, question.id, '-1 1', '0', true);
        }

        console.log('\n4. 发布考试...');
        await publishExam(teacherToken, examId);
        console.log('考试发布成功');

        console.log('\n✅ 测试数据创建完成！');
        console.log(`考试ID: ${examId}`);
        console.log('可以使用学生账号 student01/StructExam123 进行考试');
        
    } catch (error) {
        console.error('创建测试数据失败:', error.message);
        process.exit(1);
    }
}

main();