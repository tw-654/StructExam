// @ts-check
const { defineConfig, devices } = require('@playwright/test')

const baseURL = process.env.E2E_BASE_URL || 'http://localhost:3000'

module.exports = defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure'
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  // 默认不自动拉起前端：需先在 frontend 执行 npm install && npm run dev。
  // 若要在 CI 中由 Playwright 启动，设置 E2E_START_WEB_SERVER=1（会先 npm ci）。
  ...(process.env.E2E_START_WEB_SERVER === '1'
    ? {
        webServer: {
          command: 'npm ci && npm run dev',
          cwd: require('path').join(__dirname, '..', 'frontend'),
          url: baseURL,
          reuseExistingServer: false,
          timeout: 300_000
        }
      }
    : {})
})
