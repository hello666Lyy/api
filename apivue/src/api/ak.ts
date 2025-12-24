import request from './request'
import { buildSignedAdminParams } from '@/utils/adminSign'

/** AK状态VO */
export interface AkStatusVO {
  accessKey: string
  status: number
  permissionType: number
  expireTime: string | null
  permissionDesc: string
}

/** 生成新AK/SK响应 */
export interface GenerateAkSkResult {
  newAccessKey: string
  newSecretKey: string
  newToken?: string
}

/** AK操作日志VO */
export interface AkOperateLogVO {
  id: number
  targetAk: string
  operateType: number
  operateTypeText: string
  remark: string
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
 * 通过Token+密码生成新AK/SK
 * 需要Token认证
 */
export async function generateNewAkSkByToken(password: string) {
  const formData = new URLSearchParams()
  formData.append('password', password)
  return request.post<GenerateAkSkResult>('/user/generateNewAkSkByToken', formData, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

/**
 * 通过Token获取AK状态
 * 需要Token认证
 */
export async function getAkStatusByToken() {
  return request.get<AkStatusVO>('/user/getAkStatusByToken')
}

/**
 * 通过AK/SK更新AK状态（管理员）
 * 注意：此接口需要AK/SK签名，但前端无法直接调用
 * 建议使用批量操作接口 batchOperateAkStatus
 */
export async function updateAkStatus(
  targetAk: string,
  status: number
) {
  const params = await buildSignedAdminParams({
    targetAk,
    status
  })
  return request.put('/user/updateAkStatus', null, { params })
}

/**
 * 管理AK过期时间（管理员）
 * 需要管理员AK/SK签名
 */
export async function manageAkExpireTime(
  targetAk: string,
  expireTime?: string
) {
  const params: any = await buildSignedAdminParams({
    targetAk
  })
  if (expireTime) {
    params.expireTime = expireTime
  }
  return request.post('/user/manageAkExpireTime', null, { params })
}

/**
 * 批量查询AK（管理员）
 * 需要管理员AK/SK签名
 */
export async function batchQueryAk(params: {
  pageNum?: number
  pageSize?: number
  targetAkLike?: string
  status?: number
  permissionType?: number
}) {
  const signedParams = await buildSignedAdminParams(params)
  return request.post<PageResult<any>>('/user/batchQueryAk', null, { params: signedParams })
}

/**
 * 批量创建AK（管理员）
 * 需要管理员AK/SK签名
 */
export async function batchCreateAk(
  count: number,
  permissionType?: number
): Promise<{ data: Record<string, string> }> {
  const params: any = await buildSignedAdminParams({
    count
  })
  if (permissionType !== undefined) {
    params.permissionType = permissionType
  }
  return request.post<Record<string, string>>('/user/batchCreateAk', null, { params })
}

/**
 * 批量启用/禁用AK（管理员）
 * 需要管理员AK/SK签名
 * operateType: 2=启用, 3=禁用
 */
export async function batchOperateAkStatus(
  operateType: number,
  targetAks: string[],
  remark?: string
) {
  const params: any = await buildSignedAdminParams({
    operateType,
    targetAks: targetAks.join(',')
  })
  if (remark) {
    params.remark = remark
  }
  return request.post('/user/batchOperateAkStatus', null, { params })
}

/**
 * 管理AK权限（管理员）
 * 需要管理员AK/SK签名
 */
export async function manageAkPermission(
  targetAk: string,
  permissionType?: number
) {
  const params: any = await buildSignedAdminParams({
    targetAk
  })
  if (permissionType !== undefined) {
    params.permissionType = permissionType
  }
  return request.post('/user/manageAkPermission', null, { params })
}

/**
 * 校验AK有效性（管理员）
 * 需要管理员AK/SK签名
 */
export async function checkAkValid(checkAccessKey: string) {
  const params = await buildSignedAdminParams({
    checkAccessKey
  })
  return request.post('/user/checkAkValid', null, { params })
}

/**
 * 逻辑删除AK（管理员）
 * 需要管理员AK/SK签名
 */
export async function logicDeleteAk(targetAk: string, remark?: string) {
  const params: any = await buildSignedAdminParams({
    targetAk
  })
  if (remark) {
    params.remark = remark
  }
  return request.post('/user/logicDeleteAk', null, { params })
}

/**
 * 分页查询AK操作日志（管理员）
 * 需要管理员AK/SK签名
 */
export async function queryAkOperateLog(params: {
  pageNum: number
  pageSize: number
  targetAk?: string
  operateType?: number
  startTime?: string
  endTime?: string
}) {
  const signedParams = await buildSignedAdminParams(params)
  return request.post<PageResult<AkOperateLogVO>>('/user/queryAkOperateLog', null, { params: signedParams })
}

