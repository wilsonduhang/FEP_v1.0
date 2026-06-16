import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    host: '127.0.0.1',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // B-8 Dashboard 实时告警 WebSocket（/ws/dashboard）—— dev 下转发到后端并升级为
      // WebSocket（ws: true）。缺此项时 resolveDashboardWsUrl() 回落 location.host(:5173)
      // 而 Vite 不转发，实时层在 dev 静默失效、仅轮询兜底（DEF-4 E2E 揭出）。
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
});
