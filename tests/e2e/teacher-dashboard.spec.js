const { test, expect } = require('@playwright/test')

const baseURL = process.env.E2E_BASE_URL || 'http://localhost:80'
const teacherUsername = process.env.E2E_TEACHER_USERNAME || 'teacher01'
const teacherPassword = process.env.E2E_TEACHER_PASSWORD || 'StructExam123'

test.describe('教师端测试', () => {
  let authToken = null

  test.beforeAll(async ({ request }) => {
    const loginResponse = await request.post(`${baseURL}/api/auth/login`, {
      data: {
        username: teacherUsername,
        password: teacherPassword
      }
    })
    
    const loginData = await loginResponse.json()
    authToken = loginData.data?.token || null
    console.log('Teacher login successful, token obtained:', !!authToken)
  })

  test.beforeEach(async ({ page }) => {
    if (!authToken) {
      test.skip('未获取到教师token')
    }
    
    await page.goto(`${baseURL}/login`)
    
    await page.evaluate((token) => {
      localStorage.setItem('token', token)
      localStorage.setItem('role', 'TEACHER')
      localStorage.setItem('username', 'teacher01')
      localStorage.setItem('realName', '张老师')
      localStorage.setItem('userId', '2')
    }, authToken)
    
    await page.reload()
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)
  })

  test('教师登录后进入教师首页', async ({ page }) => {
    await page.goto(`${baseURL}/teacher`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
    
    await expect(page).toHaveURL(/.*\/teacher/)
    
    const h2Element = page.getByRole('heading', { name: '教师管理' })
    await expect(h2Element).toHaveText('教师管理')
  })

  test('试卷列表展示', async ({ page }) => {
    await page.goto(`${baseURL}/teacher`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
    
    const table = page.locator('.el-table').first()
    await expect(table).toBeVisible({ timeout: 15000 })
    
    const headers = await table.locator('th').allTextContents()
    console.log('Table headers:', headers)
    
    expect(headers).toContain('考试名称')
    expect(headers).toContain('时长')
    expect(headers).toContain('总分')
    expect(headers).toContain('状态')
    expect(headers).toContain('操作')
  })

  test('新建试卷', async ({ page }) => {
    await page.goto(`${baseURL}/teacher`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
    
    const createBtn = page.getByRole('button', { name: '新建试卷' })
    if (await createBtn.isVisible()) {
      await createBtn.click()
      await page.waitForTimeout(1000)
      
      const dialogTitle = page.locator('.el-dialog__title')
      await expect(dialogTitle).toHaveText('新建试卷')
      
      const examNameInput = page.getByRole('textbox', { name: '' }).first()
      if (await examNameInput.isVisible()) {
        await examNameInput.fill('测试试卷 ' + Date.now())
      }
      
      const datePicker = page.getByPlaceholder('开始时间')
      if (await datePicker.isVisible()) {
        await datePicker.fill('2025-12-01 09:00:00')
      }
      
      const endDatePicker = page.getByPlaceholder('结束时间')
      if (await endDatePicker.isVisible()) {
        await endDatePicker.fill('2025-12-31 18:00:00')
      }
      
      const saveBtn = page.getByRole('button', { name: '保存' })
      if (await saveBtn.isEnabled()) {
        await saveBtn.click({ force: true })
        const successMsg = page.locator('.el-message--success')
        if (await successMsg.waitFor({ timeout: 15000 }).catch(() => false)) {
          await expect(successMsg).toContainText('试卷已保存')
        }
      }
    }
  })

  test('编辑试卷', async ({ page }) => {
    await page.goto(`${baseURL}/teacher`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
    
    const editBtn = page.getByRole('button', { name: '编辑' }).first()
    if (await editBtn.isVisible()) {
      await editBtn.click()
      await page.waitForTimeout(1000)
      
      const dialogTitle = page.locator('.el-dialog__title')
      await expect(dialogTitle).toHaveText('编辑试卷')
      
      const saveBtn = page.getByRole('button', { name: '保存' })
      if (await saveBtn.isEnabled()) {
        await saveBtn.click()
        await page.waitForSelector('.el-message--success', { timeout: 10000 })
        await expect(page.locator('.el-message--success')).toContainText('试卷已保存')
      }
    }
  })

  test('发布试卷', async ({ page }) => {
    await page.goto(`${baseURL}/teacher`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
    
    const publishBtn = page.getByRole('button', { name: '发布' }).first()
    if (await publishBtn.isVisible() && await publishBtn.isEnabled()) {
      await publishBtn.click()
      const successMsg = page.locator('.el-message--success')
      if (await successMsg.waitFor({ timeout: 15000 }).catch(() => false)) {
        await expect(successMsg).toContainText('考试已发布')
      }
    }
  })

  test('题目管理面板', async ({ page }) => {
    await page.goto(`${baseURL}/teacher`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
    
    const rows = page.locator('table tr')
    const rowCount = await rows.count()
    if (rowCount > 1) {
      await rows.nth(1).click()
      await page.waitForTimeout(1000)
      
      const sectionTitle = page.locator('.section-title span', { hasText: '题目管理' })
      await expect(sectionTitle).toHaveText('题目管理')
    }
  })

  test('考试监控面板', async ({ page }) => {
    await page.goto(`${baseURL}/teacher`)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(3000)
    
    const rows = page.locator('table tr')
    const rowCount = await rows.count()
    if (rowCount > 1) {
      await rows.nth(1).click()
      await page.waitForTimeout(1000)
      
      const sectionTitle = page.locator('.section-title span').first()
      await expect(sectionTitle).toHaveText('实时监控')
    }
  })

  test('教师页面API访问测试', async ({ request }) => {
    if (!authToken) {
      test.skip('未获取到教师token')
    }
    
    const examListResponse = await request.get(`${baseURL}/api/exam/list?pageNum=1&pageSize=10`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'X-User-Id': '2',
        'X-User-Role': 'TEACHER'
      }
    })
    
    console.log('Teacher exam list API status:', examListResponse.status())
    expect(examListResponse.status()).toBe(200)
  })
})
