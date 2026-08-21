<script setup>
import { computed } from 'vue'
import Icon from './Icon.vue'
import { t } from '../i18n'

const props = defineProps({
  page: { type: Number, default: 0 },
  size: { type: Number, default: 20 },
  total: { type: Number, default: 0 }
})

const emit = defineEmits(['change'])

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.size)))

/** 页码窗口：最多 5 个 */
const pages = computed(() => {
  const t = totalPages.value
  if (t <= 5) return Array.from({ length: t }, (_, i) => i)
  let start = Math.max(0, Math.min(props.page - 2, t - 5))
  return Array.from({ length: 5 }, (_, i) => start + i)
})

const rangeText = computed(() => {
  if (props.total === 0) return t('pagination.zero')
  const from = props.page * props.size + 1
  const to = Math.min((props.page + 1) * props.size, props.total)
  return t('pagination.range', { from, to, total: props.total })
})
</script>

<template>
  <div class="a-pagination">
    <span>{{ rangeText }}</span>
    <div class="a-pagination-pages">
      <button class="a-page-btn" :disabled="page === 0" @click="emit('change', page - 1)">
        <Icon name="chevron-left" :size="14" />
      </button>
      <button
        v-for="p in pages"
        :key="p"
        class="a-page-btn"
        :class="{ active: p === page }"
        @click="emit('change', p)"
      >
        {{ p + 1 }}
      </button>
      <button class="a-page-btn" :disabled="page >= totalPages - 1" @click="emit('change', page + 1)">
        <Icon name="chevron-right" :size="14" />
      </button>
    </div>
  </div>
</template>
