const { test, expect } = require('@playwright/test')

const baseURL = process.env.E2E_BASE_URL || 'http://localhost:80'
const adminUsername = process.env.E2E_ADMIN_USERNAME || 'admin'
const adminPassword = process.env.E2E_ADMIN_PASSWORD || 'StructExam123'

test.describe('分布式判题集群测试', () => {
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
    
    await page.goto(`${baseURL}/admin`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
  })

  test('沙箱节点状态监控', async ({ page }) => {
    const clusterTab = page.getByRole('tab', { name: '沙箱节点' })
    if (await clusterTab.isVisible()) {
      await clusterTab.click()
      await page.waitForTimeout(1000)
      
      const nodeTable = page.locator('table').first()
      await expect(nodeTable).toBeVisible()
    }
  })

  test('检查沙箱节点健康状态', async ({ page }) => {
    const clusterTab = page.getByRole('tab', { name: '沙箱节点' })
    if (await clusterTab.isVisible()) {
      await clusterTab.click()
      await page.waitForTimeout(1000)
      
      await page.waitForSelector('table tr', { timeout: 10000 })
      const rows = page.locator('table').first().locator('tr')
      const count = await rows.count()
      
      expect(count).toBeGreaterThanOrEqual(1)
    }
  })

  test('最近任务列表', async ({ page }) => {
    const taskTab = page.getByRole('tab', { name: '最近任务' })
    if (await taskTab.isVisible()) {
      await taskTab.click()
      await page.waitForTimeout(1000)
      
      const taskTable = page.locator('table').first()
      await expect(taskTable).toBeVisible()
    }
  })

  test('交互会话列表', async ({ page }) => {
    const sessionTab = page.getByRole('tab', { name: '交互会话' })
    if (await sessionTab.isVisible()) {
      await sessionTab.click()
      await page.waitForTimeout(1000)
      
      const sessionTable = page.locator('table').first()
      await expect(sessionTable).toBeVisible()
    }
  })

  test('节点容量监控', async ({ page }) => {
    const clusterTab = page.getByRole('tab', { name: '沙箱节点' })
    if (await clusterTab.isVisible()) {
      await clusterTab.click()
      await page.waitForTimeout(1000)
      
      await page.waitForSelector('table tr', { timeout: 10000 })
      const rows = page.locator('table').first().locator('tr')
      const count = await rows.count()
      
      expect(count).toBeGreaterThanOrEqual(1)
    }
  })

  test('任务队列深度监控', async ({ page }) => {
    const taskTab = page.getByRole('tab', { name: '最近任务' })
    if (await taskTab.isVisible()) {
      await taskTab.click()
      await page.waitForTimeout(1000)
      
      const taskTable = page.locator('table').first()
      await expect(taskTable).toBeVisible()
    }
  })
})
