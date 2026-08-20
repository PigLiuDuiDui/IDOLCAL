<template>
  <div class="schedule-view">
    <!-- 顶部视图切换：Today / This Week / Calendar -->
    <div class="container">
      <div class="view-switch" role="tablist" :aria-label="t('home.switchView')">
        <button
          type="button"
          role="tab"
          class="vs-btn"
          :class="{ active: view === 'today' }"
          :aria-selected="view === 'today'"
          @click="switchView('today')"
        >
          {{ t('home.today') }}
        </button>
        <button
          type="button"
          role="tab"
          class="vs-btn"
          :class="{ active: view === 'week' }"
          :aria-selected="view === 'week'"
          @click="switchView('week')"
        >
          {{ t('home.thisWeek') }}
        </button>
        <button
          type="button"
          role="tab"
          class="vs-btn"
          :class="{ active: view === 'calendar' }"
          :aria-selected="view === 'calendar'"
          @click="switchView('calendar')"
        >
          {{ t('home.calendar') }}
        </button>
      </div>
    </div>

    <!-- ============ Today / This Week 快捷视图 ============ -->
    <template v-if="view !== 'calendar'">
      <div class="container quick-view">
        <!-- 紧凑页头：艺人 + 本地日期 + 时区 -->
        <header class="quick-head">
          <div class="quick-id">
            <h1 class="quick-name">{{ artist.name }}</h1>
            <p class="quick-date">
              {{ view === 'today' ? t('home.todayLabel') : t('home.thisWeekLabel') }}
              · {{ fullDate(todayLocal) }}
              <span class="quick-tz">{{ tzLabel }}</span>
            </p>
          </div>
        </header>

        <FilterBar />

        <!-- Today：按时间排序 -->
        <section v-if="view === 'today'" class="quick-section" :aria-label="t('home.today')">
          <p v-if="todayEvents.length === 0" class="quick-empty">{{ t('home.todayEmpty') }}</p>
          <div v-else class="quick-list">
            <DayScheduleCard v-for="e in todayEvents" :key="e.id" :event="e" />
          </div>
        </section>

        <!-- This Week：未来 7 天按日期分组 -->
        <section v-else class="quick-section" :aria-label="t('home.thisWeek')">
          <p v-if="weekGroups.length === 0" class="quick-empty">{{ t('home.weekEmpty') }}</p>
          <div v-for="group in weekGroups" :key="group.date" class="week-group">
            <div class="week-group-head">
              <h2 class="week-group-title">{{ weekGroupLabel(group.date, locale) }}</h2>
              <span v-if="isToday(group.date)" class="week-group-tag">{{ t('home.todayTag') }}</span>
            </div>
            <div class="quick-list">
              <DayScheduleCard v-for="e in group.items" :key="e.id" :event="e" />
            </div>
          </div>
        </section>
      </div>
    </template>

    <!-- ============ Calendar 视图（原有主页） ============ -->
    <template v-else>
      <!-- 1. 艺人身份 + 当前时期 + Next Event -->
      <HeroSection />

      <!-- 2. 类型筛选 -->
      <div class="container">
        <FilterBar />
      </div>

      <!-- 3. 桌面：日历 + Upcoming 双栏；移动端：Upcoming 在上，日历在下 -->
      <div class="container schedule-grid">
        <section class="schedule-cal" :aria-label="t('calendar.month')">
          <CalendarPanel />
        </section>

        <aside class="schedule-upcoming" :aria-label="t('upcoming.title')">
          <UpcomingList />
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup>
// 主页：Today / This Week / Calendar 三视图
// Today / Week：移动优先的当日与未来 7 天快捷视图（本地时区分组）
// Calendar：原有 Hero → 筛选 → 日历 / Upcoming
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useDataStore } from '../stores/data'
import { useUiStore } from '../stores/ui'
import { useTimezoneStore } from '../stores/timezone'
import { todayKeyInZone, localizeEvent, tzAbbr, weekGroupLabel } from '../utils/time'
import { fullDate, addDaysKey } from '../utils/date'
import HeroSection from '../components/HeroSection.vue'
import FilterBar from '../components/FilterBar.vue'
import CalendarPanel from '../components/CalendarPanel.vue'
import UpcomingList from '../components/UpcomingList.vue'
import DayScheduleCard from '../components/DayScheduleCard.vue'

const { t, locale } = useI18n()
const ui = useUiStore()
const timezone = useTimezoneStore()
const data = useDataStore()
const artist = computed(() => data.currentArtist)

// 通知点击直达 /#/event/{id}：挂载时打开活动详情抽屉并清除直达参数（避免刷新重复打开）
const route = useRoute()
const router = useRouter()
onMounted(() => {
  const eventId = route.params.id
  if (eventId) {
    ui.openEvent(String(eventId))
    router.replace('/')
  }
})

// 视图模式（持久化）
const VIEW_KEY = 'idolcal-home-view'
const view = ref(localStorage.getItem(VIEW_KEY) || 'today')
if (!['today', 'week', 'calendar'].includes(view.value)) view.value = 'today'

function switchView(v) {
  view.value = v
  localStorage.setItem(VIEW_KEY, v)
}

// 本地时区今天（YYYY-MM-DD）
const todayLocal = computed(() => todayKeyInZone(timezone.displayZone))

const tzLabel = computed(() => tzAbbr(timezone.displayZone, todayLocal.value))

// ---- Today：今天所有活动，按本地时间排序（无明确时间放最后） ----
const todayEvents = computed(() => {
  const today = todayLocal.value
  const filtered = filterByTypes(data.eventsSorted)
  const list = filtered.filter((e) => localizeCached(e).local?.date === today)
  return list.sort(compareByLocalTime)
})

// ---- This Week：未来 7 天（含今天），按本地日期分组 ----
const weekGroups = computed(() => {
  const today = todayLocal.value
  const filtered = filterByTypes(data.eventsSorted)
  const groups = []
  for (let i = 0; i < 7; i++) {
    const date = addDaysKey(today, i)
    const items = filtered.filter((e) => localizeCached(e).local?.date === date)
    if (items.length) groups.push({ date, items: items.sort(compareByLocalTime) })
  }
  return groups
})

function filterByTypes(list) {
  return ui.activeTypes.length === 0 ? list : list.filter((e) => ui.activeTypes.includes(e.type))
}

// 本地时间排序：无 time 的排最后
function compareByLocalTime(a, b) {
  const ta = localizeCached(a).local
  const tb = localizeCached(b).local
  if (!ta) return 1
  if (!tb) return -1
  return ta.time < tb.time ? -1 : 1
}

// localizeEvent 结果缓存：同一事件 + 同一显示时区只做一次 Temporal 时区转换，
// filter / 排序 / 分组共用同一份结果（Temporal 转换是昂贵操作，避免每屏重复计算）
const localizeCache = new Map()
function localizeCached(e) {
  const key = `${e.id}|${timezone.displayZone}`
  const hit = localizeCache.get(key)
  if (hit && hit.event === e) return hit.value // 事件对象已刷新（数据重载）则重新转换
  const value = localizeEvent(e, timezone.displayZone)
  localizeCache.set(key, { event: e, value })
  return value
}

function isToday(dateKey) {
  return dateKey === todayLocal.value
}
</script>

<style scoped>
/* ---------- 视图切换条 ---------- */
.view-switch {
  display: inline-flex;
  border: 1px solid var(--line);
  border-radius: 999px;
  overflow: hidden;
  margin-top: 28px;
  background: var(--surface);
}

.vs-btn {
  min-height: 40px;
  padding: 0 22px;
  border: none;
  background: transparent;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.22em;
  color: var(--ink-soft);
  cursor: pointer;
  transition: all var(--dur) var(--ease);
  text-transform: uppercase;
}

.vs-btn + .vs-btn {
  border-left: 1px solid var(--line);
}

.vs-btn:hover {
  color: var(--ink);
}

.vs-btn.active {
  background: var(--ink);
  color: #fff;
}

/* ---------- 快捷视图 ---------- */
.quick-view {
  padding-top: 28px;
  padding-bottom: 96px;
}

.quick-head {
  margin-bottom: 20px;
}

.quick-name {
  font-family: var(--serif);
  font-size: clamp(34px, 5vw, 48px);
  font-weight: 400;
  letter-spacing: 0.08em;
  line-height: 1.1;
}

.quick-date {
  margin-top: 6px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.24em;
  color: var(--ink-faint);
  text-transform: uppercase;
}

.quick-tz {
  margin-left: 8px;
  color: var(--accent);
  font-family: var(--mono);
}

.quick-section {
  margin-top: 28px;
}

.quick-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-empty {
  margin-top: 40px;
  padding: 56px 24px;
  text-align: center;
  font-size: 12px;
  letter-spacing: 0.16em;
  color: var(--ink-faint);
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius);
}

/* This Week 分组 */
.week-group {
  margin-top: 30px;
}

.week-group-head {
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--line-strong);
  padding-bottom: 10px;
  margin-bottom: 12px;
}

.week-group-title {
  font-family: var(--serif);
  font-size: 17px;
  font-weight: 400;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.week-group-tag {
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.22em;
  color: #fff;
  background: var(--accent);
  padding: 3px 10px;
  border-radius: 999px;
}

/* ---------- Calendar 视图（原有布局） ---------- */
.schedule-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 400px;
  gap: 56px;
  padding-top: 48px;
  padding-bottom: 96px;
  align-items: start;
}

.schedule-upcoming {
  position: sticky;
  top: 96px;
}

/* 移动端 */
@media (max-width: 900px) {
  .schedule-grid {
    display: flex;
    flex-direction: column;
    gap: 56px;
    padding-top: 40px;
    padding-bottom: 64px;
  }

  .schedule-upcoming {
    position: static;
    order: -1;
  }

  .quick-view {
    padding-bottom: 64px;
  }

  .view-switch {
    margin-top: 20px;
    width: 100%;
  }

  .vs-btn {
    flex: 1;
    padding: 0 12px;
    font-size: 9px;
  }
}
</style>
