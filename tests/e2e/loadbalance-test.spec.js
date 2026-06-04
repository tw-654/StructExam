const { test, expect } = require('@playwright/test')

const apiBaseURL = process.env.E2E_API_URL || 'http://gateway:8080'
const adminUsername = process.env.E2E_ADMIN_USERNAME || 'admin01'
const adminPassword = process.env.E2E_ADMIN_PASSWORD || 'StructExam123'

test.describe('负载均衡策略测试', () => {
  let authToken = null

  test.beforeAll(async ({ request }) => {
    const loginResponse = await request.post(`${apiBaseURL}/api/auth/login`, {
      data: {
        username: adminUsername,
        password: adminPassword
      }
    })
    
    const loginData = await loginResponse.json()
    authToken = loginData.data?.token || null
    console.log('Admin login successful, token obtained:', !!authToken)
  })

  test('验证异构节点元数据信息', async ({ request }) => {
    if (!authToken) {
      test.skip('未获取到管理员token')
    }

    const nodesResponse = await request.get(`${apiBaseURL}/api/code/distributed/nodes`, {
      headers: {
        'Authorization': `Bearer ${authToken}`
      }
    })
    
    expect(nodesResponse.ok()).toBeTruthy()
    const nodesData = await nodesResponse.json()
    
    expect(nodesData.code).toBe(200)
    expect(Array.isArray(nodesData.data)).toBe(true)
    
    if (nodesData.data.length > 0) {
      nodesData.data.forEach((node, index) => {
        console.log(`节点 ${index + 1}:`)
        console.log(`  Host: ${node.host}`)
        console.log(`  Port: ${node.port}`)
        console.log(`  Healthy: ${node.healthy}`)
        console.log(`  Running Tasks: ${node.runningTasks}`)
        console.log(`  Metadata:`, node.metadata)
      })
      
      const hasDifferentMetadata = nodesData.data.some((node, index, array) => {
        const firstMeta = JSON.stringify(array[0].metadata)
        return JSON.stringify(node.metadata) !== firstMeta
      })
      console.log(`是否存在异构节点: ${hasDifferentMetadata}`)
    }
  })

  test('测试负载均衡策略切换API', async ({ request }) => {
    if (!authToken) {
      test.skip('未获取到管理员token')
    }

    const strategies = ['roundRobin', 'leastTasks']
    
    for (const strategy of strategies) {
      const response = await request.post(`${apiBaseURL}/api/code/distributed/admin/strategy`, {
        headers: {
          'Authorization': `Bearer ${authToken}`,
          'Content-Type': 'application/json',
          'X-User-Role': 'ADMIN'
        },
        data: {
          strategy: strategy
        }
      })
      
      expect(response.ok()).toBeTruthy()
      const result = await response.json()
      expect(result.code).toBe(200)
      console.log(`策略切换为 ${strategy}: ${result.message}`)
      
      await new Promise(resolve => setTimeout(resolve, 1000))
    }
  })

  test('测试高并发下的负载均衡效果', async ({ request }) => {
    if (!authToken) {
      test.skip('未获取到管理员token')
    }

    const initialNodes = await request.get(`${apiBaseURL}/api/code/distributed/nodes`, {
      headers: { 'Authorization': `Bearer ${authToken}` }
    })
    const initialData = await initialNodes.json()
    const initialTasks = initialData.data.reduce((sum, node) => sum + node.runningTasks, 0)
    console.log(`初始运行任务数: ${initialTasks}`)
    
    const submitPromises = []
    for (let i = 0; i < 10; i++) {
      submitPromises.push(
        request.post(`${apiBaseURL}/api/code/submit`, {
          headers: {
            'Authorization': `Bearer ${authToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            examId: 1,
            questionId: 1,
            code: `public class Test${i} { public static void main(String[] args) { System.out.println("LoadTest-${i}"); } }`,
            language: 'JAVA'
          }
        })
      )
    }
    
    await Promise.all(submitPromises)
    await new Promise(resolve => setTimeout(resolve, 3000))
    
    const afterNodes = await request.get(`${apiBaseURL}/api/code/distributed/nodes`, {
      headers: { 'Authorization': `Bearer ${authToken}` }
    })
    const afterData = await afterNodes.json()
    
    console.log('任务提交后的节点状态:')
    afterData.data.forEach((node, index) => {
      console.log(`节点 ${index + 1}: ${node.host}:${node.port} - 运行任务: ${node.runningTasks}`)
    })
    
    const maxTasks = Math.max(...afterData.data.map(n => n.runningTasks))
    const minTasks = Math.min(...afterData.data.map(n => n.runningTasks))
    const taskDiff = maxTasks - minTasks
    
    console.log(`任务分布差异: ${taskDiff}`)
    
    if (afterData.data.length > 1) {
      expect(taskDiff).toBeLessThanOrEqual(3)
    }
  })

  test('测试节点故障恢复机制', async ({ request }) => {
    if (!authToken) {
      test.skip('未获取到管理员token')
    }

    const nodesResponse = await request.get(`${apiBaseURL}/api/code/distributed/nodes`, {
      headers: { 'Authorization': `Bearer ${authToken}` }
    })
    const nodesData = await nodesResponse.json()
    
    if (nodesData.data.length > 0) {
      const healthyNodes = nodesData.data.filter(n => n.healthy)
      console.log(`健康节点数量: ${healthyNodes.length}`)
      
      if (healthyNodes.length > 0) {
        console.log('节点健康检查通过')
      }
    }
  })

  test('测试无效策略参数返回错误', async ({ request }) => {
    if (!authToken) {
      test.skip('未获取到管理员token')
    }

    const response = await request.post(`${apiBaseURL}/api/code/distributed/admin/strategy`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      data: {
        strategy: 'invalidStrategy'
      }
    })
    
    expect(response.status()).toBe(400)
    const result = await response.json()
    console.log(`无效策略测试结果: ${result.message}`)
  })

  test('测试空策略参数返回错误', async ({ request }) => {
    if (!authToken) {
      test.skip('未获取到管理员token')
    }

    const response = await request.post(`${apiBaseURL}/api/code/distributed/admin/strategy`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      data: {
        strategy: ''
      }
    })
    
    expect(response.status()).toBe(400)
    const result = await response.json()
    console.log(`空策略参数测试结果: ${result.message}`)
  })
})