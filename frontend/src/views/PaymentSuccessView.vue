<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="min-h-[80vh] flex flex-col items-center justify-center text-center px-4">
    <div class="w-24 h-24 bg-green-100 rounded-full flex items-center justify-center mb-6 animate-bounce-slow">
      <svg class="w-12 h-12 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path>
      </svg>
    </div>

    <h1 class="text-3xl font-serif-sc font-bold text-slate-900 mb-2">支付成功</h1>
    <p class="text-slate-500 mb-8">感谢您的购买，仓库正在打印发货单...</p>

    <div class="bg-white p-6 rounded-xl border border-slate-100 shadow-sm w-full max-w-sm mb-8 relative overflow-hidden">
      <div class="absolute top-0 left-0 w-full h-2 bg-slate-50" style="background-image: radial-gradient(circle, #fff 5px, transparent 6px); background-size: 16px 16px;"></div>
      
      <div class="space-y-3 text-sm">
        <div class="flex justify-between text-slate-600">
          <span>收款方</span>
          <span class="font-bold text-slate-800">渔鲜直供自营店</span>
        </div>
        <div class="flex justify-between text-slate-600">
          <span>支付方式</span>
          <span>支付宝 / 微信</span>
        </div>
        <div class="flex justify-between text-slate-600 border-t border-dashed border-slate-200 pt-3 mt-3">
          <span>实付金额</span>
          <span class="text-xl font-bold text-blue-900 font-serif-sc">¥{{ $route.query.amount || '0.00' }}</span>
        </div>
      </div>
    </div>

    <div class="flex gap-4">
      <button 
        @click="router.push('/profile')" 
        class="px-6 py-2 border border-slate-300 rounded-full text-slate-600 hover:border-slate-800 hover:text-slate-900 transition"
      >
        查看订单
      </button>
      <button 
        @click="router.push('/')" 
        class="px-6 py-2 bg-blue-900 text-white rounded-full hover:bg-blue-800 transition shadow-lg shadow-blue-900/20"
      >
        继续逛逛
      </button>
    </div>
  </div>
</template>

<style>
@keyframes bounce-slow {
  0%, 100% { transform: translateY(-5%); }
  50% { transform: translateY(5%); }
}
.animate-bounce-slow {
  animation: bounce-slow 2s infinite ease-in-out;
}
</style>