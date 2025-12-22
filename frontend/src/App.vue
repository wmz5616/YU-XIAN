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

// 获取光晕颜色
const getGlowColor = (type) => {
  switch (type) {
    case 'success': return '0 10px 40px -10px rgba(16, 185, 129, 0.4), inset 0 0 20px rgba(16, 185, 129, 0.1)'
    case 'error': return '0 10px 40px -10px rgba(244, 63, 94, 0.4), inset 0 0 20px rgba(244, 63, 94, 0.1)'
    case 'warning': return '0 10px 40px -10px rgba(245, 158, 11, 0.4), inset 0 0 20px rgba(245, 158, 11, 0.1)'
    case 'loading': return '0 10px 40px -10px rgba(34, 211, 238, 0.4), inset 0 0 20px rgba(34, 211, 238, 0.1)'
    default: return '0 10px 40px -10px rgba(255, 255, 255, 0.1)'
  }
}

// 获取呼吸灯颜色
const getLightColor = (type) => {
  switch (type) {
    case 'success': return 'bg-emerald-400'
    case 'error': return 'bg-rose-500'
    case 'warning': return 'bg-amber-400'
    case 'loading': return 'bg-cyan-400'
    default: return 'bg-slate-400'
  }
}
</script>

<template>
  <div class="min-h-screen flex flex-col bg-slate-50 font-sans text-slate-800 pb-16 md:pb-0">

    <Transition name="liquid">
      <div v-if="store.notification.show" class="fixed top-6 left-1/2 -translate-x-1/2 z-[10000] flex items-center justify-between gap-4 px-5 py-3.5 
        min-w-[340px] max-w-[90vw] cursor-pointer select-none overflow-hidden origin-top
        
        bg-slate-950/90
        backdrop-blur-2xl
        rounded-[32px]
        border border-white/10
        shadow-[0_20px_60px_-15px_rgba(0,0,0,0.6)]
        
        will-change-transform will-change-width will-change-radius
        " :class="{
          'pr-6': store.notification.type !== 'loading',
          'pr-5': store.notification.type === 'loading'
        }" :style="{
          boxShadow: getGlowColor(store.notification.type),
          transition: 'background-color 0.3s, box-shadow 0.3s'
        }" @click="store.notification.show = false">

        <div
          class="absolute inset-0 z-0 bg-gradient-to-r from-transparent via-white/15 to-transparent -translate-x-full animate-scan-once pointer-events-none">
        </div>

        <div
          class="absolute inset-x-0 top-0 h-[1px] bg-gradient-to-r from-transparent via-white/40 to-transparent opacity-70">
        </div>

        <div class="relative w-9 h-9 flex-shrink-0 flex items-center justify-center z-10">
          <div v-if="store.notification.type === 'success'"
            class="w-7 h-7 rounded-full bg-gradient-to-b from-emerald-400 to-emerald-600 flex items-center justify-center shadow-[0_0_15px_rgba(16,185,129,0.5)] animate-scale-in">
            <svg class="w-4 h-4 text-white drop-shadow-md" fill="none" viewBox="0 0 24 24" stroke="currentColor"
              stroke-width="3.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>

          <div v-else-if="store.notification.type === 'error'"
            class="w-7 h-7 rounded-full bg-gradient-to-b from-rose-500 to-red-600 flex items-center justify-center shadow-[0_0_15px_rgba(244,63,94,0.5)] animate-shake">
            <svg class="w-4 h-4 text-white drop-shadow-md" fill="none" viewBox="0 0 24 24" stroke="currentColor"
              stroke-width="3">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>

          <div v-else-if="store.notification.type === 'warning'"
            class="w-7 h-7 rounded-full bg-gradient-to-b from-amber-400 to-orange-500 flex items-center justify-center shadow-[0_0_15px_rgba(245,158,11,0.5)]">
            <span class="text-white font-black text-sm drop-shadow-md">!</span>
          </div>

          <div v-else-if="store.notification.type === 'loading'" class="w-7 h-7 relative">
            <div class="absolute inset-0 rounded-full border-2 border-white/20"></div>
            <div
              class="absolute inset-0 rounded-full border-2 border-t-cyan-400 border-r-cyan-400/30 border-b-transparent border-l-transparent animate-spin blur-[0.5px]">
            </div>
          </div>
        </div>

        <div class="flex-1 flex flex-col justify-center min-h-[28px] z-10">
          <span
            class="text-[14px] font-medium text-white/95 leading-tight tracking-wide antialiased drop-shadow-md font-sans">
            {{ store.notification.message }}
          </span>
        </div>

        <div class="flex items-center justify-center w-3 h-3 z-10 mr-1">
          <span class="absolute w-2 h-2 rounded-full animate-ping opacity-75"
            :class="getLightColor(store.notification.type)"></span>
          <span class="relative w-1.5 h-1.5 rounded-full shadow-[0_0_6px_rgba(255,255,255,0.9)]"
            :class="getLightColor(store.notification.type)"></span>
        </div>

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

    <footer v-if="!isAdminRoute" class="relative bg-[#0B1120] text-slate-400 py-16 mt-0 border-t border-white/5 overflow-hidden hidden md:block group font-sans">
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-blue-900/10 via-[#0B1120] to-[#0B1120] pointer-events-none"></div>

      <div class="container mx-auto px-6 relative z-10">
        <div class="grid grid-cols-1 md:grid-cols-12 gap-12 text-sm">
            
            <div class="col-span-1 md:col-span-4 space-y-6">
                <div class="flex items-center gap-4">
                     <div class="w-12 h-12 bg-white/5 backdrop-blur-sm rounded-xl flex items-center justify-center p-2 border border-white/10 shadow-2xl">
                        <img src="/icons/logo-white.png" class="w-full h-full object-contain opacity-90" />
                     </div>
                     <div>
                        <span class="block text-xl font-bold text-white tracking-[0.2em] font-serif-sc">渔鲜直供</span>
                        <span class="text-[10px] text-slate-500 uppercase tracking-widest font-bold">Premium Seafood</span>
                     </div>
                </div>
                <p class="leading-loose opacity-60 font-light text-xs max-w-xs text-justify">
                    深耕全球海域，甄选顶级食材。<br>
                    我们不生产海鲜，我们只是大自然的搬运工，致力于为中国家庭餐桌提供极致的鲜美体验。
                </p>
                <div class="flex gap-4 pt-2">
                    <a href="#" class="w-9 h-9 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center transition-all duration-300 hover:scale-110 border border-white/5 group/icon">
                        <img src="/icons/wechat.png" class="w-4 h-4 opacity-50 group-hover/icon:opacity-100 transition-all duration-300 grayscale group-hover/icon:grayscale-0" />
                    </a>
                    <a href="#" class="w-9 h-9 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center transition-all duration-300 hover:scale-110 border border-white/5 group/icon">
                         <img src="/icons/douyin.png" class="w-4 h-4 opacity-50 group-hover/icon:opacity-100 transition-all duration-300 grayscale group-hover/icon:grayscale-0" />
                    </a>
                     <a href="#" class="w-9 h-9 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center transition-all duration-300 hover:scale-110 border border-white/5 group/icon">
                         <img src="/icons/redbook.png" class="w-4 h-4 opacity-50 group-hover/icon:opacity-100 transition-all duration-300 grayscale group-hover/icon:grayscale-0" />
                    </a>
                </div>
            </div>

            <div class="col-span-1 md:col-span-2 md:col-start-6">
                <h4 class="text-white font-bold mb-6 tracking-[0.2em] text-xs uppercase opacity-90 flex items-center gap-2">
                    <span class="w-1 h-3 bg-blue-500 rounded-full shadow-[0_0_10px_rgba(59,130,246,0.5)]"></span> 探索
                </h4>
                <ul class="space-y-4 font-light text-xs text-slate-400">
                    <li><a href="#" class="hover:text-blue-400 hover:translate-x-1 transition-all inline-block flex items-center gap-2 group/link"><span class="opacity-0 -ml-2 group-hover/link:opacity-100 group-hover/link:ml-0 transition-all text-blue-500">›</span> 当季热销</a></li>
                    <li><a href="#" class="hover:text-blue-400 hover:translate-x-1 transition-all inline-block flex items-center gap-2 group/link"><span class="opacity-0 -ml-2 group-hover/link:opacity-100 group-hover/link:ml-0 transition-all text-blue-500">›</span> 主厨推荐</a></li>
                    <li><a href="#" class="hover:text-blue-400 hover:translate-x-1 transition-all inline-block flex items-center gap-2 group/link"><span class="opacity-0 -ml-2 group-hover/link:opacity-100 group-hover/link:ml-0 transition-all text-blue-500">›</span> 礼品卡兑换</a></li>
                </ul>
            </div>

            <div class="col-span-1 md:col-span-2">
                 <h4 class="text-white font-bold mb-6 tracking-[0.2em] text-xs uppercase opacity-90 flex items-center gap-2">
                    <span class="w-1 h-3 bg-emerald-500 rounded-full shadow-[0_0_10px_rgba(16,185,129,0.5)]"></span> 服务
                </h4>
                <ul class="space-y-4 font-light text-xs text-slate-400">
                    <li><a href="#" class="hover:text-emerald-400 hover:translate-x-1 transition-all inline-block flex items-center gap-2 group/link"><span class="opacity-0 -ml-2 group-hover/link:opacity-100 group-hover/link:ml-0 transition-all text-emerald-500">›</span> 全程冷链</a></li>
                    <li><a href="#" class="hover:text-emerald-400 hover:translate-x-1 transition-all inline-block flex items-center gap-2 group/link"><span class="opacity-0 -ml-2 group-hover/link:opacity-100 group-hover/link:ml-0 transition-all text-emerald-500">›</span> 售后无忧</a></li>
                    <li><a href="#" class="hover:text-emerald-400 hover:translate-x-1 transition-all inline-block flex items-center gap-2 group/link"><span class="opacity-0 -ml-2 group-hover/link:opacity-100 group-hover/link:ml-0 transition-all text-emerald-500">›</span> 商务合作</a></li>
                </ul>
            </div>

            <div class="col-span-1 md:col-span-3">
                 <h4 class="text-white font-bold mb-6 tracking-[0.2em] text-xs uppercase opacity-90 flex items-center gap-2">
                    <span class="w-1 h-3 bg-orange-500 rounded-full shadow-[0_0_10px_rgba(249,115,22,0.5)]"></span> 联系
                </h4>
                
                <div class="mb-8 group/phone cursor-default">
                    <div class="text-3xl font-serif-sc font-bold text-transparent bg-clip-text bg-gradient-to-r from-white to-slate-400 mb-2 group-hover/phone:to-white transition-all">400-888-6666</div>
                    <p class="text-[10px] opacity-50 tracking-wide flex items-center gap-2 font-mono">
                        <span class="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></span>
                        星期一 - 星期日: 09:00 - 22:00
                    </p>
                </div>

                <div>
                     <p class="text-[10px] opacity-30 uppercase tracking-widest mb-3 font-bold">Secure Payment</p>
                     <div class="flex gap-3">
                        <div class="h-8 px-4 bg-white/5 border border-white/10 rounded flex items-center justify-center grayscale hover:grayscale-0 transition-all duration-300 opacity-60 hover:opacity-100 hover:bg-white/10 hover:shadow-lg cursor-pointer">
                            <img src="/icons/alipay.png" class="h-4 object-contain" />
                        </div>
                        <div class="h-8 px-4 bg-white/5 border border-white/10 rounded flex items-center justify-center grayscale hover:grayscale-0 transition-all duration-300 opacity-60 hover:opacity-100 hover:bg-white/10 hover:shadow-lg cursor-pointer">
                            <img src="/icons/wechatpay.png" class="h-4 object-contain" />
                        </div>
                     </div>
                </div>
            </div>
        </div>

        <div class="border-t border-white/5 mt-16 pt-8 flex flex-col md:flex-row justify-between items-center text-[10px] opacity-40 uppercase tracking-widest gap-4">
            <p class="font-mono">&copy; 2025 YU XIAN Direct Supply. All rights reserved.</p>
            <div class="flex gap-8">
                <a href="#" class="hover:text-white transition-colors relative after:content-[''] after:absolute after:bottom-0 after:left-0 after:w-0 after:h-[1px] after:bg-white after:transition-all hover:after:w-full">Privacy Policy</a>
                <a href="#" class="hover:text-white transition-colors relative after:content-[''] after:absolute after:bottom-0 after:left-0 after:w-0 after:h-[1px] after:bg-white after:transition-all hover:after:w-full">Terms of Service</a>
                <a href="#" class="hover:text-white transition-colors relative after:content-[''] after:absolute after:bottom-0 after:left-0 after:w-0 after:h-[1px] after:bg-white after:transition-all hover:after:w-full">Sitemap</a>
            </div>
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
/* 🚀 性能优化: 开启硬件加速提示 */
.will-change-transform {
  will-change: transform;
}

.will-change-width {
  will-change: width;
}

.will-change-radius {
  will-change: border-radius;
}

/* 🌊 终极液态物理引擎: 滴落 -> 拉伸 -> 摊开 -> 回弹 */
.liquid-enter-active {
  animation: liquid-drop 0.75s cubic-bezier(0.23, 1, 0.32, 1) forwards;
}

/* 离场: 时间倒流，实现丝滑吸回效果 */
.liquid-leave-active {
  animation: liquid-drop 0.6s cubic-bezier(0.5, 0, 0.75, 0) reverse forwards;
}

@keyframes liquid-drop {
  0% {
    opacity: 0;
    /* 使用 translate3d 强制 GPU 加速 */
    transform: translate3d(-50%, -120%, 0) scale(0.5, 0.5);
    width: 60px;
    border-radius: 5px 5px 50% 50%;
    padding-top: 0;
  }

  30% {
    opacity: 1;
    transform: translate3d(-50%, 10px, 0) scale(0.9, 1.3);
    width: 280px;
    border-radius: 15px 15px 30px 30px;
  }

  55% {
    transform: translate3d(-50%, -2px, 0) scale(1.05, 0.9);
    width: 360px;
    border-radius: 32px;
  }

  75% {
    transform: translate3d(-50%, 1px, 0) scale(0.98, 1.02);
    width: 335px;
  }

  100% {
    opacity: 1;
    transform: translate3d(-50%, 0, 0) scale(1, 1);
    width: auto;
    border-radius: 32px;
  }
}

/* 全息扫描光波 */
@keyframes scan-once {
  0% {
    transform: translateX(-100%) skewX(-15deg);
    opacity: 0;
  }

  20% {
    opacity: 1;
  }

  100% {
    transform: translateX(200%) skewX(-15deg);
    opacity: 0;
  }
}

.animate-scan-once {
  animation: scan-once 0.8s ease-out forwards;
}

/* 图标弹入 */
@keyframes scale-in {
  0% {
    transform: scale(0);
    opacity: 0;
  }

  60% {
    transform: scale(1.15);
  }

  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.animate-scale-in {
  animation: scale-in 0.4s 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
}

/* 错误震动 */
@keyframes shake {

  0%,
  100% {
    transform: translateX(0);
  }

  25% {
    transform: translateX(-4px);
  }

  75% {
    transform: translateX(4px);
  }
}

.animate-shake {
  animation: shake 0.3s ease-in-out;
}

/* 页面切换动画 */
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

div:where(.swal2-container) div:where(.swal2-popup) {
  background-color: #ffffff !important;
  /* 修复：移除了复杂的 SVG 背景，防止 CSS 解析错误导致页面显示白条 */
  border-radius: 32px !important;
  padding: 3rem 2.5rem !important;
  color: #1e293b !important;
  box-shadow:
    0 20px 60px -10px rgba(0, 0, 0, 0.15),
    0 10px 20px -5px rgba(0, 0, 0, 0.04),
    inset 0 0 0 1px rgba(255, 255, 255, 1) !important;
  border: 1px solid rgba(0, 0, 0, 0.03) !important;
}

div:where(.swal2-container) .swal2-title {
  color: #0f172a !important;
  font-family: "Georgia", "Songti SC", serif !important;
  font-weight: 900 !important;
  letter-spacing: -0.02em !important;
  font-size: 1.75rem !important;
  margin-bottom: 0.8rem !important;
}

div:where(.swal2-container) .swal2-html-container {
  color: #64748b !important;
  font-size: 1rem !important;
  line-height: 1.8 !important;
  font-weight: 400 !important;
}

div:where(.swal2-icon) {
  border-width: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  transform: scale(0.9);
  margin-bottom: 2rem !important;
}

div:where(.swal2-icon).swal2-success {
  color: #059669 !important;
}

div:where(.swal2-icon).swal2-success [class^=swal2-success-line] {
  background-color: #059669 !important;
}

div:where(.swal2-icon).swal2-success .swal2-success-ring {
  background-color: rgba(5, 150, 105, 0.05);
}

div:where(.swal2-icon).swal2-error {
  color: #e11d48 !important;
}

div:where(.swal2-icon).swal2-error [class^=swal2-x-mark-line] {
  background-color: #e11d48 !important;
}

div:where(.swal2-icon).swal2-error {
  background-color: rgba(225, 29, 72, 0.05) !important;
}

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

div:where(.swal2-container) button.swal2-confirm {
  background: #0f172a !important;
  color: #ffffff !important;
  border: 1px solid #0f172a !important;
  position: relative;
  overflow: hidden;
}

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

div:where(.swal2-container) button.swal2-cancel {
  background: transparent !important;
  color: #94a3b8 !important;
  border: 1px solid #e2e8f0 !important;
}

div:where(.swal2-container) button.swal2-cancel:hover {
  color: #0f172a !important;
  border-color: #cbd5e1 !important;
  background: #f8fafc !important;
}

div:where(.swal2-container).swal2-backdrop-show {
  background: rgba(241, 245, 249, 0.7) !important;
  backdrop-filter: blur(12px) !important;
}

.amap-logo,
.amap-copyright,
.amap-geolocation-con {
  display: none !important;
  visibility: hidden !important;
}

iframe[id^="amap"] {
  display: none !important;
  width: 0 !important;
  height: 0 !important;
  opacity: 0 !important;
  pointer-events: none !important;
}
</style>