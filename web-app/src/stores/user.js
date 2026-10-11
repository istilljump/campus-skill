import { defineStore } from 'pinia'
import {
  accountLogin, register, guestLogin, changePassword,
  skillerAccountLogin, skillerGuestLogin,
  getProfile, countUnread, skCountUnread,
} from '@/api'
import { getUserToken, getRunnerToken, setUserToken, setRunnerToken, clearAllTokens } from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    userToken: getUserToken(),
    runnerToken: getRunnerToken(),
    profile: {},           // 用户端个人信息
    center: {},            // 技能者端个人中心
    unread: 0,             // 当前身份未读消息数
    ws: null,
    wsTimer: null,
  }),
  getters: {
    hasUser: (s) => !!s.userToken,
    hasSkiller: (s) => !!s.runnerToken,
  },
  actions: {
    async loginAsUser(form) {
      const data = await accountLogin(form)
      setUserToken(data.token)
      this.userToken = data.token
    },
    async registerAsUser(form) {
      const data = await register(form)
      setUserToken(data.token)
      this.userToken = data.token
    },
    async guestLoginAsUser(code) {
      const data = await guestLogin({ code })
      setUserToken(data.token)
      this.userToken = data.token
    },
    async loginAsSkiller(form) {
      const data = await skillerAccountLogin(form)
      setRunnerToken(data.token)
      this.runnerToken = data.token
    },
    async guestLoginAsSkiller(code) {
      const data = await skillerGuestLogin({ code })
      setRunnerToken(data.token)
      this.runnerToken = data.token
    },
    async loadProfile() {
      if (!this.userToken) return
      this.profile = await getProfile() || {}
    },
    async loadUnread(area) {
      try {
        this.unread = area === 'sk' ? (await skCountUnread()) || 0 : (await countUnread()) || 0
      } catch (e) { this.unread = 0 }
    },
    async logout() {
      clearAllTokens()
      this.userToken = ''
      this.runnerToken = ''
      this.profile = {}
      this.unread = 0
      this.closeWs()
    },
    async changePassword(form) {
      await changePassword(form)
    },
    // WebSocket 实时推送：与 H5 端一致（断线 10s 重连，消息角标 +1）
    connectWs(token) {
      if (!token) return
      this.closeWs()
      clearTimeout(this.wsTimer)
      const proto = location.protocol === 'https:' ? 'wss' : 'ws'
      try {
        this.ws = new WebSocket(`${proto}://${location.host}/ws/${encodeURIComponent(token)}`)
      } catch (e) { return }
      this.ws.onmessage = (ev) => {
        try {
          const d = JSON.parse(ev.data)
          if (d.type === 'message') {
            import('element-plus').then(({ ElNotification }) => {
              ElNotification({ title: d.title, message: d.content || '', type: 'info', duration: 4000 })
            })
            this.unread += 1
          }
        } catch (e) {}
      }
      this.ws.onclose = () => {
        this.wsTimer = setTimeout(() => {
          const t = getUserToken() || getRunnerToken()
          if (t) this.connectWs(t)
        }, 10000)
      }
      this.ws.onerror = () => { try { this.ws.close() } catch (e) {} }
    },
    closeWs() {
      if (this.ws) { try { this.ws.onclose = null; this.ws.close() } catch (e) {} }
      this.ws = null
    },
  },
})
