import request from './request'
import { buildSignedAdminParams } from '@/utils/adminSign'

/** 接口权限VO */
export interface ApiPermissionVO {
  id: number
  userId: number
  apiId: number
  apiName: string
  apiPath: string
  method: string
  status: number // 0=未授权, 1=已授权
  expireTime: string | null
  createTime: string
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
 * 为用户开通接口权限（管理员）
 * 需要管理员AK/SK签名
 * 
 * ⚠️ 需要后端实现此接口：
 * POST /api/admin/api-permission/grant
 * 参数：
 * - targetAk: string (目标用户的AK)
 * - apiIds: string (接口ID列表，逗号分隔，如 "1,2,3")
 * - expireTime?: string (可选，过期时间，格式：2025-12-31T23:59:59)
 */
export async function grantApiPermission(
  targetAk: string,
  apiIds: number[],
  expireTime?: string
) {
  const params: any = await buildSignedAdminParams({
    targetAk,
    apiIds: apiIds.join(',')
  })
  if (expireTime) {
    params.expireTime = expireTime
  }
  return request.post('/admin/api-permission/grant', null, { params })
}

/**
 * 撤销用户接口权限（管理员）
 * 需要管理员AK/SK签名
 * 
 * ⚠️ 需要后端实现此接口：
 * POST /api/admin/api-permission/revoke
 * 参数：
 * - targetAk: string (目标用户的AK)
 * - apiIds: string (接口ID列表，逗号分隔)
 */
export async function revokeApiPermission(
  targetAk: string,
  apiIds: number[]
) {
  const params = await buildSignedAdminParams({
    targetAk,
    apiIds: apiIds.join(',')
  })
  return request.post('/admin/api-permission/revoke', null, { params })
}

/**
 * 查询用户已开通的接口权限列表（管理员）
 * 需要管理员AK/SK签名
 * 
 * ⚠️ 需要后端实现此接口：
 * GET /api/admin/api-permission/userApis
 * 参数：
 * - targetAk: string (目标用户的AK)
 * - pageNum?: number (可选，页码)
 * - pageSize?: number (可选，每页数量)
 * 
 * 返回：PageResult<ApiPermissionVO>
 */
export async function getUserApiPermissions(
  targetAk: string,
  pageNum?: number,
  pageSize?: number
) {
  const params: any = await buildSignedAdminParams({
    targetAk
  })
  if (pageNum !== undefined) {
    params.pageNum = pageNum
  }
  if (pageSize !== undefined) {
    params.pageSize = pageSize
  }
  return request.get<PageResult<ApiPermissionVO>>('/admin/api-permission/userApis', { params })
}

/**
 * 查询当前用户的接口权限（通过Token）
 * 需要Token认证
 * 
 * ⚠️ 需要后端实现此接口：
 * GET /api/user/myApiPermissions
 * 参数：
 * - pageNum?: number (可选，页码)
 * - pageSize?: number (可选，每页数量)
 * 
 * 返回：PageResult<ApiPermissionVO>
 */
export async function getMyApiPermissions(
  pageNum?: number,
  pageSize?: number
) {
  const params: any = {}
  if (pageNum !== undefined) {
    params.pageNum = pageNum
  }
  if (pageSize !== undefined) {
    params.pageSize = pageSize
  }
  return request.get<PageResult<ApiPermissionVO>>('/user/myApiPermissions', { params })
}


