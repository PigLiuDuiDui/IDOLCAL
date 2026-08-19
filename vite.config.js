import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    open: false,
    // 允许所有主机访问（cpolar/ngrok 等动态内网穿透域名）
    // 注意：Vite 5.x 的放行值是 true，'all' 是 Vite 6+ 的写法
    allowedHosts: true,
    // 开发环境：/api 转发到本地 Spring Boot 后端（api 目录）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})