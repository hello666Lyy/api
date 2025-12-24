import request from './request'
import { buildSignedAdminParams } from '@/utils/adminSign'

/** 我的调用统计 */
export interface MyStatisticsVO {
  totalCalls: number
  successCalls: number
  failedCalls: number
  todayCalls: number
  apiCallStats: Array<{
    apiId: number
    apiName: string
    callCount: number
  }>
}

/** 全局调用统计 */
export interface GlobalStatisticsVO {
  totalCalls: number
  totalUsers: number
  totalApis: number
  todayCalls: number
  avgResponseTime?: number
}

/**
 * 获取我的调用统计
 * 需要Token认证
 * 
 * ⚠️ 需要后端实现此接口：
 * GET /api/user/myStatistics
 * 参数（query）：
 * - startTime?: string (可选，开始时间)
 * - endTime?: string (可选，结束时间)
 */
export async function getMyStatistics(startTime?: string, endTime?: string) {
  const params: any = {}
  if (startTime) {
    params.startTime = startTime
  }
  if (endTime) {
    params.endTime = endTime
  }
  return request.get<MyStatisticsVO>('/user/myStatistics', { params })
}

/**
 * 获取全局调用统计（管理员）
 * 需要管理员AK/SK签名
 * 
 * ⚠️ 需要后端实现此接口：
 * GET /api/admin/statistics/global
 * 参数（query）：
 * - accessKey: string (管理员AK，签名参数)
 * - sign: string (签名，签名参数)
 * - timestamp: number (时间戳，签名参数)
 * - nonce: string (随机串，签名参数)
 * - startTime?: string (可选，开始时间)
 * - endTime?: string (可选，结束时间)
 */
export async function getGlobalStatistics(startTime?: string, endTime?: string) {
  const params: any = {}
  if (startTime) {
    params.startTime = startTime
  }
  if (endTime) {
    params.endTime = endTime
  }
  const signedParams = await buildSignedAdminParams(params)
  return request.get<GlobalStatisticsVO>('/admin/statistics/global', { params: signedParams })
}


