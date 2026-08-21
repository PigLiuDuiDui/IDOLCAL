<script setup>
defineProps({
  rows: { type: Number, default: 6 },
  cols: { type: Number, default: 6 }
})
</script>

<template>
  <div class="a-table-wrap">
    <table class="a-table">
      <thead>
        <tr>
          <th v-for="i in cols" :key="i">
            <span class="a-skeleton" style="display: inline-block; height: 10px; width: 56px" />
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in rows" :key="r">
          <td v-for="c in cols" :key="c">
            <span
              class="a-skeleton"
              style="display: inline-block; height: 12px"
              :style="{ width: `${45 + ((r * 7 + c * 13) % 45)}%` }"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
