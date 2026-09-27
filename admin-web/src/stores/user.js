import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '@/api'
import { setToken, clearToken } from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    name: localStorage.getItem('campus_admin_name') || '',
  }),
  actions: {
    async login(form) {
      const data = await loginApi(form)
      setToken(data.token)
      this.name = data.name || data.userName
      localStorage.setItem('campus_admin_name', this.name)
      return data
    },
    async logout() {
      try {
        await logoutApi()
      } finally {
        clearToken()
        localStorage.removeItem('campus_admin_name')
        this.name = ''
      }
    },
  },
})
