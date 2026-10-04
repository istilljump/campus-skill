import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const TOKEN_KEY = 'campus_admin_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.token = token
  return config
})

function toLogin() {
  clearToken()
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 1) return res.data
    const msg = res.msg || '请求失败'
    if (msg.includes('token')) {
      //token 缺失或过期：静默清理并回登录页，避免登录前弹一串过期提示
      toLogin()
      return Promise.reject(new Error(msg))
    }
    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  },
  (error) => {
    ElMessage.error(error.message === 'Network Error' ? '网络异常，请检查后端服务' : error.message)
    return Promise.reject(error)
  },
)

export default request
