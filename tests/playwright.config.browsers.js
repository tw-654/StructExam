/**
 * Playwright 多浏览器兼容性测试配置
 * 覆盖 Firefox/Safari/Edge 等浏览器
 */

module.exports = {
  // 测试目录
  testDir: './e2e',
  
  // 超时配置
  timeout: 60000,
  
  // 全局设置
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost',
    viewport: { width: 1920, height: 1080 },
    actionTimeout: 30000,
    trace: 'retain-on-failure',
    video: 'retain-on-failure',
  },

  // 多浏览器配置
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' },
    },
    {
      name: 'firefox',
      use: { browserName: 'firefox' },
    },
    {
      name: 'webkit',
      use: { browserName: 'webkit' },
    },
    // Edge 通过 Chromium 实现
    {
      name: 'edge',
      use: { 
        browserName: 'chromium',
        channel: 'msedge',
      },
    },
    // Safari 通过 WebKit 实现
    {
      name: 'safari',
      use: { 
        browserName: 'webkit',
        // macOS 上需要额外配置
      },
    },
  ],

  // 报告配置
  reporter: [
    ['html', { outputFolder: 'test-results/browser-compat' }],
    ['json', { outputFile: 'test-results/browser-compat-results.json' }],
  ],

  // 并发数
  workers: 2,
};
