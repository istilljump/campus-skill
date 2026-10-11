import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const USER_TOKEN_KEY = 'user_token'
const RUNNER_TOKEN_KEY = 'runner_token'

export function getUserToken() {
  return localStorage.getItem(USER_TOKEN_KEY) || ''
}

export function setUserToken(t) {
  localStorage.setItem(USER_TOKEN_KEY, t)
}

export function getRunnerToken() {
  return localStorage.getItem(RUNNER_TOKEN_KEY) || ''
}

export function setRunnerToken(t) {
  localStorage.setItem(RUNNER_TOKEN_KEY, t)
}

export function clearAllTokens() {
  localStorage.removeItem(USER_TOKEN_KEY)
  localStorage.removeItem(RUNNER_TOKEN_KEY)
  localStorage.removeItem('campus_admin_name')
}

// 统一数据交互：单实例 axios，按路径前缀自动携带 user/skiller 侧 token
// （/user/** 与 /skiller/** 均要求 authentication 头；token 过期统一回登录页）
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const url = config.url || ''
  // /skiller/** 用技能者 token；其余默认用户 token。
  // 后端 JWT 同签发密钥且拦截器只验签，技能者 token 可调 /user/** 只读接口
  // （与 H5 技能者端行为一致），因此任一侧 token 缺失时回退到另一侧，
  // 避免纯技能者会话调分类等只读接口时被"token不合法"踢回登录页。
  const token = url.startsWith('/skiller')
    ? (getRunnerToken() || getUserToken())
    : (getUserToken() || getRunnerToken())
  if (token) config.headers.authentication = token
  return config
})

function toLogin() {
  clearAllTokens()
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
