<script setup>
import Icon from './Icon.vue'

defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  /** 抽屉宽度（px） */
  width: { type: Number, default: 560 }
})

const emit = defineEmits(['close'])
</script>

<template>
  <Teleport to="body">
    <div v-if="open">
      <div class="a-drawer-overlay" @click="emit('close')" />
      <aside class="a-drawer" :style="{ width: `min(${width}px, 92vw)` }" role="dialog" aria-modal="true">
        <div class="a-drawer-head">
          <h3>{{ title }}</h3>
          <button class="a-icon-btn" @click="emit('close')">
            <Icon name="x" :size="16" />
          </button>
        </div>
        <div class="a-drawer-body">
          <slot />
        </div>
        <div v-if="$slots.footer" class="a-drawer-foot">
          <slot name="footer" />
        </div>
      </aside>
    </div>
  </Teleport>
</template>
