import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, register as registerApi, getUserInfo as getUserInfoApi, logout as logoutApi, type LoginDTO, type RegisterDTO, type LoginResultVO } from '@/api/auth'
import { setToken, setUser, getToken, getUser, clearAuth } from '@/utils/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string | null>(getToken())
  const userInfo = ref<LoginResultVO | null>(getUser())
  const isLoggedIn = ref<boolean>(!!token.value)

  // 注册
  async function register(registerData: RegisterDTO) {
    const res = await registerApi(registerData)
    const data = res.data
    
    // 保存 Token 和用户信息
    token.value = data.token
    userInfo.value = data
    isLoggedIn.value = true
    
    setToken(data.token)
    setUser(data)
    
    return data
  }

  // 登录
  async function login(loginData: LoginDTO) {
    const res = await loginApi(loginData)
    const data = res.data
    
    // 保存 Token 和用户信息
    token.value = data.token
    userInfo.value = data
    isLoggedIn.value = true
    
    setToken(data.token)
    setUser(data)
    
    return data
  }

  // 获取用户信息
  async function fetchUserInfo() {
    const res = await getUserInfoApi()
    userInfo.value = res.data
    setUser(res.data)
    return res.data
  }

  // 退出登录
  async function logout() {
    try {
      // 调用后端退出登录接口（可选，后端可能只是做token黑名单）
      await logoutApi()
    } catch (error) {
      // 即使后端调用失败，也清除本地状态
      console.error('退出登录接口调用失败:', error)
    } finally {
      // 清除本地状态
      token.value = null
      userInfo.value = null
      isLoggedIn.value = false
      clearAuth()
      router.push('/login')
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    register,
    login,
    fetchUserInfo,
    logout
  }
})

