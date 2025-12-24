import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from '@/utils/auth'
import router from '@/router'

// 创建 Axios 实例
const request = axios.create({
  baseURL: '/api',  // 通过 Vite 代理转发到后端
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 如果是 blob 响应（如下载文件），直接返回 blob 数据
    if (response.config.responseType === 'blob') {
      // 检查响应类型，如果是 JSON 格式的 blob（说明是错误响应），需要解析
      const contentType = response.headers['content-type'] || ''
      if (contentType.includes('application/json')) {
        // 错误响应，需要解析 JSON
        return response.data.text().then((text: string) => {
          try {
            const errorData = JSON.parse(text)
            ElMessage.error(errorData.msg || '下载失败')
            return Promise.reject(new Error(errorData.msg || '下载失败'))
          } catch (e) {
            ElMessage.error('下载失败')
            return Promise.reject(new Error('下载失败'))
          }
        })
      }
      // 正常的 blob 响应（如 jar 文件），直接返回
      return response.data
    }
    
    const res = response.data
    
    // 后端返回的 Result 结构：{ code, msg, data }
    if (res.code === 200) {
      return res
    } else {
      // 业务错误
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
  },
  (error) => {
    // HTTP 错误
    if (error.response) {
      const status = error.response.status
      
      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          clearAuth()
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(error.message || '网络错误')
      }
    } else {
      ElMessage.error('网络连接失败')
    }
    
    return Promise.reject(error)
  }
)

export default request

