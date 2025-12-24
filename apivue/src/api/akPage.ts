import request from './request'
import { buildSignedAdminParams } from '@/utils/adminSign'

export interface SysUserAk {
  id: number
  username: string
  accessKey: string
  status: number
  permissionType: number
  expireTime?: string | null
  updateTime?: string | null
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export async function queryAkPage(params: {
  pageNum?: number
  pageSize?: number
  targetAkLike?: string
  status?: number
  permissionType?: number
}) {
  const signedParams = await buildSignedAdminParams(params)
  return request.post<PageResult<SysUserAk>>('/user/batchQueryAk', null, { params: signedParams })
}


