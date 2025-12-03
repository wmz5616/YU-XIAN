import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/main.css'
import { vScrollReveal } from './directives/vScrollReveal.js'
import { vTilt } from './directives/vTilt.js'

const app = createApp(App)

app.use(router)
app.directive('scroll-reveal', vScrollReveal)
app.directive('tilt', vTilt)
app.mount('#app')