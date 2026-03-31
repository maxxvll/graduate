import logger from './logger.js'

const env = import.meta.env || {}
const isDevEnv = Boolean(env.DEV || env.MODE === 'development')

const envConfig = {
  API_BASE_URL:
    globalThis.__API_BASE_URL__ ||
    env.VITE_API_BASE_URL ||
    (isDevEnv ? '/api' : 'http://47.99.57.75:5050/api'),
  WS_BASE_URL:
    globalThis.__WS_BASE_URL__ ||
    env.VITE_WS_BASE_URL ||
    (isDevEnv ? '127.0.0.1' : '47.99.57.75'),
  WS_PORT: globalThis.__WS_PORT__ || env.VITE_WS_PORT || '5051',
  NATIVE_WEBRTC_PLUGIN_ID:
    globalThis.__NATIVE_WEBRTC_PLUGIN_ID__ ||
    env.VITE_NATIVE_WEBRTC_PLUGIN_ID ||
    '',
  RTC_PUSH_BASE_URL:
    globalThis.__RTC_PUSH_BASE_URL__ ||
    env.VITE_RTC_PUSH_BASE_URL ||
    '',
  RTC_PLAY_BASE_URL:
    globalThis.__RTC_PLAY_BASE_URL__ ||
    env.VITE_RTC_PLAY_BASE_URL ||
    '',
  REQUEST_TIMEOUT:
    Number(globalThis.__REQUEST_TIMEOUT__ || env.VITE_REQUEST_TIMEOUT) || 10000,
  ENV: env.MODE || 'production',
  IS_DEV: isDevEnv,
}

export const API_CONFIG = {
  BASE_URL: envConfig.API_BASE_URL,
  TIMEOUT: envConfig.REQUEST_TIMEOUT,
}

export const WS_CONFIG = {
  BASE_URL: envConfig.WS_BASE_URL,
  PORT: String(envConfig.WS_PORT),
  PATH: '/ws',
}

export const RTC_CONFIG = {
  NATIVE_WEBRTC_PLUGIN_ID: String(envConfig.NATIVE_WEBRTC_PLUGIN_ID || '').trim(),
  PUSH_BASE_URL: String(envConfig.RTC_PUSH_BASE_URL || '').trim(),
  PLAY_BASE_URL: String(envConfig.RTC_PLAY_BASE_URL || '').trim(),
}

export const APP_CONFIG = {
  APP_NAME: '毕业设计聊天应用',
  VERSION: '1.0.0',
  DEBUG: envConfig.IS_DEV,
  ENV: envConfig.ENV,
  USE_LOCALHOST: envConfig.IS_DEV,
}

const resolveWsProtocol = () => {
  // #ifdef H5
  if (typeof window !== 'undefined' && window.location?.protocol === 'https:') {
    return 'wss'
  }
  // #endif
  return 'ws'
}

export const trimTrailingSlash = (value = '') => String(value || '').replace(/\/+$/, '')

const sanitizeStreamSegment = (value, fallback = 'unknown') => {
  const normalized = String(value || '').trim()
  if (!normalized) {
    return fallback
  }
  return normalized.replace(/[^0-9a-zA-Z_-]/g, '_')
}

export const hasRtcStreamConfig = () =>
  Boolean(RTC_CONFIG.PUSH_BASE_URL && RTC_CONFIG.PLAY_BASE_URL)

export const hasConfiguredNativeWebRtcPlugin = () => Boolean(RTC_CONFIG.NATIVE_WEBRTC_PLUGIN_ID)

export const buildCallStreamKey = ({ sessionId, userId, mode = 'audio' } = {}) => {
  const safeMode = sanitizeStreamSegment(mode === 'video' ? 'video' : 'audio', 'audio')
  const safeSessionId = sanitizeStreamSegment(sessionId, 'session')
  const safeUserId = sanitizeStreamSegment(userId, 'user')
  return `im-call/${safeMode}/${safeSessionId}/${safeUserId}`
}

export const buildRtcPushUrl = (params = {}) => {
  if (!RTC_CONFIG.PUSH_BASE_URL) {
    return ''
  }
  return `${trimTrailingSlash(RTC_CONFIG.PUSH_BASE_URL)}/${buildCallStreamKey(params)}`
}

export const buildRtcPlayUrl = (params = {}) => {
  if (!RTC_CONFIG.PLAY_BASE_URL) {
    return ''
  }
  return `${trimTrailingSlash(RTC_CONFIG.PLAY_BASE_URL)}/${buildCallStreamKey(params)}`
}

export const getWebSocketUrl = (token) => {
  const baseUrl = APP_CONFIG.USE_LOCALHOST ? 'localhost' : WS_CONFIG.BASE_URL
  const encodedToken = encodeURIComponent(token || '')
  return `${resolveWsProtocol()}://${baseUrl}:${WS_CONFIG.PORT}${WS_CONFIG.PATH}?token=${encodedToken}`
}

export const log = {
  debug: (tag, message, data = null) => {
    if (APP_CONFIG.DEBUG) {
      logger.debug(tag, message, data)
    }
  },
  info: (tag, message, data = null) => {
    if (APP_CONFIG.DEBUG) {
      logger.info(tag, message, data)
    }
  },
  warn: (tag, message, data = null) => {
    logger.warn(tag, message, data)
  },
  error: (tag, message, error = null) => {
    logger.error(tag, message, error)
  },
}

export default {
  API_CONFIG,
  WS_CONFIG,
  RTC_CONFIG,
  APP_CONFIG,
  envConfig,
  getWebSocketUrl,
  hasConfiguredNativeWebRtcPlugin,
  hasRtcStreamConfig,
  buildCallStreamKey,
  buildRtcPushUrl,
  buildRtcPlayUrl,
  log,
}
