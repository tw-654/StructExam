const { test, expect } = require('@playwright/test')

const username = process.env.E2E_STUDENT_USERNAME
const password = process.env.E2E_STUDENT_PASSWORD

test.describe('学生登录流程（需环境变量）', () => {
  test.skip(!username || !password, '未设置 E2E_STUDENT_USERNAME / E2E_STUDENT_PASSWORD 时跳过')

  test('使用有效学生账号登录后进入首页', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名/学号').fill(username)
    await page.getByPlaceholder('密码').fill(password)
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page).toHaveURL(/\/home/, { timeout: 15_000 })
  })
})
