import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  // 👇👇👇 新增这部分配置 👇👇👇
  server: {
    port: 5173, // 确保端口固定
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 你的后端地址 (Spring Boot)
        changeOrigin: true,
        // 如果你的后端 Controller @RequestMapping("/api/...") 包含了 /api，则不需要 rewrite
        // 你的代码里 ProductController 是 @RequestMapping("/api/products")，所以不需要 rewrite
      }
    }
  }
})