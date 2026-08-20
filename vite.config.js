import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    open: false,
    host: true, // 服务监听全部网卡，内网/内网穿透需要这个
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    },
    // 本地 localhost 访问，hmr.host必须写localhost
    hmr: {
      host: 'localhost',
      port: 5173
    }
  }
})