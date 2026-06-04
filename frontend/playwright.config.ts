import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright E2E test configuration.
 * 
 * Tests run against the Vite preview server (port 4173) which serves the
 * production build. The backend is expected to be running on port 8080.
 * 
 * In CI, the workflow starts both the backend and the preview server before
 * running these tests.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: process.env.CI ? 'github' : 'list',
  timeout: 30_000,

  use: {
    baseURL: 'http://localhost:4173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
