const http = require('http');

const BASE_URL = 'localhost';
const PORT = 8088;

let teacherToken = '';
let examId = 3;

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
                    resolve({ data: body });
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
        headers: { 'Content-Type': 'application/json' }
    };
    const response = await makeRequest(options, { username, password });
    return response.data?.token || response.token;
}

async function createExamRecord(token, userId, examId) {
    const options = {
        hostname: BASE_URL,
        port: PORT,
        path: `/exam/enter/${examId}`,
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            'X-User-Role': 'STUDENT',
            'X-User-Id': userId.toString()
        }
    };
    
    try {
        await makeRequest(options, {});
        return true;
    } catch (error) {
        return false;
    }
}

async function submitExam(token, userId, examId) {
    const options = {
        hostname: BASE_URL,
        port: PORT,
        path: `/exam/submit/${examId}`,
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
            'X-User-Role': 'STUDENT',
            'X-User-Id': userId.toString()
        }
    };
    
    try {
        await makeRequest(options, {});
        return true;
    } catch (error) {
        return false;
    }
}

async function createTestRecords(count) {
    console.log(`开始创建 ${count} 条测试记录...`);
    
    for (let i = 1; i <= count; i++) {
        const username = `testuser${i}`;
        const password = 'TestPass123';
        
        console.log(`\n创建用户 ${username} 的考试记录...`);
        
        try {
            const token = await login(username, password);
            if (!token) {
                console.log(`  ❌ 用户 ${username} 登录失败，跳过`);
                continue;
            }
            
            await createExamRecord(token, 1000 + i, examId);
            console.log(`  ✅ 进入考试成功`);
            
            await submitExam(token, 1000 + i, examId);
            console.log(`  ✅ 提交考试成功`);
            
        } catch (error) {
            console.log(`  ❌ 处理失败: ${error.message}`);
        }
        
        if (i % 10 === 0) {
            console.log(`\n已完成 ${i}/${count} 条记录`);
        }
    }
    
    console.log(`\n✅ 测试记录创建完成！`);
}

async function main() {
    try {
        console.log('1. 教师登录验证...');
        teacherToken = await login('teacher01', 'StructExam123');
        console.log('教师登录成功');
        
        console.log('\n2. 创建测试成绩记录...');
        await createTestRecords(100);
        
    } catch (error) {
        console.error('执行失败:', error.message);
        process.exit(1);
    }
}

main();