import { isAppPlusRuntime } from './runtime'

export const APP_PERMISSION_SCOPE = {
  CAMERA: 'camera',
  MICROPHONE: 'microphone',
  ALBUM: 'album',
}

const PERMISSION_KEY_MAP = {
  [APP_PERMISSION_SCOPE.CAMERA]: 'cameraAuthorized',
  [APP_PERMISSION_SCOPE.MICROPHONE]: 'microphoneAuthorized',
  [APP_PERMISSION_SCOPE.ALBUM]: 'albumAuthorized',
}

const PERMISSION_LABEL_MAP = {
  [APP_PERMISSION_SCOPE.CAMERA]: '相机',
  [APP_PERMISSION_SCOPE.MICROPHONE]: '麦克风',
  [APP_PERMISSION_SCOPE.ALBUM]: '相册',
}

const BLOCKED_STATUSES = new Set(['denied', 'config error'])

const normalizeScope = (scope) => String(scope || '').trim().toLowerCase()

const uniqueScopes = (scopes = []) => [...new Set(scopes.map(normalizeScope).filter(Boolean))]

const readAppAuthorizeSetting = () => {
  if (!isAppPlusRuntime() || typeof uni.getAppAuthorizeSetting !== 'function') {
    return {}
  }

  try {
    return uni.getAppAuthorizeSetting() || {}
  } catch (error) {
    console.warn('[app-permission] getAppAuthorizeSetting failed', error)
    return {}
  }
}

export const formatAppPermissionLabels = (scopes = []) =>
  uniqueScopes(scopes)
    .map((scope) => PERMISSION_LABEL_MAP[scope] || scope)
    .join('、')

export const getAppPermissionStatus = (scope) => {
  if (!isAppPlusRuntime()) {
    return 'unsupported'
  }

  const normalizedScope = normalizeScope(scope)
  if (!normalizedScope) {
    return 'unknown'
  }

  const settingKey = PERMISSION_KEY_MAP[normalizedScope] || normalizedScope
  const status = readAppAuthorizeSetting()?.[settingKey]
  return typeof status === 'string' && status.trim() ? status.trim().toLowerCase() : 'unknown'
}

export const getAppPermissionStatuses = (scopes = []) =>
  uniqueScopes(scopes).reduce((result, scope) => {
    result[scope] = getAppPermissionStatus(scope)
    return result
  }, {})

const showPermissionModal = ({ title, content, confirmText = '去设置', cancelText = '取消' }) =>
  new Promise((resolve) => {
    uni.showModal({
      title,
      content,
      confirmText,
      cancelText,
      success: (res) => resolve(Boolean(res?.confirm)),
      fail: () => resolve(false),
    })
  })

const openAppAuthorizeSetting = () =>
  new Promise((resolve) => {
    if (typeof uni.openAppAuthorizeSetting !== 'function') {
      resolve(false)
      return
    }

    uni.openAppAuthorizeSetting({
      success: () => resolve(true),
      fail: (error) => {
        console.warn('[app-permission] openAppAuthorizeSetting failed', error)
        resolve(false)
      },
    })
  })

export const ensureAppPermissionAccess = async (
  scopes = [],
  {
    title = '需要系统权限',
    content = '',
    confirmText = '去设置',
    cancelText = '取消',
    autoOpenSettings = true,
  } = {},
) => {
  const normalizedScopes = uniqueScopes(scopes)
  const statuses = getAppPermissionStatuses(normalizedScopes)
  const blockedScopes = normalizedScopes.filter((scope) => BLOCKED_STATUSES.has(statuses[scope]))

  if (!blockedScopes.length) {
    return {
      ok: true,
      openedSettings: false,
      statuses,
      blockedScopes: [],
    }
  }

  const labels = formatAppPermissionLabels(blockedScopes)
  let openedSettings = false

  if (autoOpenSettings && isAppPlusRuntime()) {
    const confirmed = await showPermissionModal({
      title,
      content: content || `请先在系统设置中开启${labels}权限，然后再继续操作。`,
      confirmText,
      cancelText,
    })
    if (confirmed) {
      openedSettings = await openAppAuthorizeSetting()
    }
  }

  const nextStatuses = getAppPermissionStatuses(normalizedScopes)
  const nextBlockedScopes = normalizedScopes.filter((scope) =>
    BLOCKED_STATUSES.has(nextStatuses[scope]),
  )

  return {
    ok: nextBlockedScopes.length === 0,
    openedSettings,
    statuses: nextStatuses,
    blockedScopes: nextBlockedScopes,
  }
}

export const ensureAnyAppPermissionAccess = async (
  scopes = [],
  {
    title = '需要系统权限',
    content = '',
    confirmText = '去设置',
    cancelText = '取消',
    autoOpenSettings = true,
  } = {},
) => {
  const normalizedScopes = uniqueScopes(scopes)
  const statuses = getAppPermissionStatuses(normalizedScopes)
  const blockedScopes = normalizedScopes.filter((scope) => BLOCKED_STATUSES.has(statuses[scope]))
  const availableScopes = normalizedScopes.filter((scope) => !BLOCKED_STATUSES.has(statuses[scope]))

  if (availableScopes.length > 0) {
    return {
      ok: true,
      openedSettings: false,
      statuses,
      blockedScopes,
      availableScopes,
    }
  }

  const labels = formatAppPermissionLabels(blockedScopes)
  let openedSettings = false

  if (autoOpenSettings && isAppPlusRuntime()) {
    const confirmed = await showPermissionModal({
      title,
      content: content || `请至少开启${labels}中的一个权限，然后再继续操作。`,
      confirmText,
      cancelText,
    })
    if (confirmed) {
      openedSettings = await openAppAuthorizeSetting()
    }
  }

  const nextStatuses = getAppPermissionStatuses(normalizedScopes)
  const nextBlockedScopes = normalizedScopes.filter((scope) =>
    BLOCKED_STATUSES.has(nextStatuses[scope]),
  )
  const nextAvailableScopes = normalizedScopes.filter(
    (scope) => !BLOCKED_STATUSES.has(nextStatuses[scope]),
  )

  return {
    ok: nextAvailableScopes.length > 0,
    openedSettings,
    statuses: nextStatuses,
    blockedScopes: nextBlockedScopes,
    availableScopes: nextAvailableScopes,
  }
}
