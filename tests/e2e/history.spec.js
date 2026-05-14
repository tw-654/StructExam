const { test, expect } = require('@playwright/test')

const username = process.env.E2E_STUDENT_USERNAME
const password = process.env.E2E_STUDENT_PASSWORD

test.describe('历史记录页面（需登录）', () => {
  test.beforeEach(async ({ page }) => {
    if (!username || !password) {
      test.skip('未设置环境变量')
    }
    await page.goto('/login')
    await page.getByPlaceholder('用户名/学号').fill(username)
    await page.getByPlaceholder('密码').fill(password)
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page).toHaveURL(/\/home/, { timeout: 15_000 })
    await page.locator('.el-dropdown-link').click()
    await page.getByText('考试记录').click()
    await expect(page).toHaveURL(/\/history/, { timeout: 10_000 })
  })

  test('导航到历史记录页面', async ({ page }) => {
    await expect(page).toHaveURL(/\/history/)
  })

  test('历史记录页面展示', async ({ page }) => {
    await expect(page.getByRole('heading', { name: '考试记录' })).toBeVisible()
  })
})
