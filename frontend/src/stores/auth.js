import { defineStore } from 'pinia'
import { authApi } from '@/api/modules'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null
  }),
  getters: {
    isLoggedIn: state => !!state.token,
    username: state => state.userInfo?.username || '',
    realName: state => state.userInfo?.realName || '',
    role: state => state.userInfo?.role || ''
  },
  actions: {
    async login(loginForm) {
      const res = await authApi.login(loginForm)
      this.token = res.data.token
      this.userInfo = {
        username: res.data.username,
        realName: res.data.realName,
        role: res.data.role
      }
      localStorage.setItem('token', this.token)
      return res.data
    },
    async register(registerForm) {
      return await authApi.register(registerForm)
    },
    async logout() {
      try {
        await authApi.logout()
      } finally {
        this.token = ''
        this.userInfo = null
        localStorage.removeItem('token')
      }
    },
    async fetchUserInfo() {
      try {
        const res = await authApi.getUserInfo()
        this.userInfo = res.data
        return res.data
      } catch (error) {
        this.logout()
        throw error
      }
    },
    async updatePassword(oldPassword, newPassword) {
      return await authApi.updatePassword(oldPassword, newPassword)
    }
  }
})
