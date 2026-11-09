import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  server: {
    port: 5173,
    proxy: {
      // 凡是遇到 /file 开头的请求，都代理到后端服务
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        // 核心：把请求路径中的 /api 替换为空字符串
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // 加上对 /sse 的代理
      '/sse': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
