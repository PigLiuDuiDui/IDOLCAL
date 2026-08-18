import { defineStore } from 'pinia'

// 全局 UI 状态：活动详情 Drawer / Bottom Sheet
export const useUiStore = defineStore('ui', {
  state: () => ({
    selectedEventId: null,
    drawerOpen: false,
    // 当前活动的筛选类型（多选），空数组 = ALL
    activeTypes: []
  }),
  actions: {
    openEvent(id) {
      this.selectedEventId = id
      this.drawerOpen = true
    },
    closeEvent() {
      this.drawerOpen = false
    },
    toggleType(type) {
      const i = this.activeTypes.indexOf(type)
      if (i >= 0) this.activeTypes.splice(i, 1)
      else this.activeTypes.push(type)
    },
    clearTypes() {
      this.activeTypes = []
    }
  }
})
