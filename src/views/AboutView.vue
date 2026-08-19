<template>
  <div class="about-view container">
    <header class="page-head">
      <p class="eyebrow">{{ t('about.eyebrow') }}</p>
      <h1 class="page-title">ABOUT</h1>
    </header>

    <div class="about-grid">
      <!-- 左侧：艺人信息 -->
      <section class="about-artist">
        <h2 class="about-section-title">{{ artist.name }}</h2>
        <p class="about-intro">{{ text(artist.intro) }}</p>

        <dl class="about-facts">
          <div class="fact">
            <dt>CURRENT ERA</dt>
            <dd>{{ artist.era }}</dd>
          </div>
          <div class="fact">
            <dt>ERA PERIOD</dt>
            <dd>{{ artist.eraPeriod }}</dd>
          </div>
          <div class="fact">
            <dt>ARCHIVE YEAR</dt>
            <dd>{{ artist.year }}</dd>
          </div>
        </dl>
      </section>

      <!-- 右侧：来源可信度说明 -->
      <section class="about-sources">
        <h2 class="about-section-title">{{ t('about.sourcePolicy') }}</h2>
        <p class="about-note">
          {{ t('about.note') }}
        </p>

        <ul class="level-list">
          <li class="level-item">
            <span class="level-tag" data-level="OFFICIAL">Official</span>
            <div class="level-info">
              <strong>{{ t('about.levels.official.name') }}</strong>
              <p>{{ t('about.levels.official.desc') }}</p>
            </div>
          </li>
          <li class="level-item">
            <span class="level-tag" data-level="BRAND">Brand</span>
            <div class="level-info">
              <strong>{{ t('about.levels.brand.name') }}</strong>
              <p>{{ t('about.levels.brand.desc') }}</p>
            </div>
          </li>
          <li class="level-item">
            <span class="level-tag" data-level="MEDIA">Media</span>
            <div class="level-info">
              <strong>{{ t('about.levels.media.name') }}</strong>
              <p>{{ t('about.levels.media.desc') }}</p>
            </div>
          </li>
          <li class="level-item">
            <span class="level-tag" data-level="FAN">Fan Project</span>
            <div class="level-info">
              <strong>{{ t('about.levels.fan.name') }}</strong>
              <p>{{ t('about.levels.fan.desc') }}</p>
            </div>
          </li>
        </ul>
      </section>
    </div>

    <footer class="about-foot">
      <p>
        {{ t('about.footer', { artist: artist.name }) }}
      </p>
      <p class="about-foot-note">{{ t('about.footNote', { year: artist.year }) }}</p>
    </footer>
  </div>
</template>

<script setup>
// About：档案站说明 + 来源可信度政策
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDataStore } from '../stores/data'
import { useText } from '../i18n'

const { t } = useI18n()
const text = useText()
const data = useDataStore()
const artist = computed(() => data.currentArtist)
</script>

<style scoped>
.about-view {
  padding-top: 72px;
  padding-bottom: 120px;
  max-width: 1080px;
}

.page-head {
  margin-bottom: 56px;
}

.page-title {
  font-family: var(--serif);
  font-size: clamp(40px, 6vw, 64px);
  font-weight: 400;
  letter-spacing: 0.1em;
  margin: 12px 0 0;
}

.about-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 64px;
  align-items: start;
}

.about-section-title {
  font-family: var(--serif);
  font-size: 22px;
  font-weight: 400;
  letter-spacing: 0.1em;
  border-bottom: 1px solid var(--line-strong);
  padding-bottom: 14px;
  margin-bottom: 24px;
}

.about-intro {
  font-size: 14px;
  line-height: 1.9;
  color: var(--ink-soft);
  margin-bottom: 28px;
}

.about-facts {
  border-top: 1px solid var(--line);
}

.fact {
  display: flex;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
}

.fact dt {
  flex-shrink: 0;
  width: 120px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: var(--ink-faint);
  padding-top: 2px;
}

.fact dd {
  font-family: var(--serif);
  font-size: 15px;
  letter-spacing: 0.06em;
}

.about-note {
  font-size: 13px;
  line-height: 1.8;
  color: var(--ink-soft);
  margin-bottom: 28px;
}

/* 来源等级列表 */
.level-list {
  list-style: none;
  display: flex;
  flex-direction: column;
}

.level-item {
  display: flex;
  gap: 18px;
  padding: 16px 0;
  border-bottom: 1px solid var(--line);
  align-items: flex-start;
}

.level-tag {
  flex-shrink: 0;
  min-width: 92px;
  text-align: center;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid var(--line-strong);
  color: var(--ink-soft);
  margin-top: 2px;
}

.level-tag[data-level='OFFICIAL'] {
  color: var(--accent);
  border-color: color-mix(in srgb, var(--accent) 40%, transparent);
  background: var(--accent-soft);
}

.level-info strong {
  display: block;
  font-size: 12px;
  letter-spacing: 0.16em;
  margin-bottom: 4px;
}

.level-info p {
  font-size: 12px;
  line-height: 1.75;
  color: var(--ink-soft);
}

/* 底部说明 */
.about-foot {
  margin-top: 80px;
  border-top: 1px solid var(--line);
  padding-top: 28px;
  font-size: 12px;
  color: var(--ink-faint);
  line-height: 1.9;
}

.about-foot-note {
  margin-top: 8px;
  font-size: 9px;
  letter-spacing: 0.26em;
}

@media (max-width: 900px) {
  .about-grid {
    grid-template-columns: 1fr;
    gap: 48px;
  }

  .about-view {
    padding-top: 48px;
    padding-bottom: 80px;
  }
}
</style>
