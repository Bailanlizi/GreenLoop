import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './api/axios.config' // 导入以确保拦截器被设置
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/message/style/css'
import './style.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.mount('#app')
