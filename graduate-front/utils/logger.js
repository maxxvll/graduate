class Logger {
  constructor() {
    this.levels = {
      debug: 0,
      info: 1,
      warn: 2,
      error: 3,
      none: 4,
    }
    this.logLevel = process.env.NODE_ENV === 'production' ? 'error' : 'debug'
    this.enableStorage = true
    this.maxStorageSize = 100
    this.logs = []
    this.saveTimer = null
    this.loadLogsFromStorage()
  }

  loadLogsFromStorage() {
    try {
      const storedLogs = uni.getStorageSync('app_logs')
      if (storedLogs) {
        this.logs = JSON.parse(storedLogs)
      }
    } catch (error) {
      console.error('[Logger] Failed to load logs', error)
    }
  }

  saveLogsToStorage() {
    if (!this.enableStorage) {
      return
    }
    try {
      uni.setStorageSync(
        'app_logs',
        JSON.stringify(this.logs.slice(-this.maxStorageSize)),
      )
    } catch (error) {
      console.error('[Logger] Failed to save logs', error)
    }
  }

  shouldLog(level) {
    return this.levels[level] >= this.levels[this.logLevel]
  }

  addLog(level, tag, message, data = null) {
    this.logs.push({
      timestamp: new Date().toISOString(),
      level,
      tag,
      message,
      data,
    })

    if (this.logs.length > this.maxStorageSize * 2) {
      this.logs = this.logs.slice(-this.maxStorageSize)
    }

    if (this.enableStorage) {
      if (this.saveTimer) {
        clearTimeout(this.saveTimer)
      }
      this.saveTimer = setTimeout(() => {
        this.saveLogsToStorage()
      }, 500)
    }
  }

  format(level, tag, message, data) {
    const prefix = `[${new Date().toLocaleTimeString()}] [${level.toUpperCase()}] [${tag}]`
    return data == null ? `${prefix} ${message}` : `${prefix} ${message}`
  }

  debug(tag, message, data = null) {
    if (!this.shouldLog('debug')) {
      return
    }
    console.log(this.format('debug', tag, message, data), data ?? '')
    this.addLog('debug', tag, message, data)
  }

  info(tag, message, data = null) {
    if (!this.shouldLog('info')) {
      return
    }
    console.log(this.format('info', tag, message, data), data ?? '')
    this.addLog('info', tag, message, data)
  }

  warn(tag, message, data = null) {
    if (!this.shouldLog('warn')) {
      return
    }
    console.warn(this.format('warn', tag, message, data), data ?? '')
    this.addLog('warn', tag, message, data)
  }

  error(tag, message, error = null) {
    if (!this.shouldLog('error')) {
      return
    }
    const errorData =
      error instanceof Error
        ? {
            name: error.name,
            message: error.message,
            stack: error.stack,
          }
        : error
    console.error(this.format('error', tag, message, errorData), errorData ?? '')
    this.addLog('error', tag, message, errorData)
  }

  getLogs() {
    return [...this.logs]
  }

  clearLogs() {
    this.logs = []
    try {
      uni.removeStorageSync('app_logs')
    } catch (error) {
      console.error('[Logger] Failed to clear logs', error)
    }
  }

  setLogLevel(level) {
    if (this.levels[level] !== undefined) {
      this.logLevel = level
    }
  }

  setEnableStorage(enable) {
    this.enableStorage = Boolean(enable)
    if (!this.enableStorage) {
      this.clearLogs()
    }
  }
}

const logger = new Logger()

export const debug = (tag, message, data) => logger.debug(tag, message, data)
export const info = (tag, message, data) => logger.info(tag, message, data)
export const warn = (tag, message, data) => logger.warn(tag, message, data)
export const error = (tag, message, err) => logger.error(tag, message, err)

export default logger
export { Logger }
