const { test, expect } = require('@playwright/test')

const username = process.env.E2E_STUDENT_USERNAME
const password = process.env.E2E_STUDENT_PASSWORD

const getEnabledEnterButton = async (page) => {
  const enterBtns = page.getByRole('button', { name: '进入考试' })
  const count = await enterBtns.count()
  
  for (let i = 0; i < count; i++) {
    const btn = enterBtns.nth(i)
    if (await btn.isEnabled()) {
      return btn
    }
  }
  return null
}

const performanceMetrics = []

test.describe('高频交互页面渲染性能测试', () => {
  test.beforeEach(async ({ page }) => {
    if (!username || !password) {
      test.skip('未设置环境变量')
    }
  })

  test('首页加载性能', async ({ page }) => {
    const startTime = Date.now()
    
    await page.goto('/login')
    await page.getByPlaceholder('用户名/学号').fill(username)
    await page.getByPlaceholder('密码').fill(password)
    await page.getByRole('button', { name: '登录' }).click()
    
    const loginEndTime = Date.now()
    await expect(page).toHaveURL(/\/home/, { timeout: 15_000 })
    
    await page.waitForSelector('.el-table', { timeout: 10_000 })
    const homeReadyTime = Date.now()

    performanceMetrics.push({
      metric: '首页加载',
      loginTime: loginEndTime - startTime,
      totalTime: homeReadyTime - startTime
    })

    console.log(`[性能指标] 登录时间: ${loginEndTime - startTime}ms`)
    console.log(`[性能指标] 首页就绪时间: ${homeReadyTime - startTime}ms`)
  })

  test('考试页面加载与编辑器初始化性能', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名/学号').fill(username)
    await page.getByPlaceholder('密码').fill(password)
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page).toHaveURL(/\/home/, { timeout: 15_000 })
    await page.waitForSelector('.el-table', { timeout: 10_000 })

    const enterBtn = await getEnabledEnterButton(page)
    if (!enterBtn) {
      test.skip('当前没有可进入的考试')
    }

    const enterStartTime = Date.now()
    await enterBtn.click()
    await page.waitForSelector('.el-message-box', { timeout: 5000 })
    await page.getByRole('button', { name: '进入', exact: true }).click()
    
    await page.waitForSelector('.exam-info', { timeout: 15_000 })
    const examInfoReady = Date.now()
    
    await page.waitForSelector('.editor-container', { timeout: 15_000 })
    const editorReady = Date.now()
    
    await page.waitForSelector('.monaco-editor', { timeout: 15_000 })
    const monacoReady = Date.now()

    performanceMetrics.push({
      metric: '考试页面加载',
      enterToExamInfo: examInfoReady - enterStartTime,
      enterToEditor: editorReady - enterStartTime,
      enterToMonaco: monacoReady - enterStartTime
    })

    console.log(`[性能指标] 考试信息就绪: ${examInfoReady - enterStartTime}ms`)
    console.log(`[性能指标] 编辑器容器就绪: ${editorReady - enterStartTime}ms`)
    console.log(`[性能指标] Monaco编辑器就绪: ${monacoReady - enterStartTime}ms`)
  })

  test('题目切换响应性能', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名/学号').fill(username)
    await page.getByPlaceholder('密码').fill(password)
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page).toHaveURL(/\/home/, { timeout: 15_000 })
    await page.waitForSelector('.el-table', { timeout: 10_000 })

    const enterBtn = await getEnabledEnterButton(page)
    if (!enterBtn) {
      test.skip('当前没有可进入的考试')
    }

    await enterBtn.click()
    await page.waitForSelector('.el-message-box', { timeout: 5000 })
    await page.getByRole('button', { name: '进入', exact: true }).click()
    await page.waitForSelector('.question-nav', { timeout: 15_000 })

    const questionItems = page.locator('.question-item')
    const count = await questionItems.count()
    
    if (count < 3) {
      test.skip('题目数量不足，无法测试切换性能')
    }

    const switchTimes = []
    for (let i = 1; i < Math.min(count, 5); i++) {
      const startTime = Date.now()
      await questionItems.nth(i).click()
      await page.waitForSelector('.question-header h3', { timeout: 5000 })
      const endTime = Date.now()
      switchTimes.push(endTime - startTime)
      
      console.log(`[性能指标] 题目切换 ${i}: ${switchTimes[i-1]}ms`)
    }

    const avgSwitchTime = switchTimes.reduce((a, b) => a + b, 0) / switchTimes.length
    const maxSwitchTime = Math.max(...switchTimes)

    performanceMetrics.push({
      metric: '题目切换',
      switchCount: switchTimes.length,
      avgTime: avgSwitchTime,
      maxTime: maxSwitchTime
    })

    console.log(`[性能指标] 题目切换平均时间: ${avgSwitchTime.toFixed(2)}ms`)
    console.log(`[性能指标] 题目切换最大时间: ${maxSwitchTime}ms`)
  })

  test('代码保存响应性能', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名/学号').fill(username)
    await page.getByPlaceholder('密码').fill(password)
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page).toHaveURL(/\/home/, { timeout: 15_000 })
    await page.waitForSelector('.el-table', { timeout: 10_000 })

    const enterBtn = await getEnabledEnterButton(page)
    if (!enterBtn) {
      test.skip('当前没有可进入的考试')
    }

    await enterBtn.click()
    await page.waitForSelector('.el-message-box', { timeout: 5000 })
    await page.getByRole('button', { name: '进入', exact: true }).click()
    await page.waitForSelector('.action-buttons', { timeout: 15_000 })

    const saveTimes = []
    for (let i = 0; i < 3; i++) {
      const saveBtn = page.getByRole('button', { name: '保存代码' })
      if (!await saveBtn.isEnabled()) {
        test.skip('保存按钮不可用')
      }

      const startTime = Date.now()
      await saveBtn.click()
      await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })
      const endTime = Date.now()
      
      await page.waitForSelector('.el-message', { state: 'hidden', timeout: 3000 })
      
      saveTimes.push(endTime - startTime)
      console.log(`[性能指标] 代码保存 ${i+1}: ${saveTimes[i]}ms`)
    }

    const avgSaveTime = saveTimes.reduce((a, b) => a + b, 0) / saveTimes.length
    const maxSaveTime = Math.max(...saveTimes)

    performanceMetrics.push({
      metric: '代码保存',
      saveCount: saveTimes.length,
      avgTime: avgSaveTime,
      maxTime: maxSaveTime
    })

    console.log(`[性能指标] 代码保存平均时间: ${avgSaveTime.toFixed(2)}ms`)
    console.log(`[性能指标] 代码保存最大时间: ${maxSaveTime}ms`)
  })

  test('代码运行响应性能', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名/学号').fill(username)
    await page.getByPlaceholder('密码').fill(password)
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page).toHaveURL(/\/home/, { timeout: 15_000 })
    await page.waitForSelector('.el-table', { timeout: 10_000 })

    const enterBtn = await getEnabledEnterButton(page)
    if (!enterBtn) {
      test.skip('当前没有可进入的考试')
    }

    await enterBtn.click()
    await page.waitForSelector('.el-message-box', { timeout: 5000 })
    await page.getByRole('button', { name: '进入', exact: true }).click()
    await page.waitForSelector('.editor-container', { timeout: 15_000 })

    const runBtn = page.getByRole('button', { name: '运行' })
    if (!await runBtn.isEnabled()) {
      test.skip('运行按钮不可用')
    }

    const startTime = Date.now()
    await runBtn.click()
    await page.waitForSelector('.terminal-status.running', { timeout: 10_000 })
    
    await page.waitForSelector('.terminal-status', { timeout: 30_000 })
    const statusElement = await page.locator('.terminal-status')
    let running = true
    let attempts = 0
    while (running && attempts < 60) {
      const statusClass = await statusElement.getAttribute('class')
      running = statusClass.includes('running')
      if (running) {
        await page.waitForTimeout(500)
        attempts++
      }
    }

    const endTime = Date.now()

    performanceMetrics.push({
      metric: '代码运行',
      totalTime: endTime - startTime
    })

    console.log(`[性能指标] 代码运行总时间: ${endTime - startTime}ms`)
  })

  test('语言切换响应性能', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('用户名/学号').fill(username)
    await page.getByPlaceholder('密码').fill(password)
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page).toHaveURL(/\/home/, { timeout: 15_000 })
    await page.waitForSelector('.el-table', { timeout: 10_000 })

    const enterBtn = await getEnabledEnterButton(page)
    if (!enterBtn) {
      test.skip('当前没有可进入的考试')
    }

    await enterBtn.click()
    await page.waitForSelector('.el-message-box', { timeout: 5000 })
    await page.getByRole('button', { name: '进入', exact: true }).click()
    await page.waitForSelector('.editor-container', { timeout: 15_000 })

    const languageSelect = page.locator('.editor-actions .el-select')
    const languages = ['Python', 'Java', 'C++']
    const switchTimes = []

    for (const lang of languages) {
      const startTime = Date.now()
      await languageSelect.click()
      await page.getByText(lang).click()
      
      const model = await page.evaluate(() => {
        const editor = window.monaco?.editor?.getEditors?.()[0]
        return editor?.getModel()?.getLanguageId?.()
      })
      
      const endTime = Date.now()
      switchTimes.push(endTime - startTime)
      console.log(`[性能指标] 语言切换到 ${lang}: ${switchTimes[switchTimes.length-1]}ms`)
    }

    const avgSwitchTime = switchTimes.reduce((a, b) => a + b, 0) / switchTimes.length

    performanceMetrics.push({
      metric: '语言切换',
      switchCount: switchTimes.length,
      avgTime: avgSwitchTime
    })

    console.log(`[性能指标] 语言切换平均时间: ${avgSwitchTime.toFixed(2)}ms`)
  })

  test.afterAll(async () => {
    console.log('\n=== 性能测试报告 ===')
    console.log('测试时间:', new Date().toISOString())
    console.log('====================')
    
    for (const metric of performanceMetrics) {
      console.log(`\n[${metric.metric}]`)
      for (const [key, value] of Object.entries(metric)) {
        if (key !== 'metric') {
          console.log(`  ${key}: ${typeof value === 'number' ? value.toFixed(2) + 'ms' : value}`)
        }
      }
    }

    console.log('\n=== 性能指标汇总 ===')
    console.log('| 指标 | 平均时间 | 最大时间 |')
    console.log('|------|----------|----------|')
    
    for (const metric of performanceMetrics) {
      const avgTime = metric.avgTime !== undefined ? metric.avgTime.toFixed(0) : '-'
      const maxTime = metric.maxTime !== undefined ? metric.maxTime.toFixed(0) : '-'
      console.log(`| ${metric.metric} | ${avgTime}ms | ${maxTime}ms |`)
    }

    const fs = require('fs')
    const report = {
      timestamp: new Date().toISOString(),
      metrics: performanceMetrics
    }
    fs.writeFileSync('performance-report.json', JSON.stringify(report, null, 2))
    console.log('\n报告已保存到: performance-report.json')
  })
})