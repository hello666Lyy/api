import request from './request'

/** SDK版本信息 */
export interface SdkVersionInfo {
  version: string
  groupId: string
  artifactId: string
  description?: string
}

/**
 * 下载SDK jar包
 */
export function downloadSdk() {
  return request({
    url: '/sdk/download',  // baseURL已经包含/api，这里不需要再加/api
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 获取SDK版本信息
 */
export function getSdkVersion(): Promise<{ data: SdkVersionInfo }> {
  return request({
    url: '/sdk/version',  // baseURL已经包含/api，这里不需要再加/api
    method: 'get'
  })
}

