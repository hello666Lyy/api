import request from './request'

/** 登录请求参数 */
export interface LoginDTO {
  username: string
  password: string
}

/** 注册请求参数 */
export interface RegisterDTO {
  username: string
  password: string
}

/** 登录响应结果 */
export interface LoginResultVO {
  token: string
  userId: number
  username: string
  accessKey: string
  secretKey?: string  // 注册时返回，仅此一次
  permissionType: number
  expireTime: number
}

/** 统一响应结构 */
export interface Result<T> {
  code: number
  msg: string
  data: T
}

/** 用户注册 */
export function register(data: RegisterDTO): Promise<Result<LoginResultVO>> {
  return request.post('/auth/register', data)
}

/** 用户登录 */
export function login(data: LoginDTO): Promise<Result<LoginResultVO>> {
  return request.post('/auth/login', data)
}

/** 获取当前用户信息 */
export function getUserInfo(): Promise<Result<LoginResultVO>> {
  return request.get('/auth/userInfo')
}

/** 退出登录 */
export function logout(): Promise<Result<void>> {
  return request.post('/auth/logout')
}

/** 修改密码 */
export function changePassword(oldPassword: string, newPassword: string): Promise<Result<void>> {
  const formData = new URLSearchParams()
  formData.append('oldPassword', oldPassword)
  formData.append('newPassword', newPassword)
  return request.post('/auth/changePassword', formData, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

