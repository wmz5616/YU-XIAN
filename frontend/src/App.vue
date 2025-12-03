<script setup>
import { ref, watch, nextTick } from 'vue'
import { RouterView } from 'vue-router'
import { store } from './store.js'

const cartBtnRef = ref(null) // 购物车的 DOM 引用

// 监听飞入信号
watch(() => store.flySignal.id, () => {
  if (store.flySignal.rect) {
    startFlyAnimation(store.flySignal.rect, store.flySignal.img)
  }
})

// 创建并执行动画
const startFlyAnimation = (startRect, imgUrl) => {
  if (!cartBtnRef.value) return

  // 1. 获取终点坐标 (购物车按钮中心)
  const cartRect = cartBtnRef.value.getBoundingClientRect()
  const endX = cartRect.left + cartRect.width / 2
  const endY = cartRect.top + cartRect.height / 2

  // 2. 创建飞行元素 (外层控制X轴，内层控制Y轴)
  const flyer = document.createElement('div')
  flyer.className = 'flyer-box fixed z-[9999] pointer-events-none transition-transform duration-700 ease-linear'
  flyer.style.left = `${startRect.left}px`
  flyer.style.top = `${startRect.top}px`
  
  const inner = document.createElement('img')
  inner.src = imgUrl || '/icons/logo.png' // 没图就用Logo兜底
  inner.className = 'w-10 h-10 rounded-full border-2 border-blue-500 shadow-lg object-cover transition-transform duration-700'
  // 贝塞尔曲线：模拟向上抛物线 (先快后慢再快)
  inner.style.transitionTimingFunction = 'cubic-bezier(0.5, -0.5, 1, 1)' 
  
  flyer.appendChild(inner)
  document.body.appendChild(flyer)

  // 3. 强制重绘，确保初始位置生效
  flyer.offsetHeight 

  // 4. 开始动画 (设置位移)
  const x = endX - startRect.left
  const y = endY - startRect.top
  
  flyer.style.transform = `translateX(${x}px)`
  inner.style.transform = `translateY(${y}px) scale(0.2)` // 飞得同时变小

  // 5. 动画结束后清理 + 购物车抖动反馈
  setTimeout(() => {
    flyer.remove()
    // 这里可以加一个购物车抖动的逻辑，为了简单先省略
  }, 700)
}
</script>

<template>
  <div class="min-h-screen flex flex-col bg-slate-50 font-sans text-slate-800">
    
    <div 
      v-if="store.notification.show"
      class="fixed top-24 right-6 z-[100] bg-white/80 backdrop-blur-md border border-white/40 shadow-2xl rounded-2xl px-6 py-4 flex items-center gap-3 animate-slide-in transform transition-all hover:scale-105"
      :class="store.notification.type === 'success' ? 'text-green-600' : 'text-red-600'"
    >
      <img v-if="store.notification.type === 'success'" src="/icons/icon-success.png" class="w-6 h-6" />
      <img v-else src="/icons/icon-error.png" class="w-6 h-6" />
      <span class="font-bold text-slate-800 tracking-wide">{{ store.notification.message }}</span>
    </div>

    <header class="sticky top-0 z-50 bg-white/70 backdrop-blur-xl border-b border-slate-200/50 shadow-sm transition-all duration-300 supports-[backdrop-filter]:bg-white/60">
      <div class="container mx-auto px-4 h-16 flex items-center justify-between">
        <div class="flex items-center gap-2 cursor-pointer" @click="$router.push('/')">
          <img src="/icons/logo.png" class="w-10 h-10 object-contain" alt="Logo" />
          <span class="text-xl font-bold tracking-tight text-blue-950">渔鲜直供</span>
        </div>
        
        <nav class="hidden md:flex gap-6 text-sm font-medium text-slate-600 items-center">
          <RouterLink to="/" class="hover:text-blue-700 transition">首页</RouterLink>
          
          <div class="h-4 w-px bg-slate-200"></div>

          <div v-if="!store.currentUser" class="flex gap-4">
            <RouterLink to="/login" class="hover:text-blue-900">登录</RouterLink>
            <RouterLink to="/register" class="bg-slate-900 text-white px-3 py-1 rounded text-xs hover:bg-slate-700 transition">注册会员</RouterLink>
          </div>

          <div v-else class="flex items-center gap-3 pl-4 border-l border-slate-200">
            
            <RouterLink to="/profile" class="flex items-center gap-2 group">
              <div class="w-8 h-8 rounded-full bg-blue-100 overflow-hidden border border-blue-200">
                <img 
                  v-if="store.currentUser.avatar" 
                  :src="store.currentUser.avatar" 
                  class="w-full h-full object-cover"
                />
                <div v-else class="w-full h-full flex items-center justify-center text-blue-800 text-xs font-bold">
                  {{ store.currentUser.displayName.charAt(0) }}
                </div>
              </div>
              
              <span class="text-slate-700 font-bold group-hover:text-blue-900 transition">
                {{ store.currentUser.displayName }}
              </span>
            </RouterLink>

            <button @click="store.logout()" class="text-slate-400 hover:text-red-500 text-xs ml-2">
              退出
            </button>
          </div>
        </nav>

        <button 
          ref="cartBtnRef"  @click="$router.push('/cart')"
          class="flex items-center gap-2 px-5 py-2 bg-blue-600 text-white text-sm rounded-full hover:bg-blue-500 transition shadow-lg shadow-blue-600/20 active:scale-95"
        >
          <img src="/icons/icon-cart.png" class="w-4 h-4 brightness-200" alt="Cart" />
          <span>购物车 ({{ store.cartCount }})</span>
        </button>
      </div>
    </header>

    <div class="flex-1">
      <RouterView />
    </div>

    <footer class="bg-slate-900 text-slate-400 py-12 mt-12">
      <div class="container mx-auto px-4 grid grid-cols-1 md:grid-cols-3 gap-8 text-sm">
        <div>
          <h4 class="text-white font-bold text-lg mb-4">渔鲜直供</h4>
          <p class="leading-relaxed opacity-80">
            专注高品质海鲜供应链，<br>从舟山渔场直达您的餐桌。
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

  </div>
</template>

<style>
/* 简单的滑入动画 */
@keyframes slide-in {
  from { transform: translateX(100%); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}
.animate-slide-in {
  animation: slide-in 0.3s ease-out forwards;
}
</style>