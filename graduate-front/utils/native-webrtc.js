import { RTC_CONFIG } from './config'
import { isAppPlusRuntime } from './runtime'

let cachedPlugin = null
let pluginResolved = false

const getRootScope = () => {
  if (typeof globalThis !== 'undefined') {
    return globalThis
  }
  if (typeof window !== 'undefined') {
    return window
  }
  return {}
}

const tryResolvePluginMember = (plugin, keys = []) => {
  for (const key of keys) {
    const value = plugin?.[key]
    if (value) {
      return value
    }
  }
  return null
}

export const getNativeWebRtcPlugin = () => {
  if (pluginResolved) {
    return cachedPlugin
  }

  pluginResolved = true

  if (!isAppPlusRuntime() || !RTC_CONFIG.NATIVE_WEBRTC_PLUGIN_ID) {
    cachedPlugin = null
    return cachedPlugin
  }

  try {
    cachedPlugin = uni.requireNativePlugin(RTC_CONFIG.NATIVE_WEBRTC_PLUGIN_ID) || null
  } catch (error) {
    console.warn('[native-webrtc] resolve plugin failed', error)
    cachedPlugin = null
  }

  return cachedPlugin
}

export const hasNativeWebRtcPlugin = () => Boolean(getNativeWebRtcPlugin())

export const getWebRtcPeerConnectionConstructor = () => {
  const scope = getRootScope()
  const plugin = getNativeWebRtcPlugin()
  return (
    scope.RTCPeerConnection ||
    tryResolvePluginMember(plugin, ['RTCPeerConnection', 'WebRTCPeerConnection'])
  )
}

export const getWebRtcSessionDescriptionConstructor = () => {
  const scope = getRootScope()
  const plugin = getNativeWebRtcPlugin()
  return (
    scope.RTCSessionDescription ||
    tryResolvePluginMember(plugin, ['RTCSessionDescription', 'WebRTCSessionDescription'])
  )
}

export const getWebRtcIceCandidateConstructor = () => {
  const scope = getRootScope()
  const plugin = getNativeWebRtcPlugin()
  return (
    scope.RTCIceCandidate ||
    tryResolvePluginMember(plugin, ['RTCIceCandidate', 'WebRTCIceCandidate'])
  )
}

export const getWebRtcMediaDevices = () => {
  const scope = getRootScope()
  const rootNavigator = scope.navigator || null
  const plugin = getNativeWebRtcPlugin()
  return (
    rootNavigator?.mediaDevices ||
    plugin?.mediaDevices ||
    plugin?.navigator?.mediaDevices ||
    null
  )
}

export const canUseUniversalWebRtcRuntime = () =>
  Boolean(getWebRtcPeerConnectionConstructor() && getWebRtcMediaDevices()?.getUserMedia)

export const createRtcSessionDescription = (value) => {
  if (!value) {
    return null
  }

  const payload = typeof value === 'string' ? JSON.parse(value) : value
  const Constructor = getWebRtcSessionDescriptionConstructor()
  if (!Constructor) {
    return payload
  }

  try {
    return payload instanceof Constructor ? payload : new Constructor(payload)
  } catch {
    return payload
  }
}

export const createRtcIceCandidate = (value) => {
  if (!value) {
    return null
  }

  const payload = typeof value === 'string' ? JSON.parse(value) : value
  const Constructor = getWebRtcIceCandidateConstructor()
  if (!Constructor) {
    return payload
  }

  try {
    return payload instanceof Constructor ? payload : new Constructor(payload)
  } catch {
    return payload
  }
}
