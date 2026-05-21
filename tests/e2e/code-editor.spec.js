const { test, expect } = require('@playwright/test')

const username = process.env.E2E_STUDENT_USERNAME
const password = process.env.E2E_STUDENT_PASSWORD

test.describe('代码编辑器功能测试', () => {
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

  test('代码编辑器语言切换', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '进入', exact: true }).click()
      await page.waitForSelector('.editor-container', { timeout: 15_000 })
      
      const languageSelect = page.locator('.editor-actions select')
      await languageSelect.selectOption('cpp')
      await expect(languageSelect).toHaveValue('cpp')
      
      await languageSelect.selectOption('python')
      await expect(languageSelect).toHaveValue('python')
      
      await languageSelect.selectOption('java')
      await expect(languageSelect).toHaveValue('java')
    } else {
      test.skip('当前没有可进入的考试')
    }
  })

  test('运行代码按钮功能', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '进入', exact: true }).click()
      await page.waitForSelector('.editor-container', { timeout: 15_000 })
      
      const runBtn = page.getByRole('button', { name: '运行' })
      if (await runBtn.isEnabled()) {
        await runBtn.click()
        await expect(page.locator('.terminal-status.running')).toBeVisible({ timeout: 10_000 })
      }
    } else {
      test.skip('当前没有可进入的考试')
    }
  })

  test('停止运行按钮状态', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '进入', exact: true }).click()
      await page.waitForSelector('.editor-container', { timeout: 15_000 })
      
      const stopBtn = page.getByRole('button', { name: '停止' })
      await expect(stopBtn).toBeDisabled()
      
      const runBtn = page.getByRole('button', { name: '运行' })
      if (await runBtn.isEnabled()) {
        await runBtn.click()
        await page.waitForSelector('.terminal-status.running', { timeout: 10_000 })
        await expect(stopBtn).toBeEnabled()
      }
    } else {
      test.skip('当前没有可进入的考试')
    }
  })

  test('终端显示与输入', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '进入', exact: true }).click()
      await page.waitForSelector('.editor-container', { timeout: 15_000 })
      
      const runBtn = page.getByRole('button', { name: '运行' })
      if (await runBtn.isEnabled()) {
        await runBtn.click()
        await page.waitForSelector('.terminal-container', { timeout: 10_000 })
        await expect(page.locator('.terminal-body')).toBeVisible()
      }
    } else {
      test.skip('当前没有可进入的考试')
    }
  })
})

test.describe('交卷流程完整测试', () => {
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

  test('交卷时未答题提示', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '进入', exact: true }).click()
      await page.waitForSelector('.action-buttons', { timeout: 15_000 })
      
      await page.getByRole('button', { name: '交卷并退出' }).click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      
      const content = await page.locator('.el-message-box__message').textContent()
      expect(content).toContain('未答')
      
      await page.getByRole('button', { name: '取消' }).click()
    } else {
      test.skip('当前没有可进入的考试')
    }
  })

  test('交卷成功后返回首页', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const enterBtn = page.getByRole('button', { name: '进入考试' })
    
    if (await enterBtn.isVisible()) {
      await enterBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '进入', exact: true }).click()
      await page.waitForSelector('.action-buttons', { timeout: 15_000 })
      
      await page.getByRole('button', { name: '交卷并退出' }).click()
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.getByRole('button', { name: '确定' }).click()
      
      await expect(page).toHaveURL(/\/home/, { timeout: 15_000 })
    } else {
      test.skip('当前没有可进入的考试')
    }
  })
})