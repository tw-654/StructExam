const { test, expect } = require('@playwright/test')

const username = process.env.E2E_STUDENT_USERNAME
const password = process.env.E2E_STUDENT_PASSWORD

test.describe('个人中心页面（需登录）', () => {
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

  test('导航到个人中心页面', async ({ page }) => {
    await page.locator('.el-dropdown-link').click()
    await page.getByText('个人中心').click()
    await expect(page).toHaveURL(/\/profile/, { timeout: 10_000 })
    await expect(page.locator('.card-header').getByText('个人中心')).toBeVisible()
  })

  test('登出功能', async ({ page }) => {
    await page.locator('.el-dropdown-link').click()
    await page.getByText('退出登录').click()
    await expect(page).toHaveURL(/\/login/, { timeout: 10_000 })
  })
})