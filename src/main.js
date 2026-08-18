import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import i18n, { applyDocumentLocale } from './i18n'
import './style.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)

// 同步 <html lang> 与文档标题 / 描述（按检测到的语言）
applyDocumentLocale()

app.mount('#app')
