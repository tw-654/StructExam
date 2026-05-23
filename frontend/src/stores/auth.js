import { defineStore } from 'pinia'
import { authApi } from '@/api/modules'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: localStorage.getItem('role')
      ? {
          id: localStorage.getItem('userId'),
          username: localStorage.getItem('username') || '',
          realName: localStorage.getItem('realName') || '',
          role: localStorage.getItem('role') || ''
        }
      : null
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
      const userId = res.data.id || res.data.userId || '1'
      this.userInfo = {
        id: userId,
        username: res.data.username,
        realName: res.data.realName,
        role: res.data.role
      }
      localStorage.setItem('token', this.token)
      localStorage.setItem('userId', userId)
      localStorage.setItem('username', res.data.username)
      localStorage.setItem('realName', res.data.realName)
      localStorage.setItem('role', res.data.role)
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
        localStorage.removeItem('userId')
        localStorage.removeItem('username')
        localStorage.removeItem('realName')
        localStorage.removeItem('role')
      }
    },
    async fetchUserInfo() {
      try {
        const res = await authApi.getUserInfo()
        this.userInfo = res.data
        localStorage.setItem('userId', res.data.id)
        localStorage.setItem('username', res.data.username)
        localStorage.setItem('realName', res.data.realName)
        localStorage.setItem('role', res.data.role)
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
