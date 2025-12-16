<script setup>
import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'

const router = useRouter()
const route = useRoute()
const payMethod = computed(() => route.query.method || 'alipay')

const payInfo = computed(() => {
  if (payMethod.value === 'wechatpay') {
    return { name: '微信支付', color: 'text-[#07C160]', bg: 'bg-[#07C160]/10' }
  }
  return { name: '支付宝', color: 'text-[#1677FF]', bg: 'bg-[#1677FF]/10' }
})
</script>

<template>
  <div class="min-h-[80vh] flex flex-col items-center justify-center text-center px-4">
    
    <div class="w-24 h-24 bg-green-100 rounded-full flex items-center justify-center mb-6 animate-bounce-slow shadow-lg shadow-green-100">
      <svg class="w-12 h-12 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path>
      </svg>
    </div>

    <h1 class="text-3xl font-serif-sc font-bold text-slate-900 mb-2">支付成功</h1>
    <p class="text-slate-500 mb-10">感谢您的购买，仓库正在打印发货单...</p>

    <div class="bg-white p-8 rounded-3xl border border-slate-100 shadow-xl shadow-slate-200/50 w-full max-w-sm mb-10 relative overflow-hidden">
      <div class="absolute top-0 left-0 w-full h-1.5 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500"></div>
      
      <div class="space-y-5 text-sm">
        <div class="flex justify-between items-center text-slate-600">
          <span>收款方</span>
          <span class="font-bold text-slate-800 flex items-center gap-1">
            <img src="/icons/logo.png" class="w-4 h-4 opacity-80"> 渔鲜直供自营店
          </span>
        </div>
        
        <div class="flex justify-between items-center text-slate-600">
          <span>支付方式</span>
          <div class="flex items-center gap-2 font-bold text-slate-800">
            <img v-if="payMethod === 'alipay'" src="/icons/alipay.png" class="w-6 h-6 object-contain">
            <img v-else src="/icons/wechatpay.png" class="w-6 h-6 object-contain">
            {{ payInfo.name }}
          </div>
        </div>

        <div class="border-b-2 border-dashed border-slate-100 -mx-8 relative">
          <div class="absolute -left-2 -top-2 w-4 h-4 bg-white rounded-full border border-slate-100/50"></div>
          <div class="absolute -right-2 -top-2 w-4 h-4 bg-white rounded-full border border-slate-100/50"></div>
        </div>

        <div class="flex justify-between items-center pt-2">
          <span class="text-slate-600">实付金额</span>
          <span class="text-3xl font-bold text-blue-900 font-serif-sc">¥{{ $route.query.amount || '0.00' }}</span>
        </div>
      </div>
    </div>

    <div class="flex gap-4">
      <button 
        @click="router.push('/profile')" 
        class="px-8 py-3 bg-white border border-slate-200 rounded-xl text-slate-600 font-bold hover:border-slate-400 hover:text-slate-800 transition"
      >
        查看订单
      </button>
      <button 
        @click="router.push('/')" 
        class="px-8 py-3 bg-slate-900 text-white rounded-xl font-bold hover:bg-blue-900 transition shadow-lg shadow-slate-900/20"
      >
        继续逛逛
      </button>
    </div>
  </div>
</template>

<style>
@keyframes bounce-slow {

  0%,
  100% {
    transform: translateY(-5%);
  }

  50% {
    transform: translateY(5%);
  }
}

.animate-bounce-slow {
  animation: bounce-slow 2s infinite ease-in-out;
}
</style>