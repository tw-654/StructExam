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

  test('历史记录表格展示', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
    
    await expect(page.getByRole('columnheader', { name: '考试ID' })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: '得分' })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: '状态' })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: '进入时间' })).toBeVisible()
  })

  test('查看考试记录列表', async ({ page }) => {
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const rows = page.locator('.el-table .el-table__row')
    const count = await rows.count()
    console.log(`历史记录数量: ${count}`)
  })

  test('历史记录列表虚拟滚动渲染性能', async ({ page }) => {
    const makeMockRecords = (count) => {
      return Array.from({ length: count }, (_, index) => ({
        examId: `EXAM-${1000 + index}`,
        enterTime: new Date(Date.now() - index * 60000).toISOString(),
        submitTime: new Date(Date.now() - index * 30000).toISOString(),
        score: Math.floor(Math.random() * 101),
        status: index % 2 === 0 ? 'GRADED' : 'SUBMITTED',
        ipAddress: `192.168.0.${(index % 254) + 1}`
      }))
    }

    await page.route('**/exam/record/list', async (route) => {
      const mockData = { data: makeMockRecords(5000) }
      await route.fulfill({
        status: 200,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(mockData)
      })
    })

    await page.reload({ waitUntil: 'networkidle' })
    const startTime = Date.now()
    await page.waitForSelector('.el-table .el-table__row', { timeout: 30_000 })
    const renderTime = Date.now() - startTime
    const rows = page.locator('.el-table .el-table__row')
    const count = await rows.count()

    console.log(`[性能指标] 虚拟滚动历史记录渲染耗时: ${renderTime}ms, 行数: ${count}`)
    expect(count).toBeGreaterThanOrEqual(5000)
    expect(renderTime).toBeLessThan(2000)
  })
})