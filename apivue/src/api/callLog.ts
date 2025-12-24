import request from './request'
import { buildSignedAdminParams } from '@/utils/adminSign'

export interface CallLogQuery {
  pageNum?: number
  pageSize?: number
  apiPath?: string
  accessKeyFilter?: string
  status?: number
  startTime?: string
  endTime?: string
}

export interface CallLogRecord {
  id: number
  apiId: number
  apiPath: string
  method: string
  accessKey: string
  status: number
  costTime: number
  callTime: string
  ip?: string
  requestParams?: string
  responseResult?: string
  errorMsg?: string
}

export interface CallLogPage {
  records: CallLogRecord[]
  total: number
  size: number
  current: number
}

/**
 * 管理员分页查询调用日志
 */
export async function getAdminCallLogs(query: CallLogQuery = {}) {
  const signedParams = await buildSignedAdminParams(query)
  return request.get<CallLogPage>('/admin/call-log/page', { params: signedParams })
}

/**
 * 用户分页查询调用日志（需要登录Token）
 */
export async function getUserCallLogs(query: CallLogQuery = {}) {
  return request.get<CallLogPage>('/user/call-log/page', { params: query })
}










