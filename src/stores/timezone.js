import { defineStore } from 'pinia'
import { browserZone } from '../utils/time'

// 显示时区状态：'auto' = 跟随浏览器时区，其余为 IANA 时区
export const useTimezoneStore = defineStore('timezone', {
  state: () => ({
    // 用户手动选择的时区（'auto' 或 IANA 时区），持久化
    zone: localStorage.getItem('idolcal-timezone') || 'auto'
  }),
  getters: {
    /** 实际生效的显示时区（IANA） */
    displayZone(state) {
      return state.zone === 'auto' ? browserZone() : state.zone
    },
    /** 是否跟随浏览器时区 */
    isAuto(state) {
      return state.zone === 'auto'
    }
  },
  actions: {
    setZone(zone) {
      this.zone = zone
      localStorage.setItem('idolcal-timezone', zone)
    }
  }
})
