const { test, expect } = require('@playwright/test')

const username = process.env.E2E_STUDENT_USERNAME
const password = process.env.E2E_STUDENT_PASSWORD

test.describe('分布式韧性测试', () => {
  test.beforeEach(async ({ page }) => {
    if (!username || !password) {
      test.skip('未设置环境变量')
    }
    await page.goto('/login')
  })

  test('网络延迟下的页面加载容错', async ({ page }) => {
    await page.route('**/api/**', route => {
      return new Promise(resolve => {
        setTimeout(() => resolve(route.continue()), 3000)
      })
    })
    
    await page.getByPlaceholder('用户名/学号').fill(username)
    await page.getByPlaceholder('密码').fill(password)
    await page.getByRole('button', { name: '登录' }).click()
    
    await expect(page).toHaveURL(/\/home/, { timeout: 30_000 })
  })

  test('请求超时后的重试机制', async ({ page }) => {
    let requestCount = 0
    
    await page.route('**/api/auth/login', route => {
      requestCount++
      if (requestCount === 1) {
        route.abort('timedout')
      } else {
        route.continue()
      }
    })
    
    await page.getByPlaceholder('用户名/学号').fill(username)
    await page.getByPlaceholder('密码').fill(password)
    await page.getByRole('button', { name: '登录' }).click()
    
    await expect(page).toHaveURL(/\/home/, { timeout: 30_000 })
    expect(requestCount).toBeGreaterThan(1)
  })

  test('服务不可用时的友好错误提示', async ({ page }) => {
    await page.route('**/api/auth/login', route => {
      route.abort('connectionrefused')
    })
    
    await page.getByPlaceholder('用户名/学号').fill(username)
    await page.getByPlaceholder('密码').fill(password)
    await page.getByRole('button', { name: '登录' }).click()
    
    await expect(page.locator('.el-message--error')).toBeVisible({ timeout: 10_000 })
  })
})

test.describe('并发一致性测试', () => {
  test.beforeEach(async ({ page }) => {
    if (!username || !password) {
      test.skip('未设置环境变量')
    }
    await page.goto('/login')
    await page.getByPlaceholder('用户名/学号').fill(username)
    await page.getByPlaceholder('密码').fill(password)
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page).toHaveURL(/\/home/, { timeout: 15_000 })
  })

  test('重复提交防止', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      
      const confirmBtn = page.getByRole('button', { name: '进入', exact: true })
      await confirmBtn.click({ clickCount: 2, delay: 100 })
      
      await page.waitForLoadState('networkidle', { timeout: 15_000 })
    } else {
      test.skip('当前没有可进入的考试')
    }
  })

  test('快速连续操作的防抖处理', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '进入', exact: true }).click()
      await page.waitForSelector('.action-buttons', { timeout: 15_000 })
      
      const saveBtn = page.getByRole('button', { name: '保存代码' })
      if (await saveBtn.isEnabled()) {
        await saveBtn.click({ clickCount: 5, delay: 50 })
        await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })
      }
    } else {
      test.skip('当前没有可进入的考试')
    }
  })
})