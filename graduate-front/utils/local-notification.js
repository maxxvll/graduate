/**
 * App 端本地推送通知
 * 仅在 APP-PLUS 平台有效
 */

// #ifdef APP-PLUS

export function showLocalNotification({ title, body, sessionId }) {
  try {
    if (typeof plus === 'undefined' || !plus.push) {
      console.warn('[LocalNotification] plus.push not available')
      return
    }

    const payload = JSON.stringify({ sessionId, timestamp: Date.now() })

    plus.push.createMessage(body, payload, {
      title: title || '新消息',
      cover: false,
      sound: 'system',
    })

    console.log('[LocalNotification] Notification created:', title)
  } catch (e) {
    console.error('[LocalNotification] showLocalNotification failed', e)
  }
}

export function onNotificationClick(callback) {
  try {
    if (typeof plus === 'undefined' || !plus.push) {
      console.warn('[LocalNotification] plus.push not available')
      return
    }

    plus.push.addEventListener('click', (msg) => {
      console.log('[LocalNotification] Notification clicked:', msg)
      try {
        const payload = typeof msg.payload === 'string'
          ? JSON.parse(msg.payload)
          : (msg.payload || {})
        callback(payload)
      } catch (e) {
        console.error('[LocalNotification] Parse notification payload failed', e)
        callback({})
      }
    })
  } catch (e) {
    console.error('[LocalNotification] onNotificationClick setup failed', e)
  }
}

export function setAppBadge(count) {
  try {
    if (typeof uni.setAppBadgeNumber === 'function') {
      uni.setAppBadgeNumber(count)
      return
    }
    if (typeof plus !== 'undefined' && plus.runtime && plus.runtime.setBadgeNumber) {
      plus.runtime.setBadgeNumber(count)
    }
  } catch (e) {
    console.warn('[LocalNotification] setAppBadge failed', e)
  }
}

export function clearAppBadge() {
  setAppBadge(0)
}

// #endif
