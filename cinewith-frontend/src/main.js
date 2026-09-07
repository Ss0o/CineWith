import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { router } from './router'
import { initializeAuth } from './composables/useAuth'

await initializeAuth()
createApp(App).use(router).mount('#app')
