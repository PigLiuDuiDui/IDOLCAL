// ============================================================
// Toast 轻提示：模块级响应式列表 + 全局方法
// 用法：import { toast } from '../toast'; toast.success('已保存')
// ============================================================
import { reactive } from 'vue'

export const toasts = reactive([])

let seed = 0

function push(type, message, duration = 3200) {
  const id = ++seed
  toasts.push({ id, type, message })
  setTimeout(() => dismiss(id), duration)
}

export const toast = {
  success: (m) => push('success', m),
  error: (m) => push('error', m),
  warning: (m) => push('warning', m),
  info: (m) => push('info', m)
}

export function dismiss(id) {
  const i = toasts.findIndex((t) => t.id === id)
  if (i >= 0) toasts.splice(i, 1)
}
