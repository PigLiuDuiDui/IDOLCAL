<template>
  <section class="tutorial">
    <div class="container">
      <div class="tutorial-head">
        <p class="eyebrow">{{ t('tutorial.eyebrow') }}</p>
        <h1 class="tutorial-title">{{ t('tutorial.title') }}</h1>
        <p class="tutorial-sub">{{ t('tutorial.sub') }}</p>
        <p class="tutorial-count">{{ t('tutorial.count', { n: data.readyBoards.length }) }}</p>
      </div>

      <!-- 板块卡片 -->
      <div class="board-grid">
        <button
          v-for="(board, i) in data.tutorialBoards"
          :key="board.id"
          type="button"
          class="board-card"
          :class="{ active: selected && selected.id === board.id, coming: board.status === 'coming' }"
          :disabled="board.status === 'coming'"
          @click="select(board)"
        >
          <span class="board-index">{{ pad(i + 1) }}</span>
          <span class="board-title">{{ board.title }}</span>
          <span class="board-tagline">{{ board.tagline }}</span>
          <span v-if="board.status === 'coming'" class="board-badge">{{ t('tutorial.coming') }}</span>
        </button>
      </div>

      <!-- 板块详情 -->
      <div v-if="selected" class="board-detail">
        <div class="detail-head">
          <h2 class="detail-title">{{ selected.title }}</h2>
          <p class="detail-tagline">{{ selected.tagline }}</p>
        </div>

        <div class="detail-sections">
          <div v-for="section in selected.sections" :key="section.title" class="detail-section">
            <h3 class="section-title">{{ section.title }}</h3>
            <ol class="section-items">
              <li v-for="(item, i) in section.items" :key="i" class="section-item">{{ item }}</li>
            </ol>
          </div>
        </div>

        <!-- 注意事项 -->
        <div v-if="selected.notes && selected.notes.length" class="detail-notes">
          <h3 class="section-title">{{ t('tutorial.notes') }}</h3>
          <ol class="section-items notes-list">
            <li v-for="(note, i) in selected.notes" :key="i" class="section-item">{{ note }}</li>
          </ol>
        </div>

        <!-- 不计入问题（Melon 专用） -->
        <div v-if="selected.noCount && selected.noCount.length" class="detail-notes">
          <h3 class="section-title">{{ t('tutorial.noCount') }}</h3>
          <ol class="section-items notes-list">
            <li v-for="(note, i) in selected.noCount" :key="i" class="section-item">{{ note }}</li>
          </ol>
        </div>
      </div>

      <!-- 待开发占位说明 -->
      <p v-if="data.comingBoards.length" class="tutorial-footnote">
        {{ t('tutorial.footnote', { n: data.comingBoards.length }) }}
      </p>
    </div>
  </section>
</template>

<script setup>
// 打榜指南：板块列表 + 详情（ready 板块可查看，coming 板块待开发）
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDataStore } from '../stores/data'

const { t } = useI18n()
const data = useDataStore()

const selected = ref(null)

const pad = (n) => String(n).padStart(2, '0')

function select(board) {
  selected.value = board
}
</script>

<style scoped>
.tutorial {
  padding: 56px 0 80px;
}

.tutorial-head {
  margin-bottom: 40px;
  max-width: 640px;
}

.tutorial-eyebrow {
  color: var(--accent);
}

.tutorial-title {
  font-family: var(--serif);
  font-size: clamp(34px, 5vw, 52px);
  font-weight: 400;
  letter-spacing: 0.04em;
  margin: 10px 0 14px;
}

.tutorial-sub {
  font-size: 13px;
  line-height: 1.8;
  color: var(--ink-soft);
}

.tutorial-count {
  margin-top: 14px;
  font-family: var(--mono);
  font-size: 11px;
  letter-spacing: 0.2em;
  color: var(--ink-faint);
}

/* ---- 板块卡片网格 ---- */
.board-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 48px;
}

.board-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 22px 20px 20px;
  text-align: left;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  cursor: pointer;
  transition: border-color var(--dur) var(--ease), transform var(--dur) var(--ease),
    box-shadow var(--dur) var(--ease);
}

.board-card:hover:not(:disabled) {
  border-color: var(--ink);
  transform: translateY(-2px);
}

.board-card.active {
  border-color: var(--accent);
  box-shadow: 0 8px 24px rgba(166, 47, 47, 0.1);
}

.board-card.coming {
  cursor: not-allowed;
  opacity: 0.55;
}

.board-index {
  font-family: var(--mono);
  font-size: 10px;
  letter-spacing: 0.18em;
  color: var(--ink-faint);
}

.board-title {
  font-family: var(--serif);
  font-size: 19px;
  letter-spacing: 0.04em;
  color: var(--ink);
}

.board-tagline {
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--ink-soft);
}

.board-badge {
  position: absolute;
  top: 14px;
  right: 14px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--ink-faint);
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 3px 10px;
}

/* ---- 板块详情 ---- */
.board-detail {
  border-top: 1px solid var(--line);
  padding-top: 40px;
}

.detail-head {
  margin-bottom: 32px;
}

.detail-title {
  font-family: var(--serif);
  font-size: 28px;
  font-weight: 400;
  letter-spacing: 0.04em;
  margin-bottom: 8px;
}

.detail-tagline {
  font-size: 11px;
  letter-spacing: 0.18em;
  color: var(--ink-soft);
}

.detail-sections {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 32px 48px;
}

.detail-section,
.detail-notes {
  break-inside: avoid;
}

.section-title {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: var(--accent);
  text-transform: uppercase;
  border-bottom: 1px solid var(--line);
  padding-bottom: 10px;
  margin-bottom: 16px;
}

.section-items {
  margin: 0;
  padding-left: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-item {
  position: relative;
  padding-left: 22px;
  font-size: 13px;
  line-height: 1.75;
  color: var(--ink-soft);
}

.section-item::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 0.72em;
  width: 6px;
  height: 6px;
  background: var(--accent-soft);
  border-radius: 50%;
}

.notes-list .section-item::before {
  background: var(--accent);
}

.detail-notes {
  margin-top: 40px;
  max-width: 720px;
}

.tutorial-footnote {
  margin-top: 48px;
  font-size: 11px;
  letter-spacing: 0.12em;
  color: var(--ink-faint);
}

/* 响应式 */
@media (max-width: 900px) {
  .board-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 560px) {
  .board-grid {
    grid-template-columns: 1fr;
  }

  .detail-sections {
    grid-template-columns: 1fr;
  }
}
</style>
