const TOKEN_KEY = 'api_token'
const USER_KEY = 'api_user'

/** 获取 Token */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/** 设置 Token */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 移除 Token */
export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/** 获取用户信息 */
export function getUser(): any {
  const user = localStorage.getItem(USER_KEY)
  return user ? JSON.parse(user) : null
}

/** 设置用户信息 */
export function setUser(user: any): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

/** 移除用户信息 */
export function removeUser(): void {
  localStorage.removeItem(USER_KEY)
}

/** 清除所有登录信息 */
export function clearAuth(): void {
  removeToken()
  removeUser()
}

