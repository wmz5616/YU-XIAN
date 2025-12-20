<script setup>
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { RouterView, useRouter, useRoute } from 'vue-router'
import { store } from './store.js'

const router = useRouter()
const route = useRoute()
const cartBtnRef = ref(null)
const isScrolled = ref(false)

const isAdminRoute = computed(() => route.path.startsWith('/admin'))

const handleScroll = () => {
  isScrolled.value = window.scrollY > 20
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

const isTransparent = computed(() => {
  return route.path === '/' && !isScrolled.value
})

watch(() => store.flySignal.id, () => {
  if (store.flySignal.rect) {
    startFlyAnimation(store.flySignal.rect, store.flySignal.img)
  }
})

// 购物车飞入动画
const startFlyAnimation = (startRect, imgUrl) => {
  if (!cartBtnRef.value) return
  const cartRect = cartBtnRef.value.getBoundingClientRect()
  const endX = cartRect.left + cartRect.width / 2
  const endY = cartRect.top + cartRect.height / 2
  const x = endX - startRect.left
  const y = endY - startRect.top

  const flyer = document.createElement('div')
  const inner = document.createElement('img')

  inner.src = imgUrl || '/icons/logo.png'
  inner.className = 'w-10 h-10 rounded-full border border-white/20 shadow-2xl object-cover'
  inner.style.transition = 'transform 0.7s cubic-bezier(0.2, 0.8, 0.2, 1)'
  inner.style.transform = `translateY(${y}px) scale(0.2)`

  flyer.className = 'flyer-box fixed z-[9999] pointer-events-none transition-transform duration-700 ease-linear'
  flyer.style.left = `${startRect.left}px`
  flyer.style.top = `${startRect.top}px`

  flyer.appendChild(inner)
  document.body.appendChild(flyer)

  flyer.offsetHeight
  flyer.style.transform = `translateX(${x}px)`

  setTimeout(() => {
    flyer.remove()
  }, 700)
}

const handleLogout = () => {
  store.logout()
  router.push('/login')
}

const isActive = (path) => route.path === path
</script>

<template>
  <div class="min-h-screen flex flex-col bg-slate-50 font-sans text-slate-800 pb-16 md:pb-0">

    <Transition name="island">
      <div v-if="store.notification.show"
        class="fixed top-8 left-1/2 -translate-x-1/2 z-[9999] flex items-center gap-3 pl-3 pr-6 py-3 bg-[#000000] rounded-full shadow-[0_20px_50px_-10px_rgba(0,0,0,0.4)] border border-white/10 backdrop-blur-2xl ring-1 ring-white/5">

        <div class="relative w-2.5 h-2.5 rounded-full flex items-center justify-center">
          <span class="absolute inline-flex h-full w-full rounded-full opacity-40 animate-ping"
            :class="store.notification.type === 'success' ? 'bg-emerald-400' : 'bg-rose-500'"></span>
          <span class="relative inline-flex rounded-full h-2 w-2"
            :class="store.notification.type === 'success' ? 'bg-emerald-500' : 'bg-rose-500'"></span>
        </div>

        <span class="text-[13px] font-medium text-white tracking-wider font-mono antialiased">
          {{ store.notification.message }}
        </span>
      </div>
    </Transition>

    <header v-if="!isAdminRoute" class="fixed top-0 left-0 right-0 z-50 transition-all duration-500 ease-in-out" :class="[
      isTransparent
        ? 'bg-transparent py-6'
        : 'bg-white/80 backdrop-blur-xl border-b border-slate-200/50 shadow-sm py-3'
    ]">
      <div class="container mx-auto px-6 flex items-center justify-between">

        <div class="flex items-center gap-3 cursor-pointer group" @click="$router.push('/')">
          <img src="/icons/logo.png" alt="Logo"
            class="w-10 h-10 object-contain drop-shadow-md transition-transform duration-300 group-hover:scale-105" />
          <div class="flex flex-col">
            <span class="text-lg font-black tracking-wider leading-none transition-colors duration-300"
              :class="isTransparent ? 'text-white drop-shadow-md' : 'text-slate-800'">
              渔鲜直供
            </span>
            <span class="text-[10px] uppercase tracking-[0.2em] transition-colors duration-300"
              :class="isTransparent ? 'text-white/70' : 'text-slate-400'">
              Direct Supply
            </span>
          </div>
        </div>

        <nav class="hidden md:flex items-center gap-8">
          <RouterLink to="/" class="relative text-sm font-bold transition-all duration-300 group py-2" :class="[
            isActive('/')
              ? (isTransparent ? 'text-white' : 'text-slate-900')
              : (isTransparent ? 'text-white/80 hover:text-white' : 'text-slate-500 hover:text-slate-900')
          ]">
            首页
            <span
              class="absolute bottom-0 left-1/2 -translate-x-1/2 w-0 h-0.5 transition-all duration-300 group-hover:w-full"
              :class="isTransparent ? 'bg-white' : 'bg-slate-900'"></span>
          </RouterLink>

          <div v-if="!store.currentUser" class="flex gap-4 items-center">
            <RouterLink to="/login" class="text-sm font-medium transition-colors hover:scale-105"
              :class="isTransparent ? 'text-white hover:text-white' : 'text-slate-600 hover:text-slate-900'">
              登录
            </RouterLink>
            <RouterLink to="/register"
              class="px-5 py-2 rounded-full text-xs font-bold transition-all shadow-lg hover:-translate-y-0.5 active:translate-y-0"
              :class="isTransparent ? 'bg-white text-slate-900 hover:bg-slate-100' : 'bg-slate-900 text-white hover:bg-slate-800'">
              注册会员
            </RouterLink>
          </div>

          <div v-else class="flex items-center gap-4">
            <RouterLink v-if="store.currentUser.role === 'ADMIN'" to="/admin"
              class="text-xs font-bold px-3 py-1 rounded-full border transition-all flex items-center gap-1"
              :class="isTransparent ? 'bg-white/20 text-white border-white/30 hover:bg-white/30' : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100'">
              进入后台
            </RouterLink>

            <div class="flex items-center gap-3 cursor-pointer group relative">
              <div class="text-right hidden lg:block">
                <div class="text-xs font-bold transition-colors"
                  :class="isTransparent ? 'text-white' : 'text-slate-800'">{{ store.currentUser.displayName }}</div>
                <div class="text-[10px] uppercase transition-colors"
                  :class="isTransparent ? 'text-white/60' : 'text-slate-400'">Welcome Back</div>
              </div>

              <RouterLink to="/profile" class="relative">
                <div class="w-9 h-9 rounded-full overflow-hidden border transition-all"
                  :class="isTransparent ? 'border-white/50' : 'border-slate-200 group-hover:border-slate-400'">
                  <img v-if="store.currentUser.avatar" :src="store.currentUser.avatar"
                    class="w-full h-full object-cover" />
                  <div v-else
                    class="w-full h-full flex items-center justify-center bg-slate-100 text-slate-500 font-bold text-xs">
                    {{ store.currentUser.displayName.charAt(0) }}
                  </div>
                </div>
              </RouterLink>

              <button @click="handleLogout"
                class="text-xs ml-2 opacity-0 -translate-x-2 group-hover:opacity-100 group-hover:translate-x-0 transition-all duration-300"
                :class="isTransparent ? 'text-white hover:text-red-300' : 'text-slate-400 hover:text-red-500'">
                退出
              </button>
            </div>
          </div>
        </nav>

        <button ref="cartBtnRef" @click="$router.push('/cart')"
          class="hidden md:flex items-center gap-2 px-6 py-2.5 rounded-full transition-all duration-300 shadow-xl hover:-translate-y-0.5 active:translate-y-0 group"
          :class="[
            isTransparent
              ? 'bg-white text-slate-900 hover:bg-slate-100'
              : 'bg-slate-900 text-white hover:bg-slate-800 shadow-slate-900/20'
          ]">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
          <span class="text-sm font-bold">购物车</span>
          <span v-if="store.cartCount > 0"
            class="flex items-center justify-center w-5 h-5 rounded-full text-[10px] font-bold ml-1 transition-colors"
            :class="isTransparent ? 'bg-slate-900 text-white' : 'bg-white text-slate-900'">
            {{ store.cartCount }}
          </span>
        </button>

        <button @click="$router.push('/cart')" class="md:hidden relative p-2"
          :class="isTransparent ? 'text-white' : 'text-slate-600'">
          <img src="/icons/icon-cart.png" class="w-6 h-6" :class="{ 'brightness-0 invert': isTransparent }" />
          <span v-if="store.cartCount > 0"
            class="absolute top-0 right-0 bg-red-500 text-white text-[10px] w-4 h-4 rounded-full flex items-center justify-center">{{
              store.cartCount }}</span>
        </button>
      </div>
    </header>

    <div class="flex-1" :class="{ 'pt-[80px]': !isAdminRoute && route.path !== '/' }">
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </div>

    <footer v-if="!isAdminRoute"
      class="bg-[#0B1120] text-slate-400 py-16 mt-0 border-t border-slate-800/50 hidden md:block">
      <div class="container mx-auto px-6">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-12 text-sm">
          <div class="col-span-1 md:col-span-1">
            <div class="flex items-center gap-3 mb-6 opacity-90">
              <div
                class="w-10 h-10 bg-white rounded-lg flex items-center justify-center p-1 shadow-lg shadow-blue-900/20">
                <img src="/icons/logo.png" class="w-full h-full object-contain" />
              </div>
              <span class="text-xl font-bold text-white tracking-widest font-serif-sc">渔鲜直供</span>
            </div>
            <p class="leading-relaxed opacity-60 mb-8 font-light text-xs">
              致力于为中国家庭提供高品质的深海食材。<br>我们就做大自然的搬运工。
            </p>
          </div>
          <div class="pl-0 md:pl-8">
            <h4 class="text-white font-bold mb-6 tracking-widest text-xs uppercase opacity-80">探索</h4>
            <ul class="space-y-4 font-light text-xs text-slate-400">
              <li><a href="#" class="hover:text-white hover:translate-x-1 transition-all inline-block">当季热销</a></li>
              <li><a href="#" class="hover:text-white hover:translate-x-1 transition-all inline-block">主厨推荐</a></li>
            </ul>
          </div>
          <div>
            <h4 class="text-white font-bold mb-6 tracking-widest text-xs uppercase opacity-80">支持</h4>
            <ul class="space-y-4 font-light text-xs text-slate-400">
              <li><a href="#" class="hover:text-white hover:translate-x-1 transition-all inline-block">物流追踪</a></li>
              <li><a href="#" class="hover:text-white hover:translate-x-1 transition-all inline-block">帮助中心</a></li>
            </ul>
          </div>
          <div>
            <h4 class="text-white font-bold mb-6 tracking-widest text-xs uppercase opacity-80">联系方式</h4>
            <div class="text-3xl font-serif-sc font-bold text-white mb-2">123-456-7890</div>
            <p class="text-[10px] opacity-50 mb-6 tracking-wide">周一至周日 09:00 - 22:00</p>
          </div>
        </div>
        <div
          class="border-t border-slate-800/60 mt-16 pt-8 text-center text-[10px] opacity-40 uppercase tracking-widest">
          <p>&copy; 2025 YU XIAN Seafood Supply Chain. All rights reserved.</p>
        </div>
      </div>
    </footer>

    <nav v-if="!isAdminRoute"
      class="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-slate-200 h-16 flex justify-around items-center z-50 pb-safe shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)]">
      <RouterLink to="/" class="flex flex-col items-center gap-1 w-16"
        :class="isActive('/') ? 'text-slate-900' : 'text-slate-400'">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>
        <span class="text-[10px] font-bold">首页</span>
      </RouterLink>
      <RouterLink to="/cart" class="flex flex-col items-center gap-1 w-16 relative"
        :class="isActive('/cart') ? 'text-slate-900' : 'text-slate-400'">
        <div class="relative">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
          <span v-if="store.cartCount > 0"
            class="absolute -top-1 -right-2 bg-red-500 text-white text-[10px] min-w-[16px] h-4 rounded-full flex items-center justify-center px-1">{{
              store.cartCount }}</span>
        </div>
        <span class="text-[10px] font-bold">购物车</span>
      </RouterLink>
      <RouterLink to="/profile" class="flex flex-col items-center gap-1 w-16"
        :class="isActive('/profile') ? 'text-slate-900' : 'text-slate-400'">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
        <span class="text-[10px] font-bold">我的</span>
      </RouterLink>
    </nav>
  </div>
</template>

<style>
/* 灵动岛弹力动画 */
.island-enter-active {
  animation: island-in 0.5s cubic-bezier(0.23, 1, 0.32, 1);
}

.island-leave-active {
  animation: island-out 0.4s cubic-bezier(0.755, 0.05, 0.855, 0.06);
}

@keyframes island-in {
  0% {
    transform: translate(-50%, -100%) scale(0.5);
    opacity: 0;
  }

  60% {
    transform: translate(-50%, 10px) scale(1.05);
    opacity: 1;
  }

  100% {
    transform: translate(-50%, 0) scale(1);
    opacity: 1;
  }
}

@keyframes island-out {
  0% {
    transform: translate(-50%, 0) scale(1);
    opacity: 1;
  }

  100% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0;
  }
}

/* Page Transition */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.page-enter-from,
.page-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

/* =========================================
   SweetAlert2: Museum Grade White (白瓷质感)
   ========================================= */

/* 1. 容器：增加纹理和多重阴影 */
div:where(.swal2-container) div:where(.swal2-popup) {
  /* 噪点纹理背景，模拟纸张/陶瓷质感 */
  background-color: #ffffff !important;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opacity='0.03'/%3E%3C/svg%3E") !important;

  border-radius: 32px !important;
  padding: 3rem 2.5rem !important;
  color: #1e293b !important;

  /* 三层物理级阴影：环境光 + 扩散光 + 核心影 */
  box-shadow:
    0 20px 60px -10px rgba(0, 0, 0, 0.15),
    0 10px 20px -5px rgba(0, 0, 0, 0.04),
    inset 0 0 0 1px rgba(255, 255, 255, 1) !important;

  border: 1px solid rgba(0, 0, 0, 0.03) !important;
}

/* 2. 标题：衬线字体，增加字间距 */
div:where(.swal2-container) .swal2-title {
  color: #0f172a !important;
  font-family: "Georgia", "Songti SC", serif !important;
  /* 强制衬线 */
  font-weight: 900 !important;
  letter-spacing: -0.02em !important;
  font-size: 1.75rem !important;
  margin-bottom: 0.8rem !important;
}

/* 3. 内容：更好的行高 */
div:where(.swal2-container) .swal2-html-container {
  color: #64748b !important;
  font-size: 1rem !important;
  line-height: 1.8 !important;
  font-weight: 400 !important;
}

/* 4. 图标：极简线条，去色 */
div:where(.swal2-icon) {
  border-width: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  transform: scale(0.9);
  margin-bottom: 2rem !important;
}

/* 成功 */
div:where(.swal2-icon).swal2-success {
  color: #059669 !important;
  /* Emerald-600 */
}

div:where(.swal2-icon).swal2-success [class^=swal2-success-line] {
  background-color: #059669 !important;
}

div:where(.swal2-icon).swal2-success .swal2-success-ring {
  background-color: rgba(5, 150, 105, 0.05);
  /* 极淡的背景 */
}

/* 错误 */
div:where(.swal2-icon).swal2-error {
  color: #e11d48 !important;
  /* Rose-600 */
}

div:where(.swal2-icon).swal2-error [class^=swal2-x-mark-line] {
  background-color: #e11d48 !important;
}

div:where(.swal2-icon).swal2-error {
  background-color: rgba(225, 29, 72, 0.05) !important;
}

/* 5. 按钮：黑底白字，流光质感 */
div:where(.swal2-container) .swal2-actions {
  gap: 1rem;
  margin-top: 3rem !important;
  width: 100%;
}

div:where(.swal2-container) button.swal2-styled {
  border-radius: 9999px !important;
  font-weight: 700 !important;
  font-size: 0.95rem !important;
  padding: 16px 32px !important;
  letter-spacing: 0.05em;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1) !important;
  box-shadow: none !important;
  flex: 1;
  max-width: 180px;
}

/* 确认按钮：深黑 + 微妙流光 */
div:where(.swal2-container) button.swal2-confirm {
  background: #0f172a !important;
  /* Slate-900 */
  color: #ffffff !important;
  border: 1px solid #0f172a !important;
  position: relative;
  overflow: hidden;
}

/* 确认按钮：悬浮时的微妙光泽 */
div:where(.swal2-container) button.swal2-confirm::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
  transition: 0.5s;
}

div:where(.swal2-container) button.swal2-confirm:hover::after {
  left: 100%;
}

div:where(.swal2-container) button.swal2-confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.3) !important;
}

/* 取消按钮：极简灰 */
div:where(.swal2-container) button.swal2-cancel {
  background: transparent !important;
  color: #94a3b8 !important;
  /* Slate-400 */
  border: 1px solid #e2e8f0 !important;
  /* Slate-200 */
}

div:where(.swal2-container) button.swal2-cancel:hover {
  color: #0f172a !important;
  border-color: #cbd5e1 !important;
  background: #f8fafc !important;
}

/* 6. 遮罩：轻盈的冷调灰 */
div:where(.swal2-container).swal2-backdrop-show {
  background: rgba(241, 245, 249, 0.7) !important;
  /* Slate-100 / 70% */
  backdrop-filter: blur(12px) !important;
  /* 深度模糊 */
}

.amap-logo,
.amap-copyright,
.amap-geolocation-con {
  display: none !important;
  visibility: hidden !important;
}

/* 隐藏可能注入的 iframe */
iframe[id^="amap"] {
  display: none !important;
  width: 0 !important;
  height: 0 !important;
  opacity: 0 !important;
  pointer-events: none !important;
}
</style>