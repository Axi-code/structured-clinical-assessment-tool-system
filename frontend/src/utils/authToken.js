/**
 * 内存中保存 accessToken，不落盘，避免 XSS 窃取。
 * 供 request 拦截器读取、store 登录/刷新/登出时写入或清空。
 */

let accessToken = ''
let onRefreshSuccess = null

export function getAccessToken() {
  return accessToken
}

export function setAccessToken(token) {
  accessToken = token || ''
}

export function clearAccessToken() {
  accessToken = ''
}

/** 注册刷新成功回调（由 store 在应用启动时注册），用于同步 token + userInfo */
export function setOnRefreshSuccess(fn) {
  onRefreshSuccess = fn
}

export function notifyRefreshSuccess(data) {
  if (onRefreshSuccess && data) onRefreshSuccess(data)
}
