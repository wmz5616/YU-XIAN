<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="min-h-[80vh] flex flex-col items-center justify-center text-center px-4">
    <div class="relative mb-8">
      <div class="text-9xl font-bold text-slate-100 font-serif-sc">404</div>
      <div class="absolute inset-0 flex items-center justify-center">
        <img src="/icons/empty-order.png" class="w-24 h-24 opacity-80" />
      </div>
    </div>
    
    <h2 class="text-2xl font-bold text-slate-800 mb-2">哎呀，页面游走了...</h2>
    <p class="text-slate-500 mb-8 max-w-xs mx-auto">您寻找的海鲜可能已经游回大海，或者您输入的地址有误。</p>
    
    <button 
      @click="router.push('/')"
      class="px-8 py-3 bg-blue-900 text-white rounded-full font-bold hover:bg-blue-800 transition shadow-lg hover:shadow-blue-900/20 active:scale-95"
    >
      返回首页
    </button>
  </div>
</template>