// src/utils/request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
// 注意：不要在这里预设 Content-Type。
// axios 会根据请求体自动设置（普通对象 -> application/json，FormData -> 交给浏览器生成带 boundary 的 multipart/form-data），
// 手动预设会导致 FormData 上传时丢失 boundary。
const service = axios.create({
  baseURL: '/api', // 替换为你的实际后端接口基础路径
  timeout: 1000000, // 请求超时时间
})

// 1. 请求拦截器（常用于携带 Token）
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token') // 假设 token 存在本地
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 2. 响应拦截器（常用于统一处理错误、过滤数据）
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端存在两种返回格式：
    // 1) TalentResult 包装：{ code: 200, success: true, message, data }   —— /file/page、/upload
    // 2) 裸数据：数组 / 字符串 / 对象                                    —— /chat/** 相关接口
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code !== 200) {
        ElMessage.error(res.message || '请求失败')
        return Promise.reject(new Error(res.message || 'Error'))
      }
      return res.data // 包装格式：取出核心数据
    }
    return res // 裸格式：原样返回
  },
  (error) => {
    let message = '网络异常，请稍后重试'
    if (error.response) {
      switch (error.response.status) {
        case 401:
          message = '未授权，请重新登录'
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求地址出错'
          break
        case 500:
          message = '服务器内部错误'
          break
      }
    }
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default service
