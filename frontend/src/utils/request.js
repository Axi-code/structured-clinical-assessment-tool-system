import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { getAccessToken, setAccessToken, clearAccessToken, notifyRefreshSuccess } from './authToken'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  withCredentials: true
})

// 请求拦截器：从内存取 accessToken 放入 Header
request.interceptors.request.use(
  config => {
    const token = getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器：200 正常返回；非 200 构造带 code 的 Error；401 时尝试刷新后重试
request.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const res = response.data
    const msg = res.msg || res.message || '请求失败'
    if (res.code === 200) {
      return res
    }
    ElMessage.error(msg)
    const err = new Error(msg)
    err.code = res.code
    err.message = msg
    return Promise.reject(err)
  },
  async error => {
    const originalRequest = error.config

    if (error.response && error.response.status === 401) {
      if (originalRequest._isRefreshRequest) {
        clearAccessToken()
        ElMessage.error('登录已过期，请重新登录')
        router.push('/login')
        return Promise.reject(error)
      }
      if (!originalRequest._retried) {
        originalRequest._retried = true
        try {
          const refreshRes = await request.post('/user/refresh', {}, { _isRefreshRequest: true })
          if (refreshRes && refreshRes.code === 200 && refreshRes.data && refreshRes.data.token) {
            setAccessToken(refreshRes.data.token)
            notifyRefreshSuccess(refreshRes.data)
            originalRequest.headers.Authorization = `Bearer ${refreshRes.data.token}`
            return request(originalRequest)
          }
        } catch (e) {
          // refresh 失败，下面统一处理
        }
        clearAccessToken()
        ElMessage.error('登录已过期，请重新登录')
        router.push('/login')
      }
      return Promise.reject(error)
    }

    if (error.response) {
      const msg = error.response.data?.msg || error.response.data?.message || '请求失败'
      if (error.response.status !== 401) ElMessage.error(msg)
      const err = new Error(msg)
      err.code = error.response.data?.code
      err.message = msg
      return Promise.reject(err)
    }
    ElMessage.error('网络错误，请检查网络连接')
    return Promise.reject(error)
  }
)

export default request
