// main.js 完整代码（仅保留Vue3逻辑）
import { createSSRApp } from 'vue'
import App from './App.vue' // 注意：后缀.vue不能省略（关键！）
import { mountUViewRuntime } from '../utils/uview-runtime'
export function createApp() {
  const app = createSSRApp(App)
  mountUViewRuntime(app)
  return {
    app
  }
}
