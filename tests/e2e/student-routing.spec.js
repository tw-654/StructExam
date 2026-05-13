const { test, expect } = require('@playwright/test')

test.describe('学生端路由与未登录访问', () => {
  test('未携带 Token 访问 /home 时重定向到登录页', async ({ page }) => {
    await page.goto('/home')
    await expect(page).toHaveURL(/\/login/)
  })

  test('登录页展示标题与登录按钮', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByRole('heading', { name: /数据结构机考平台/ })).toBeVisible()
    await expect(page.getByRole('button', { name: '登录' })).toBeVisible()
  })

  test('注册页可访问', async ({ page }) => {
    await page.goto('/register')
    await expect(page.getByRole('button', { name: '注册' })).toBeVisible()
  })
})
