import { defineStore } from 'pinia'
import { userApi } from '../api'
import { setAccessToken, clearAccessToken, setOnRefreshSuccess } from '../utils/authToken'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: null,
    _refreshCallbackRegistered: false
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    userRole: (state) => state.userInfo?.role || '',
    isAdmin: (state) => state.userInfo?.role === 'ADMIN'
  },

  actions: {
    _registerRefreshCallback() {
      if (this._refreshCallbackRegistered) return
      this._refreshCallbackRegistered = true
      const store = this
      setOnRefreshSuccess((data) => {
        if (!data) return
        store.token = data.token || ''
        store.userInfo = data.userId != null ? {
          id: data.userId,
          username: data.username,
          realName: data.realName,
          role: data.role,
          department: data.department,
          departmentId: data.departmentId
        } : null
      })
    },

    setUserFromRefresh(data) {
      if (!data) return
      this.token = data.token || ''
      this.userInfo = data.userId != null ? {
        id: data.userId,
        username: data.username,
        realName: data.realName,
        role: data.role,
        department: data.department
      } : null
    },

    async login(loginData) {
      const res = await userApi.login(loginData)
      const data = res.data
      this.token = data.token
      this.userInfo = {
        id: data.userId,
        username: data.username,
        realName: data.realName,
        role: data.role,
        department: data.department,
        departmentId: data.departmentId
      }
      setAccessToken(data.token)
      return res
    },

    async tryRestoreSession() {
      this._registerRefreshCallback()
      try {
        const res = await userApi.refresh()
        if (res && res.code === 200 && res.data && res.data.token) {
          this.token = res.data.token
          this.userInfo = res.data.userId != null ? {
            id: res.data.userId,
            username: res.data.username,
            realName: res.data.realName,
            role: res.data.role,
            department: res.data.department,
            departmentId: res.data.departmentId
          } : null
          setAccessToken(res.data.token)
          return true
        }
      } catch (e) {
        // 无 refresh cookie 或已过期
      }
      this.token = ''
      this.userInfo = null
      clearAccessToken()
      return false
    },

    async logout() {
      try {
        await userApi.logout()
      } catch (e) {
        // 可能已过期等，仍清空本地
      }
      this.token = ''
      this.userInfo = null
      clearAccessToken()
    },

    async getUserInfo() {
      const res = await userApi.getUserInfo()
      this.userInfo = res.data
      return res
    }
  }
})
