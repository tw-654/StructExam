const { test, expect } = require('@playwright/test')

const baseURL = process.env.E2E_BASE_URL || 'http://localhost:80'
const adminUsername = process.env.E2E_ADMIN_USERNAME || 'admin'
const adminPassword = process.env.E2E_ADMIN_PASSWORD || 'StructExam123'

test.describe('管理员端测试', () => {
  let authToken = null

  test.beforeAll(async ({ request }) => {
    const loginResponse = await request.post(`${baseURL}/api/auth/login`, {
      data: {
        username: adminUsername,
        password: adminPassword
      }
    })
    
    const loginData = await loginResponse.json()
    authToken = loginData.data?.token || null
    console.log('Admin login successful, token obtained:', !!authToken)
  })

  test.beforeEach(async ({ page }) => {
    if (!authToken) {
      test.skip('未获取到管理员token')
    }
    
    await page.goto(`${baseURL}/login`)
    
    await page.evaluate((token) => {
      localStorage.setItem('token', token)
      localStorage.setItem('role', 'ADMIN')
      localStorage.setItem('username', 'admin')
      localStorage.setItem('realName', '管理员')
      localStorage.setItem('userId', '1')
    }, authToken)
    
    await page.reload()
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)
  })

  test('管理员登录后进入管理员首页', async ({ page }) => {
    await page.goto(`${baseURL}/admin`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
    
    await expect(page).toHaveURL(/.*\/admin/)
    
    const h2Element = page.getByRole('heading', { name: '管理员监控' })
    await expect(h2Element).toHaveText('管理员监控')
  })

  test('考试列表只读查看', async ({ page }) => {
    await page.goto(`${baseURL}/admin`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
    
    const sectionTitle = page.locator('.section-title span', { hasText: '考试情况' })
    await expect(sectionTitle).toHaveText('考试情况')
    
    const table = page.locator('.el-table').first()
    await expect(table).toBeVisible({ timeout: 15000 })
  })

  test('管理员页面API访问测试', async ({ request }) => {
    if (!authToken) {
      test.skip('未获取到管理员token')
    }
    
    const examListResponse = await request.get(`${baseURL}/api/exam/list?pageNum=1&pageSize=10`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-User-Id': '1',
        'X-User-Role': 'ADMIN'
      }
    })
    
    console.log('Admin exam list API status:', examListResponse.status())
    expect(examListResponse.status()).toBe(200)
  })

  test('分布式集群监控面板', async ({ page }) => {
    await page.goto(`${baseURL}/admin`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
    
    const sectionTitle = page.locator('.section-title span', { hasText: '分布式判题后台监控' })
    await expect(sectionTitle).toBeVisible()
  })

  test('沙箱节点标签页', async ({ page }) => {
    await page.goto(`${baseURL}/admin`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
    
    const nodeTab = page.getByRole('tab', { name: '沙箱节点' })
    if (await nodeTab.isVisible()) {
      await nodeTab.click()
      await page.waitForTimeout(1000)
      
      const table = page.locator('#pane-nodes .el-table').first()
      await expect(table).toBeVisible()
    }
  })

  test('最近任务标签页', async ({ page }) => {
    await page.goto(`${baseURL}/admin`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
    
    const taskTab = page.getByRole('tab', { name: '最近任务' })
    if (await taskTab.isVisible()) {
      await taskTab.click()
      await page.waitForTimeout(1000)
      
      const table = page.locator('#pane-tasks .el-table').first()
      await expect(table).toBeVisible()
    }
  })

  test('交互会话标签页', async ({ page }) => {
    await page.goto(`${baseURL}/admin`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
    
    const sessionTab = page.getByRole('tab', { name: '交互会话' })
    if (await sessionTab.isVisible()) {
      await sessionTab.click()
      await page.waitForTimeout(1000)
      
      const table = page.locator('#pane-sessions .el-table').first()
      await expect(table).toBeVisible()
    }
  })

  test('测试工具标签页', async ({ page }) => {
    await page.goto(`${baseURL}/admin`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
    
    const toolTab = page.getByRole('tab', { name: '测试工具' })
    if (await toolTab.isVisible()) {
      await toolTab.click()
      await page.waitForTimeout(1000)
      
      const toolPanel = page.locator('#pane-tools .tool-panel').first()
      await expect(toolPanel).toBeVisible()
    }
  })
})
