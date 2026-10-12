import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('erp_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const result = response.data
    // 后端统一返回 Result 结构，这里直接解包 data
    if (result && typeof result === 'object' && 'code' in result) {
      if (result.code === 200) {
        return result.data
      }
      if (result.code === 401) {
        localStorage.removeItem('erp_token')
        router.push('/login')
        ElMessage.error(result.message || '登录已失效，请重新登录')
        return Promise.reject(new Error(result.message || '登录已失效'))
      }
      ElMessage.error(result.message || '操作失败')
      return Promise.reject(new Error(result.message || '操作失败'))
    }
    return result
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      localStorage.removeItem('erp_token')
      router.push('/login')
      ElMessage.error('登录已失效，请重新登录')
    } else {
      ElMessage.error(error?.response?.data?.message || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request








