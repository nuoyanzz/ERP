import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { perms as permsDirective } from '@/directives/perms'
import './permission'
import 'element-plus/dist/index.css'
import './styles/index.css'
import './styles/element-plus.css'

const app = createApp()

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.directive('perms', permsDirective)

app.config.errorHandler = (err, _vm, info) => {
  console.error('[Vue Error]', info, err)
  ElementPlus.ElMessage.error('页面渲染出错：' + (err instanceof Error ? err.message : String(err)))
}

router.onError((error) => {
  console.error('[Router Error]', error)
  if (String(error.message || error).includes('Failed to fetch dynamically imported module')) {
    window.location.reload()
    return
  }
  ElementPlus.ElMessage.error('页面加载失败：' + error.message)
})

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')








