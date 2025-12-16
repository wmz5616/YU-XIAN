<script setup>
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { RouterView, useRouter, useRoute } from 'vue-router'
import { store } from './store.js'

const router = useRouter()
const route = useRoute()
const cartBtnRef = ref(null)
const isScrolled = ref(false)

// ✅ 核心判断：当前是否为后台管理页面
// 只要路由路径以 '/admin' 开头，就认为是后台，隐藏商城组件
const isAdminRoute = computed(() => route.path.startsWith('/admin'))

// 滚动监听逻辑
const handleScroll = () => {
  isScrolled.value = window.scrollY > 20
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

// 计算属性：判断顶栏是否应该透明
// 只有在 "首页" 且 "未滚动" 时才透明，其他情况都是磨砂白
const isTransparent = computed(() => {
  return route.path === '/' && !isScrolled.value
})

// 监听加入购物车动画
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

  // 强制重绘
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
              ? (isTransparent ? 'text-white' : 'text-blue-600')
              : (isTransparent ? 'text-white/80 hover:text-white' : 'text-slate-600 hover:text-blue-600')
          ]">
            首页
            <span
              class="absolute bottom-0 left-1/2 -translate-x-1/2 w-0 h-0.5 transition-all duration-300 group-hover:w-full"
              :class="isTransparent ? 'bg-white' : 'bg-blue-600'"></span>
          </RouterLink>

          <div v-if="!store.currentUser" class="flex gap-4 items-center">
            <RouterLink to="/login" class="text-sm font-medium transition-colors hover:scale-105"
              :class="isTransparent ? 'text-white hover:text-white' : 'text-slate-600 hover:text-blue-600'">
              登录
            </RouterLink>
            <RouterLink to="/register"
              class="px-5 py-2 rounded-full text-xs font-bold transition-all shadow-lg hover:-translate-y-0.5 active:translate-y-0"
              :class="isTransparent ? 'bg-white text-blue-900 hover:bg-blue-50' : 'bg-slate-900 text-white hover:bg-slate-800'">
              注册会员
            </RouterLink>
          </div>

          <div v-else class="flex items-center gap-4">

            <RouterLink v-if="store.currentUser.role === 'ADMIN'" to="/admin"
              class="text-xs font-bold px-3 py-1 rounded-full border transition-all flex items-center gap-1"
              :class="isTransparent ? 'bg-white/20 text-white border-white/30 hover:bg-white/30' : 'bg-red-50 text-red-600 border-red-100 hover:bg-red-100'">
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
                <div class="w-9 h-9 rounded-full overflow-hidden border-2 transition-all shadow-md"
                  :class="isTransparent ? 'border-white/50' : 'border-blue-100'">
                  <img v-if="store.currentUser.avatar" :src="store.currentUser.avatar"
                    class="w-full h-full object-cover" />
                  <div v-else
                    class="w-full h-full flex items-center justify-center bg-slate-100 text-blue-600 font-bold text-xs">
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
              ? 'bg-white text-blue-900 hover:bg-blue-50'
              : 'bg-blue-600 text-white hover:bg-blue-700 shadow-blue-500/30'
          ]">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 transition-transform group-hover:scale-110" fill="none"
            viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
          <span class="text-sm font-bold">购物车</span>
          <span v-if="store.cartCount > 0"
            class="flex items-center justify-center w-5 h-5 rounded-full text-[10px] font-bold ml-1 transition-colors"
            :class="isTransparent ? 'bg-blue-900 text-white' : 'bg-white text-blue-600'">
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

        <div class="grid grid-cols-3 gap-8 pb-12 border-b border-slate-800/60 mb-12 text-center">
          <div class="flex flex-col items-center gap-4 group cursor-default">
            <div
              class="w-14 h-14 rounded-2xl bg-slate-800/30 border border-slate-700/50 flex items-center justify-center group-hover:bg-slate-800 group-hover:border-slate-600 transition-all duration-500">
              <svg xmlns="http://www.w3.org/2000/svg"
                class="w-7 h-7 text-slate-300 group-hover:text-white transition-colors" fill="none" viewBox="0 0 24 24"
                stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <h5 class="text-slate-200 font-bold mb-1 tracking-wide">源头直采</h5>
              <p class="text-xs text-slate-500 font-light">原产</p>
            </div>
          </div>

          <div class="flex flex-col items-center gap-4 group cursor-default">
            <div
              class="w-14 h-14 rounded-2xl bg-slate-800/30 border border-slate-700/50 flex items-center justify-center group-hover:bg-slate-800 group-hover:border-slate-600 transition-all duration-500">
              <svg xmlns="http://www.w3.org/2000/svg"
                class="w-7 h-7 text-slate-300 group-hover:text-white transition-colors" fill="none" viewBox="0 0 24 24"
                stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M12 3v2.25m6.364.386l-1.591 1.591M21 12h-2.25m-.386 6.364l-1.591-1.591M12 18.75V21m-4.773-4.227l-1.591 1.591M5.25 12H3m4.227-4.773L5.636 5.636M15.75 12a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0z" />
              </svg>
            </div>
            <div>
              <h5 class="text-slate-200 font-bold mb-1 tracking-wide">全程冷链</h5>
              <p class="text-xs text-slate-500 font-light">-18°C新鲜贮藏</p>
            </div>
          </div>

          <div class="flex flex-col items-center gap-4 group cursor-default">
            <div
              class="w-14 h-14 rounded-2xl bg-slate-800/30 border border-slate-700/50 flex items-center justify-center group-hover:bg-slate-800 group-hover:border-slate-600 transition-all duration-500">
              <svg xmlns="http://www.w3.org/2000/svg"
                class="w-7 h-7 text-slate-300 group-hover:text-white transition-colors" fill="none" viewBox="0 0 24 24"
                stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M9 12.75L11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 01-1.043 3.296 3.745 3.745 0 01-3.296 1.043A3.745 3.745 0 0112 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 01-3.296-1.043 3.745 3.745 0 01-1.043-3.296A3.745 3.745 0 013 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 011.043-3.296 3.746 3.746 0 013.296-1.043A3.746 3.746 0 0112 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 013.296 1.043 3.746 3.746 0 011.043 3.296A3.745 3.745 0 0121 12z" />
              </svg>
            </div>
            <div>
              <h5 class="text-slate-200 font-bold mb-1 tracking-wide">坏单包赔</h5>
              <p class="text-xs text-slate-500 font-light">全面保障</p>
            </div>
          </div>
        </div>

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
              致力于为中国家庭提供高品质的深海食材。<br>我们就做大自然的搬运工，<br>让您足不出户尝遍全球鲜味。
            </p>
            <div class="flex gap-4">
              <a href="#"
                class="w-9 h-9 rounded-full bg-slate-800 flex items-center justify-center hover:bg-[#07C160] hover:text-white transition-all duration-300 group">
                <img src="/icons/wechat.png" class="w-5 h-5 object-contain" alt="微信" />
              </a>
              <a href="#"
                class="w-9 h-9 rounded-full bg-slate-800 flex items-center justify-center hover:bg-[#FF2442] hover:text-white transition-all duration-300 group">
                <img src="/icons/redbook.png" class="w-5 h-5 object-contain" alt="小红书" />
              </a>
              <a href="#"
                class="w-9 h-9 rounded-full bg-slate-800 flex items-center justify-center hover:bg-black hover:text-black transition-all duration-300 group">
                <img src="/icons/douyin.png" class="w-5 h-5 object-contain" alt="抖音" />
              </a>
            </div>
          </div>

          <div class="pl-0 md:pl-8">
            <h4 class="text-white font-bold mb-6 tracking-widest text-xs uppercase opacity-80">探索</h4>
            <ul class="space-y-4 font-light text-xs text-slate-400">
              <li><a href="#" class="hover:text-white hover:translate-x-1 transition-all inline-block">当季热销</a></li>
              <li><a href="#" class="hover:text-white hover:translate-x-1 transition-all inline-block">主厨推荐</a></li>
              <li><a href="#" class="hover:text-white hover:translate-x-1 transition-all inline-block">新人优惠</a></li>
              <li><a href="#" class="hover:text-white hover:translate-x-1 transition-all inline-block">企业采购</a></li>
            </ul>
          </div>

          <div>
            <h4 class="text-white font-bold mb-6 tracking-widest text-xs uppercase opacity-80">支持</h4>
            <ul class="space-y-4 font-light text-xs text-slate-400">
              <li><a href="#" class="hover:text-white hover:translate-x-1 transition-all inline-block">物流追踪</a></li>
              <li><a href="#" class="hover:text-white hover:translate-x-1 transition-all inline-block">退换货政策</a></li>
              <li><a href="#" class="hover:text-white hover:translate-x-1 transition-all inline-block">帮助中心</a></li>
              <li><a href="#" class="hover:text-white hover:translate-x-1 transition-all inline-block">商务合作</a></li>
            </ul>
          </div>

          <div>
            <h4 class="text-white font-bold mb-6 tracking-widest text-xs uppercase opacity-80">联系方式</h4>
            <div class="text-3xl font-serif-sc font-bold text-white mb-2">123-456-7890</div>
            <p class="text-[10px] opacity-50 mb-6 tracking-wide">周一至周日 09:00 - 22:00 (UTC)</p>
            <button
              class="group border border-slate-700 rounded-full px-6 py-2.5 text-xs hover:bg-white hover:text-black hover:border-white transition-all duration-300 flex items-center gap-2">
              <span>在线客服咨询</span>
              <span class="group-hover:translate-x-1 transition-transform">→</span>
            </button>
          </div>
        </div>

        <div
          class="border-t border-slate-800/60 mt-16 pt-8 flex flex-col md:flex-row justify-between items-center text-[10px] opacity-40 uppercase tracking-widest">
          <p>&copy; 2025 YU XIAN Seafood Supply Chain. All rights reserved.</p>
          <div class="flex gap-6 mt-4 md:mt-0">
            <span class="cursor-pointer hover:text-white transition">隐私政策</span>
            <span class="cursor-pointer hover:text-white transition">服务条款</span>
            <span class="cursor-pointer hover:text-white transition">Cookie设置</span>
          </div>
        </div>
      </div>
    </footer>

    <nav v-if="!isAdminRoute"
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
/* 动画保持不变 */
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

.page-enter-active,
.page-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-enter-from,
.page-leave-to {
  opacity: 0;
  transform: translateY(5px);
}
</style>