<template>
  <div class="schedule-view">
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
  </div>
</template>

<script setup>
// 主页：Hero → 筛选 → 日历 / Upcoming（桌面双栏，移动端重排）
import { useI18n } from 'vue-i18n'
import HeroSection from '../components/HeroSection.vue'
import FilterBar from '../components/FilterBar.vue'
import CalendarPanel from '../components/CalendarPanel.vue'
import UpcomingList from '../components/UpcomingList.vue'

const { t } = useI18n()
</script>

<style scoped>
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

/* 移动端：单列，Upcoming 优先于日历 */
@media (max-width: 1200px) {
  .schedule-grid {
    grid-template-columns: minmax(0, 1fr) 340px;
    gap: 40px;
  }
}

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
}
</style>
