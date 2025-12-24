import request from './request'

/** 可用接口VO */
export interface AvailableApiVO {
  id: number
  apiName: string
  apiPath: string
  method: string
  apiDesc: string
  status: number
  createTime: string
  updateTime: string
  hasPermission: boolean
  expireTime: string | null
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 查询所有可用接口（带开通状态）
 * 需要Token认证
 */
export async function getAvailableApis(params: {
  pageNum?: number
  pageSize?: number
  apiName?: string
}) {
  return request.get<PageResult<AvailableApiVO>>('/user/availableApis', { params })
}

/**
 * 用户自主申请开通接口权限（自动开通）
 * 需要Token认证
 */
export async function applyApiPermission(apiId: number, expireTime?: string) {
  const formData = new URLSearchParams()
  formData.append('apiId', String(apiId))
  if (expireTime) {
    formData.append('expireTime', expireTime)
  }
  return request.post('/user/applyApiPermission', formData, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

