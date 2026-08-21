<script setup>
// 管理员登录页：POST /api/admin/login → JWT 持久化 → 跳转 Dashboard
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import { adminLogin, isAuthed, authState } from '../api'
import { t } from '../i18n'

const router = useRouter()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

onMounted(() => {
  if (isAuthed()) router.replace('/admin/dashboard')
  // 预填默认管理员名（仅本地开发体验）
  if (!username.value) username.value = 'admin'
})

async function submit() {
  if (loading.value) return
  error.value = ''
  loading.value = true
  try {
    await adminLogin(username.value.trim(), password.value)
    router.replace('/admin/dashboard')
  } catch (e) {
    error.value = e.message || t('login.failed')
  } finally {
    loading.value = false
  }
}

function onKeydown(e) {
  if (e.key === 'Enter') submit()
}
</script>

<template>
  <div class="admin-app login-page">
    <div class="login-card">
      <div class="login-brand">
        <div class="login-logo">IC</div>
        <h1>{{ t('login.title') }}</h1>
        <p>{{ t('login.subtitle') }}</p>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <div v-if="error" class="a-error-box">
          <Icon name="alert-triangle" :size="16" />
          <span>{{ error }}</span>
        </div>

        <div class="a-field">
          <label for="a-username">{{ t('login.username') }}</label>
          <input
            id="a-username"
            v-model="username"
            class="a-input"
            autocomplete="username"
            placeholder="admin"
            :disabled="loading"
          />
        </div>

        <div class="a-field">
          <label for="a-password">{{ t('login.password') }}</label>
          <input
            id="a-password"
            v-model="password"
            class="a-input"
            type="password"
            autocomplete="current-password"
            placeholder="••••••••"
            :disabled="loading"
            @keydown="onKeydown"
          />
        </div>

        <button class="a-btn a-btn-primary login-btn" type="submit" :disabled="loading">
          <Icon v-if="loading" name="loader" :size="15" class="spin" />
          {{ loading ? t('login.signingIn') : t('login.signIn') }}
        </button>

        <p class="login-foot">
          {{ t('login.foot') }}
        </p>
      </form>
    </div>

    <a class="back-link" href="#/">
      <Icon name="arrow-left" :size="14" />
      {{ t('login.back') }}
    </a>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--a-bg);
  position: relative;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: var(--a-card);
  border: 1px solid var(--a-border);
  border-radius: 14px;
  box-shadow: var(--a-shadow-lg);
  padding: 36px 36px 28px;
}

.login-brand {
  text-align: center;
  margin-bottom: 28px;
}

.login-logo {
  width: 46px;
  height: 46px;
  margin: 0 auto 14px;
  border-radius: 12px;
  background: var(--a-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 0.02em;
}

.login-brand h1 {
  font-size: 20px;
  font-weight: 700;
  color: var(--a-text);
}

.login-brand p {
  font-size: 12.5px;
  color: var(--a-text-3);
  margin-top: 4px;
  letter-spacing: 0.02em;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login-btn {
  height: 40px;
  font-size: 14px;
  margin-top: 4px;
}

.login-foot {
  text-align: center;
  font-size: 11.5px;
  color: var(--a-text-3);
}

.back-link {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--a-text-2);
  text-decoration: none;
}
.back-link:hover {
  color: var(--a-primary);
}

.spin {
  animation: spin 0.9s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
