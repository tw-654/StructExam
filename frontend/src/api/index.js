import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || 'Request failed')
      return Promise.reject(new Error(res.message || 'Request failed'))
    }
    return res
  },
  error => {
    if (error.response) {
      if (error.response.status === 401) {
        localStorage.removeItem('token')
        router.push('/login')
        ElMessage.error('Authentication expired, please login again')
      } else {
        ElMessage.error(error.response.data?.message || 'Request failed')
      }
    } else {
      ElMessage.error('Network error')
    }
    return Promise.reject(error)
  }
)

export default api
