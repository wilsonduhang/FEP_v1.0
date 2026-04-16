import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    // P7.2a Task 10: exclude Playwright spec from Vitest run
    exclude: ['**/node_modules/**', '**/dist/**', 'e2e/**'],
    coverage: { provider: 'v8', reporter: ['text', 'html'] },
  },
});
