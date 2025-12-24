import request from './request'
import { buildSignedAdminParams } from '@/utils/adminSign'

// 单个 AK 状态信息
export interface SingleAkStatusVO {
  accessKey: string
  exist: boolean
  status: number | null
  updateTime: string | null
}

// 批量 AK 状态结果
export interface BatchAkStatusVO {
  total: number
  existCount: number
  akStatusList: SingleAkStatusVO[]
}

// 批量查询 AK 状态（管理员用）
export async function batchGetAkStatus(akList: string) {
  const params = await buildSignedAdminParams({ akList })
  return request.post<BatchAkStatusVO>('/user/batchGetAkStatus', null, { params })
}

// 批量启用 / 禁用 AK（operateType: 2=启用, 3=禁用）
export async function batchOperateAkStatus(params: {
  operateType: number
  targetAks: string
  remark?: string
}) {
  const signedParams = await buildSignedAdminParams(params)
  return request.post<number>('/user/batchOperateAkStatus', null, { params: signedParams })
}


