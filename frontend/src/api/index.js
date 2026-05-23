import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const MAX_RETRIES = 2
const RETRY_DELAY = 1000

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  responseType: 'json',
  responseEncoding: 'utf8'
})

api.interceptors.request.use(
  config => {
    console.log(`请求发送: ${config.method?.toUpperCase()} ${config.url}, 重试次数: ${config.retry || 0}`)
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    const userId = localStorage.getItem('userId')
    if (userId) {
      config.headers['X-User-Id'] = userId
    }
    const role = localStorage.getItem('role')
    if (role) {
      config.headers['X-User-Role'] = role
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
  async error => {
    const config = error.config
    
    console.log('响应拦截器错误处理:', {
      code: error.code,
      message: error.message,
      response: error.response ? { status: error.response.status } : null,
      config: config ? { url: config.url, retry: config.retry } : null
    })
    
    if (!config || typeof config !== 'object') {
      ElMessage.error('Network error')
      return Promise.reject(error)
    }
    
    if (!config.retry) {
      config.retry = 0
    }

    const isNetworkError = error.code === 'ECONNABORTED' || 
                          error.code === 'ETIMEDOUT' || 
                          error.code === 'ERR_NETWORK' || 
                          error.code === 'ERR_CANCELED' ||
                          error.message?.includes('aborted') || 
                          error.message?.includes('canceled') || 
                          error.message?.includes('timeout') ||
                          error.message?.includes('Timed out') ||
                          error.message?.includes('terminated') ||
                          !error.response || 
                          !error.request ||
                          error.__CANCEL__

    const isServerError = error.response && error.response.status >= 500 && error.response.status < 600

    console.log('isNetworkError:', isNetworkError)
    console.log('isServerError:', isServerError)

    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      ElMessage.error('Authentication expired, please login again')
      return Promise.reject(error)
    } else if (isNetworkError || isServerError) {
      if (config.retry < MAX_RETRIES) {
        console.log(`重试请求: ${config.url}, 重试次数: ${config.retry + 1}`)
        config.retry++
        await new Promise(resolve => setTimeout(resolve, RETRY_DELAY * config.retry))
        return api(config)
      }
      ElMessage.error('Request timeout after retries')
      return Promise.reject(error)
    } else if (error.response) {
      ElMessage.error(error.response.data?.message || 'Request failed')
      return Promise.reject(error)
    } else {
      ElMessage.error('Network error')
      return Promise.reject(error)
    }
  }
)

export default api
