const { test, expect } = require('@playwright/test')

const username = process.env.E2E_STUDENT_USERNAME
const password = process.env.E2E_STUDENT_PASSWORD

test.describe('答题页面（需登录且有可用考试）', () => {
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

  test('进入考试页面', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '进入', exact: true }).click()
      await page.waitForLoadState('networkidle', { timeout: 15_000 })
    } else {
      test.skip('当前没有可进入的考试')
    }
  })

  test('进入考试弹窗确认', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await expect(page.getByRole('button', { name: '进入', exact: true })).toBeVisible()
      await expect(page.getByRole('button', { name: '取消' })).toBeVisible()
      await page.getByRole('button', { name: '取消' }).click()
    } else {
      test.skip('当前没有可进入的考试')
    }
  })

  test('考试信息与倒计时展示', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '进入', exact: true }).click()
      await page.waitForSelector('.exam-info', { timeout: 15_000 })
      
      await expect(page.locator('.exam-info h3')).toBeVisible()
      await expect(page.locator('.timer')).toBeVisible()
      await expect(page.locator('.timer strong')).toHaveText(/\d+:\d+:\d+/)
    } else {
      test.skip('当前没有可进入的考试')
    }
  })

  test('题目列表导航功能', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '进入', exact: true }).click()
      await page.waitForSelector('.question-nav', { timeout: 15_000 })
      
      const questionItems = page.locator('.question-item')
      const count = await questionItems.count()
      
      if (count > 1) {
        await questionItems.nth(1).click()
        await expect(questionItems.nth(1)).toHaveClass(/active/)
      }
    } else {
      test.skip('当前没有可进入的考试')
    }
  })

  test('答题页面操作按钮展示', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '进入', exact: true }).click()
      await page.waitForSelector('.action-buttons', { timeout: 15_000 })
      
      await expect(page.getByRole('button', { name: '保存代码' })).toBeVisible()
      await expect(page.getByRole('button', { name: '提交本题' })).toBeVisible()
      await expect(page.getByRole('button', { name: '交卷并退出' })).toBeVisible()
    } else {
      test.skip('当前没有可进入的考试')
    }
  })

  test('保存代码功能', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '进入', exact: true }).click()
      await page.waitForSelector('.action-buttons', { timeout: 15_000 })
      
      const saveBtn = page.getByRole('button', { name: '保存代码' })
      if (await saveBtn.isEnabled()) {
        await saveBtn.click()
        await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 })
      }
    } else {
      test.skip('当前没有可进入的考试')
    }
  })

  test('提交本题功能', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '进入', exact: true }).click()
      await page.waitForSelector('.action-buttons', { timeout: 15_000 })
      
      const submitBtn = page.getByRole('button', { name: '提交本题' })
      if (await submitBtn.isEnabled()) {
        await submitBtn.click()
        await page.waitForSelector('.el-message-box', { timeout: 5000 })
        await page.getByRole('button', { name: '取消' }).click()
      }
    } else {
      test.skip('当前没有可进入的考试')
    }
  })

  test('交卷确认弹窗', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '进入', exact: true }).click()
      await page.waitForSelector('.action-buttons', { timeout: 15_000 })
      
      await page.getByRole('button', { name: '交卷并退出' }).click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await expect(page.getByRole('button', { name: '取消' })).toBeVisible()
      await expect(page.getByRole('button', { name: '确定' })).toBeVisible()
      await page.getByRole('button', { name: '取消' }).click()
    } else {
      test.skip('当前没有可进入的考试')
    }
  })
})