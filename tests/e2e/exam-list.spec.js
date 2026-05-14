const { test, expect } = require('@playwright/test')

const username = process.env.E2E_STUDENT_USERNAME
const password = process.env.E2E_STUDENT_PASSWORD

test.describe('考试列表页面（需登录）', () => {
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

  test('首页展示考试列表标题', async ({ page }) => {
    await expect(page.getByRole('heading', { name: '考试列表' })).toBeVisible()
  })

  test('考试表格展示', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('尝试进入考试（如果有可用考试）', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '取消' }).click()
    }
  })
})