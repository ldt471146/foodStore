import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { useCartStore } from './stores/cart'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

const authStore = useAuthStore(pinia)
authStore.restore()

const cartStore = useCartStore(pinia)
if (authStore.isAuthenticated) {
  void cartStore.loadCart()
}

app.mount('#app')
