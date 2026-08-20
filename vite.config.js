import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// 内网 / 内网穿透访问 dev server 时，用 VITE_ALLOWED_HOSTS 显式声明允许的 Host（逗号分隔），
// 未配置时仅允许 localhost / 127.0.0.1——防止 dev 代理被公网任意访问者利用。
// 注意：配置加载早于 .env 注入，必须用 loadEnv 读取（process.env 只能拿到系统环境变量）。
export default defineConfig(({ mode }) => {
  // .env 中以 VITE_ 前缀声明的变量（含 .env.local / .env.[mode]），
  // 支持 .cpolar.top 这类前缀点通配，允许该域下所有子域
  const env = loadEnv(mode, process.cwd(), '')
  const extraHosts = (env.VITE_ALLOWED_HOSTS || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)

  const allowedHosts = ['localhost', '127.0.0.1', ...extraHosts]

  return {
    plugins: [vue()],
    server: {
      port: 5173,
      open: false,
      host: true, // 服务监听全部网卡，内网/内网穿透需要这个
      allowedHosts,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true
        }
      },
      // 本地 localhost 访问，hmr.host必须写localhost；
      // protocol 固定 ws：经 cpolar https 域名访问时，默认会推断 wss 连 dev server（http）导致 ERR_SSL_PROTOCOL_ERROR；
      // ws://localhost 在 https 页面中属于 localhost 安全上下文豁免，不受 mixed content 拦截
      hmr: {
        host: 'localhost',
        port: 5173,
        protocol: 'ws'
      }
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            // 框架运行时与 Temporal polyfill 拆为独立 vendor 包（配合路由懒加载，首屏只加载所需部分）
            'vendor-vue': ['vue', 'vue-router', 'pinia', 'vue-i18n'],
            'vendor-calendar': ['@fullcalendar/vue3'],
            'vendor-temporal': ['temporal-polyfill']
          }
        }
      }
    }
  }
})