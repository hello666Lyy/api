import request from './request'
import { buildSignedAdminParams } from '@/utils/adminSign'

/** API 信息 VO */
export interface ApiInfoVO {
  id: number
  apiName: string
  apiPath: string
  method: string
  apiDesc: string
  status: number
  createTime: string
  updateTime: string
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 查询 API 列表（自动附带管理员 AK/SK 签名参数） */
export async function getApiList(params: {
  pageNum?: number
  pageSize?: number
  apiName?: string
  apiPath?: string
  status?: number
}) {
  const signedParams = await buildSignedAdminParams(params)
  return request.get('/admin/api-info/list', { params: signedParams })
}

/** 添加 API（自动附带签名参数） */
export async function addApi(data: {
  apiName: string
  apiPath: string
  method: string
  apiDesc?: string
  status: number
}) {
  const signedParams = await buildSignedAdminParams(data)
  return request.post('/admin/api-info/add', null, { params: signedParams })
}

/** 更新 API（自动附带签名参数） */
export async function updateApi(
  id: number,
  data: {
    apiName: string
    apiPath: string
    method: string
    apiDesc?: string
    status: number
  }
) {
  const signedParams = await buildSignedAdminParams(data)
  return request.put(`/admin/api-info/update/${id}`, null, { params: signedParams })
}

/** 删除 API（自动附带签名参数） */
export async function deleteApi(id: number) {
  const signedParams = await buildSignedAdminParams({})
  return request.delete(`/admin/api-info/delete/${id}`, { params: signedParams })
}

/** 获取 API 详情（自动附带签名参数） */
export async function getApiDetail(id: number) {
  const signedParams = await buildSignedAdminParams({})
  return request.get(`/admin/api-info/detail/${id}`, { params: signedParams })
}

/** 更新 API 状态（自动附带签名参数） */
export async function updateApiStatus(id: number, status: number) {
  const signedParams = await buildSignedAdminParams({ status })
  return request.put(`/admin/api-info/status/${id}`, null, { params: signedParams })
}


