// ============================================================
// 管理后台 API 封装：JWT 令牌管理 + 统一请求/错误处理
// - token 持久化到 localStorage（idolcal-admin-token）
// - 所有请求自动携带 Authorization: Bearer <token>
// - 401 时自动清除令牌并跳转登录页
// - 统一抛出带 message 的 Error（后端 { error, message } 结构）
// ============================================================
import { reactive } from 'vue'

const API_BASE = import.meta.env.VITE_API_BASE || ''
const TOKEN_KEY = 'idolcal-admin-token'
const USER_KEY = 'idolcal-admin-user'

/** 登录态（响应式，供布局/守卫使用） */
export const authState = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  username: localStorage.getItem(USER_KEY) || '',
  role: localStorage.getItem('idolcal-admin-role') || ''
})

export function isAuthed() {
  return !!authState.token
}

export function saveAuth({ token, role, username }) {
  authState.token = token
  authState.role = role
  authState.username = username
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem('idolcal-admin-role', role || '')
  if (username) localStorage.setItem(USER_KEY, username)
}

export function clearAuth() {
  authState.token = ''
  authState.role = ''
  authState.username = ''
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('idolcal-admin-role')
  localStorage.removeItem(USER_KEY)
}

/** 登录（成功保存凭证；失败抛出带后端 message 的错误） */
export async function adminLogin(username, password) {
  const res = await fetch(`${API_BASE}/api/admin/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  })
  const data = await parseJson(res)
  if (!res.ok) throw new Error(data?.message || `Login failed (${res.status})`)
  saveAuth({ token: data.token, role: data.role, username })
  return data
}

/** 通用 GET */
export async function adminGet(path, params = {}) {
  const qs = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') qs.set(k, v)
  }
  const url = `${API_BASE}${path}${qs.size ? `?${qs}` : ''}`
  const res = await fetch(url, { headers: authHeaders() })
  const data = await parseJson(res)
  if (!res.ok) {
    if (res.status === 401) handleUnauthorized()
    throw new Error(data?.message || `Request failed (${res.status})`)
  }
  return data
}

/** 通用写请求（POST / PUT / DELETE） */
export async function adminSend(method, path, body, params = {}) {
  const qs = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') qs.set(k, v)
  }
  const url = `${API_BASE}${path}${qs.size ? `?${qs}` : ''}`
  const res = await fetch(url, {
    method,
    headers: { ...authHeaders(), ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}) },
    body: body !== undefined ? JSON.stringify(body) : undefined
  })
  const data = await parseJson(res)
  if (!res.ok) {
    if (res.status === 401) handleUnauthorized()
    throw new Error(data?.message || `Request failed (${res.status})`)
  }
  return data
}

export const adminApi = {
  get: adminGet,
  post: (path, body) => adminSend('POST', path, body),
  put: (path, body) => adminSend('PUT', path, body),
  del: (path) => adminSend('DELETE', path)
}

function authHeaders() {
  return authState.token ? { Authorization: `Bearer ${authState.token}` } : {}
}

async function parseJson(res) {
  try {
    return await res.json()
  } catch {
    return null
  }
}

function handleUnauthorized() {
  clearAuth()
  if (location.hash.startsWith('#/admin') && !location.hash.startsWith('#/admin/login')) {
    location.hash = '#/admin/login'
  }
}
