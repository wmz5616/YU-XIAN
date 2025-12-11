<script setup>
import { ref, watch, nextTick } from 'vue'
import { RouterView, useRouter, useRoute } from 'vue-router'
import { store } from './store.js'

const router = useRouter()
const route = useRoute()
const cartBtnRef = ref(null)

watch(() => store.flySignal.id, () => {
  if (store.flySignal.rect) {
    startFlyAnimation(store.flySignal.rect, store.flySignal.img)
  }
})

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
  inner.className = 'w-10 h-10 rounded-full border-2 border-blue-500 shadow-lg object-cover transition-transform duration-700'
  inner.style.transitionTimingFunction = 'cubic-bezier(0.5, -0.5, 1, 1)'
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

    <div v-if="store.notification.show"
      class="fixed top-24 right-6 z-[100] bg-white/80 backdrop-blur-md border border-white/40 shadow-2xl rounded-2xl px-6 py-4 flex items-center gap-3 animate-slide-in transform transition-all hover:scale-105"
      :class="store.notification.type === 'success' ? 'text-green-600' : 'text-red-600'">
      <img v-if="store.notification.type === 'success'" src="/icons/icon-success.png" class="w-6 h-6" />
      <img v-else src="/icons/icon-error.png" class="w-6 h-6" />
      <span class="font-bold text-slate-800 tracking-wide">{{ store.notification.message }}</span>
    </div>

    <header
      class="sticky top-0 z-50 bg-white/80 backdrop-blur-xl border-b border-slate-200/50 shadow-sm transition-all duration-300 supports-[backdrop-filter]:bg-white/60">
      <div class="container mx-auto px-4 h-16 flex items-center justify-between">
        <div class="flex items-center gap-2 cursor-pointer" @click="$router.push('/')">
          <img src="/icons/logo.png" class="w-8 h-8 object-contain" alt="Logo" />
          <span class="text-xl font-bold tracking-tight text-blue-950">渔鲜直供</span>
        </div>

        <nav class="hidden md:flex gap-6 text-sm font-medium text-slate-600 items-center">
          <RouterLink to="/" class="hover:text-blue-700 transition" exact-active-class="text-blue-900 font-bold">首页
          </RouterLink>

          <div class="h-4 w-px bg-slate-200"></div>

          <div v-if="!store.currentUser" class="flex gap-4">
            <RouterLink to="/login" class="hover:text-blue-900">登录</RouterLink>
            <RouterLink to="/register"
              class="bg-slate-900 text-white px-3 py-1 rounded text-xs hover:bg-slate-700 transition">注册会员</RouterLink>
          </div>

          <div v-else class="flex items-center gap-3 pl-4 border-l border-slate-200">
            <RouterLink to="/profile" class="flex items-center gap-2 group" exact-active-class="text-blue-900">
              <div class="w-8 h-8 rounded-full bg-blue-100 overflow-hidden border border-blue-200">
                <img v-if="store.currentUser.avatar" :src="store.currentUser.avatar"
                  class="w-full h-full object-cover" />
                <div v-else class="w-full h-full flex items-center justify-center text-blue-800 text-xs font-bold">
                  {{ store.currentUser.displayName.charAt(0) }}
                </div>
              </div>
              <span class="text-slate-700 font-bold group-hover:text-blue-900 transition">
                {{ store.currentUser.displayName }}
              </span>
            </RouterLink>
            <button @click="handleLogout" class="text-slate-400 hover:text-red-500 text-xs ml-2">
              退出
            </button>
          </div>
        </nav>

        <button ref="cartBtnRef" @click="$router.push('/cart')"
          class="hidden md:flex items-center gap-2 px-5 py-2 bg-blue-600 text-white text-sm rounded-full hover:bg-blue-500 transition shadow-lg shadow-blue-600/20 active:scale-95">
          <img src="/icons/icon-cart.png" class="w-4 h-4 brightness-200" alt="Cart" />
          <span>购物车 ({{ store.cartCount }})</span>
        </button>

        <button @click="$router.push('/cart')" class="md:hidden relative p-2 text-slate-600">
          <img src="/icons/icon-cart.png" class="w-6 h-6 opacity-80" />
          <span v-if="store.cartCount > 0"
            class="absolute top-0 right-0 bg-red-500 text-white text-[10px] w-4 h-4 rounded-full flex items-center justify-center">{{
              store.cartCount }}</span>
        </button>
      </div>
    </header>

    <div class="flex-1">
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </div>

    <footer class="bg-slate-900 text-slate-400 py-12 mt-12 hidden md:block">
      <div class="container mx-auto px-4 grid grid-cols-1 md:grid-cols-3 gap-8 text-sm">
        <div>
          <h4 class="text-white font-bold text-lg mb-4">渔鲜直供</h4>
          <p class="leading-relaxed opacity-80">
            汇聚世界顶级渔场，<br>将深海的纯净直接送达您的餐桌。
          </p>
        </div>
        <div>
          <h4 class="text-white font-bold mb-4">快速链接</h4>
          <ul class="space-y-2">
            <li><a href="#" class="hover:text-white transition">关于我们</a></li>
            <li><a href="#" class="hover:text-white transition">物流配送</a></li>
            <li><a href="#" class="hover:text-white transition">售后服务</a></li>
          </ul>
        </div>
        <div>
          <h4 class="text-white font-bold mb-4">联系方式</h4>
          <p>客服热线: 400-888-6666</p>
          <p>工作时间: 09:00 - 22:00</p>
        </div>
      </div>
      <div class="border-t border-slate-800 mt-8 pt-8 text-center text-xs opacity-50">
        &copy; 2025 YU XIAN Seafood Supply Chain. All rights reserved.
      </div>
    </footer>

    <nav
      class="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-slate-200 h-16 flex justify-around items-center z-50 pb-safe shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)]">
      <RouterLink to="/" class="flex flex-col items-center gap-1 w-16"
        :class="isActive('/') ? 'text-blue-600' : 'text-slate-400'">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>
        <span class="text-[10px] font-bold">首页</span>
      </RouterLink>

      <RouterLink to="/cart" class="flex flex-col items-center gap-1 w-16 relative"
        :class="isActive('/cart') ? 'text-blue-600' : 'text-slate-400'">
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

      <div class="flex flex-col items-center gap-1 w-16"
        :class="isActive('/profile') ? 'text-blue-600' : 'text-slate-400'">
        <div v-if="!store.currentUser" @click="$router.push('/login')"
          class="flex flex-col items-center cursor-pointer">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
          </svg>
          <span class="text-[10px] font-bold">登录</span>
        </div>
        <RouterLink v-else to="/profile" class="flex flex-col items-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
          <span class="text-[10px] font-bold">我的</span>
        </RouterLink>
      </div>
    </nav>
  </div>
</template>

<style>
@keyframes slide-in {
  from {
    transform: translateX(100%);
    opacity: 0;
  }

  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.animate-slide-in {
  animation: slide-in 0.3s ease-out forwards;
}

/* 【新增】页面切换动画 */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-enter-from,
.page-leave-to {
  opacity: 0;
  transform: translateY(5px);
  /* 轻微上浮效果 */
}
</style>