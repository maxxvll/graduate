// main.js 完整代码（仅保留Vue3逻辑）
import { createSSRApp } from 'vue'
import App from './App.vue' // 注意：后缀.vue不能省略（关键！）
import uviewPlus from 'uview-plus'
export function createApp() {
  const app = createSSRApp(App)
  app.use(uviewPlus)
  return {
    app
  }
}