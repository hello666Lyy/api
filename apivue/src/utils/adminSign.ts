// 管理后台 AK/SK 签名工具（前端版，与后端 SignUtil 保持一致）
// 签名规则：
// 1. 参与签名的参数：accessKey、timestamp、nonce
// 2. 按 ASCII 升序拼接为：key=value&key=value
// 3. 使用 HMAC-SHA256，秘钥为 SK，结果转为小写 16 进制字符串

/** 从 localStorage 读取管理员 AK/SK（你可以根据需要改成别的存储方式） */
export function getAdminAkSk() {
  const accessKey = localStorage.getItem('adminAccessKey') || ''
  const secretKey = localStorage.getItem('adminSecretKey') || ''
  return { accessKey, secretKey }
}

/** 生成随机 nonce（16 位字母数字） */
export function generateNonce(length = 16): string {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let result = ''
  const array = new Uint32Array(length)
  // 使用更安全的随机数
  if (window.crypto && window.crypto.getRandomValues) {
    window.crypto.getRandomValues(array)
    for (let i = 0; i < length; i++) {
      result += chars[array[i] % chars.length]
    }
  } else {
    for (let i = 0; i < length; i++) {
      result += chars[Math.floor(Math.random() * chars.length)]
    }
  }
  return result
}

/** 使用 Web Crypto 计算 HMAC-SHA256 并输出小写 16 进制字符串 */
async function hmacSha256Hex(message: string, secret: string): Promise<string> {
  const encoder = new TextEncoder()
  const keyData = encoder.encode(secret)
  const msgData = encoder.encode(message)

  const cryptoKey = await window.crypto.subtle.importKey(
    'raw',
    keyData,
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  )

  const signature = await window.crypto.subtle.sign('HMAC', cryptoKey, msgData)
  const bytes = new Uint8Array(signature)
  let hex = ''
  for (const b of bytes) {
    hex += b.toString(16).padStart(2, '0')
  }
  return hex.toLowerCase()
}

/** 构造签名字符串（与后端 SignUtil.generateSign 保持一致） */
function buildSignString(params: { [key: string]: string | number }): string {
  // 只有 accessKey / nonce / timestamp 三个参与签名，且按 ASCII 升序
  const keys = Object.keys(params).sort() // ASCII 升序
  const parts: string[] = []
  for (const key of keys) {
    const value = params[key]
    if (value !== undefined && value !== null && value.toString() !== '') {
      parts.push(`${key}=${value}`)
    }
  }
  return parts.join('&')
}

/**
 * 构造带签名的管理端请求参数：
 * - 业务 query/body 参数：businessParams
 * - 自动追加：accessKey, timestamp(秒), nonce, sign
 */
export async function buildSignedAdminParams<T extends Record<string, any>>(
  businessParams: T
): Promise<T & { accessKey: string; timestamp: number; nonce: string; sign: string }> {
  const { accessKey, secretKey } = getAdminAkSk()

  if (!accessKey || !secretKey) {
    throw new Error('未配置管理员 AK/SK，请先在浏览器 localStorage 中设置 adminAccessKey 和 adminSecretKey')
  }

  const timestamp = Math.floor(Date.now() / 1000) // 秒
  const nonce = generateNonce(16)

  const signParams = {
    accessKey,
    timestamp,
    nonce
  }

  const signStr = buildSignString(signParams)
  const sign = await hmacSha256Hex(signStr, secretKey)

  return {
    ...(businessParams as any),
    ...signParams,
    sign
  }
}


