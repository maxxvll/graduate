import route from 'uview-plus/libs/util/route.js'
import colorGradient from 'uview-plus/libs/function/colorGradient.js'
import test from 'uview-plus/libs/function/test.js'
import debounce from 'uview-plus/libs/function/debounce.js'
import throttle from 'uview-plus/libs/function/throttle.js'
import calc from 'uview-plus/libs/function/calc.js'
import digit from 'uview-plus/libs/function/digit.js'
import index, { $parent, deepMerge, rpx2px } from 'uview-plus/libs/function/index.js'
import config from 'uview-plus/libs/config/config.js'
import zIndex from 'uview-plus/libs/config/zIndex.js'
import color from 'uview-plus/libs/config/color.js'
import http from 'uview-plus/libs/function/http.js'
import fontUtil from 'uview-plus/components/u-icon/util.js'
import zhHans from 'uview-plus/libs/i18n/locales/zh-Hans.json'
import zhHant from 'uview-plus/libs/i18n/locales/zh-Hant.json'
import en from 'uview-plus/libs/i18n/locales/en.json'
import es from 'uview-plus/libs/i18n/locales/es.json'
import fr from 'uview-plus/libs/i18n/locales/fr.json'
import de from 'uview-plus/libs/i18n/locales/de.json'
import ko from 'uview-plus/libs/i18n/locales/ko.json'
import ja from 'uview-plus/libs/i18n/locales/ja.json'
import ru from 'uview-plus/libs/i18n/locales/ru.json'

const platform = 'h5'
const mpMixin = {}
const i18n = {
  settings: {
    lang: 'zh-Hans',
    locales: {
      en,
      es,
      fr,
      de,
      ko,
      ja,
      ru,
      'zh-Hant': zhHant,
      'zh-Hans': zhHans,
    },
  },
}

let localeSubscribed = false

function resolveLocale() {
  if (typeof uni === 'undefined' || typeof uni.getLocale !== 'function') {
    return 'zh-Hans'
  }

  const locale = uni.getLocale()
  return i18n.settings.locales[locale] ? locale : 'zh-Hans'
}

function ensureLocaleSync() {
  i18n.settings.lang = resolveLocale()

  if (
    localeSubscribed ||
    typeof uni === 'undefined' ||
    typeof uni.onLocaleChange !== 'function'
  ) {
    return
  }

  uni.onLocaleChange((locale) => {
    const nextLocale = typeof locale === 'string' ? locale : locale?.locale
    i18n.settings.lang = i18n.settings.locales[nextLocale] ? nextLocale : 'zh-Hans'
  })
  localeSubscribed = true
}

function t(value, params = {}) {
  if (!value) {
    return value
  }

  const lang = i18n.settings.locales[i18n.settings.lang]
    ? i18n.settings.lang
    : 'zh-Hans'
  let result = i18n.settings.locales[lang]?.[value] || value

  Object.keys(params).forEach((key) => {
    result = result.replace(new RegExp(`{${key}}`, 'g'), params[key])
  })

  return result
}

const mixin = {
  props: {
    customStyle: {
      type: [Object, String],
      default: () => ({}),
    },
    customClass: {
      type: String,
      default: '',
    },
    url: {
      type: String,
      default: '',
    },
    linkType: {
      type: String,
      default: 'navigateTo',
    },
  },
  onLoad() {
    if (this.$u) {
      this.$u.getRect = this.$uGetRect
    }
  },
  created() {
    if (this.$u) {
      this.$u.getRect = this.$uGetRect
    }
  },
  computed: {
    $u() {
      return deepMerge(uni.$u, {
        props: undefined,
        http: undefined,
        mixin: undefined,
      })
    },
    bem() {
      return function createBem(name, fixed, change) {
        const prefix = `u-${name}--`
        const classes = {}

        if (fixed) {
          fixed.forEach((item) => {
            classes[prefix + this[item]] = true
          })
        }

        if (change) {
          change.forEach((item) => {
            if (this[item]) {
              classes[prefix + item] = this[item]
            } else {
              delete classes[prefix + item]
            }
          })
        }

        return Object.keys(classes)
      }
    },
  },
  methods: {
    openPage(urlKey = 'url') {
      const url = this[urlKey]
      if (url) {
        route({ type: this.linkType, url })
      }
    },
    navTo(url = '', linkType = 'navigateTo') {
      route({ type: linkType, url })
    },
    $uGetRect(selector, all) {
      return new Promise((resolve) => {
        uni.createSelectorQuery()
          .in(this)
          [all ? 'selectAll' : 'select'](selector)
          .boundingClientRect((rect) => {
            if (all && Array.isArray(rect) && rect.length) {
              resolve(rect)
            }

            if (!all && rect) {
              resolve(rect)
            }
          })
          .exec()
      })
    },
    getParentData(parentName = '') {
      if (!this.parent) {
        this.parent = {}
      }

      this.parent = $parent.call(this, parentName)

      if (this.parent?.children) {
        if (this.parent.children.indexOf(this) === -1) {
          this.parent.children.push(this)
        }
      }

      if (this.parent && this.parentData) {
        Object.keys(this.parentData).forEach((key) => {
          this.parentData[key] = this.parent[key]
        })
      }
    },
    preventEvent(event) {
      if (event && typeof event.stopPropagation === 'function') {
        event.stopPropagation()
      }
    },
    noop(event) {
      this.preventEvent(event)
    },
  },
  onReachBottom() {
    uni.$emit('uOnReachBottom')
  },
  beforeUnmount() {
    if (this.parent && test.array(this.parent.children)) {
      const childrenList = this.parent.children
      childrenList.forEach((child, index) => {
        if (child === this) {
          childrenList.splice(index, 1)
        }
      })
    }
  },
}

function normalizeToastOptions(input, options = {}) {
  if (typeof input === 'object' && input !== null) {
    return { ...input }
  }

  return {
    title: String(input ?? ''),
    ...options,
  }
}

function toast(input, options = {}) {
  const config = normalizeToastOptions(input, options)
  const {
    title = '',
    icon = 'none',
    duration = 1800,
    mask = false,
    position,
  } = config

  uni.showToast({
    title,
    icon,
    duration,
    mask,
    ...(position ? { position } : {}),
  })

  return new Promise((resolve) => {
    setTimeout(resolve, duration)
  })
}

function shallowMerge(target, source = {}) {
  if (typeof target !== 'object' || typeof source !== 'object') {
    return false
  }

  Object.keys(source).forEach((key) => {
    const sourceValue = source[key]
    const targetValue = target[key]

    if (sourceValue == null) {
      target[key] = sourceValue
      return
    }

    if (Array.isArray(targetValue) && Array.isArray(sourceValue)) {
      target[key] = targetValue.concat(sourceValue)
      return
    }

    if (
      typeof targetValue === 'object' &&
      targetValue !== null &&
      typeof sourceValue === 'object' &&
      sourceValue !== null
    ) {
      shallowMerge(targetValue, sourceValue)
      return
    }

    target[key] = sourceValue
  })

  return target
}

function setConfig(configs = {}) {
  shallowMerge(config, configs.config || {})
  shallowMerge(color, configs.color || {})
  shallowMerge(zIndex, configs.zIndex || {})
}

function applyUpuiParams() {
  ensureLocaleSync()

  if (typeof uni?.upuiParams !== 'function') {
    return
  }

  const runtimeOptions = uni.upuiParams()

  if (runtimeOptions?.httpIns) {
    runtimeOptions.httpIns(http)
  }

  if (runtimeOptions?.options) {
    setConfig(runtimeOptions.options)
  }
}

const uviewRuntime = {
  route,
  date: index.timeFormat,
  colorGradient: colorGradient.colorGradient,
  hexToRgb: colorGradient.hexToRgb,
  rgbToHex: colorGradient.rgbToHex,
  colorToRgba: colorGradient.colorToRgba,
  test,
  type: ['primary', 'success', 'error', 'warning', 'info'],
  http,
  config,
  zIndex,
  debounce,
  throttle,
  calc,
  digit,
  mixin,
  mpMixin,
  ...index,
  color,
  platform,
  fontUtil,
  i18n,
  t,
  rpx2px,
  toast,
}

export function mountUViewRuntime(app) {
  applyUpuiParams()
  uni.$u = uviewRuntime

  if (app?.config?.globalProperties) {
    app.config.globalProperties.$u = uviewRuntime
  }

  if (typeof app?.mixin === 'function') {
    app.mixin(mixin)
    app.mixin(mpMixin)
  }
}

export { setConfig }

export default uviewRuntime
