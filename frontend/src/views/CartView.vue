<script setup>
import { computed } from 'vue'
import { store } from '../store.js'
import { useRouter } from 'vue-router'

const router = useRouter()

const freight = computed(() => {
  const total = parseFloat(store.totalPrice)
  return total > 200 ? 0 : 20
})

const finalPrice = computed(() => {
  return (parseFloat(store.totalPrice) + freight.value).toFixed(2)
})

const handleCheckout = () => {
  if (store.items.length === 0) return
  if (!store.currentUser) {
    alert('请先登录结算！')
    router.push('/login')
    return
  }
  router.push('/checkout')
}
</script>

<template>
  <div class="container mx-auto px-4 py-8 max-w-4xl">
    <h1 class="text-3xl font-serif-sc font-bold text-slate-800 mb-8">您的购物车</h1>

    <div v-if="store.items.length > 0" class="flex flex-col lg:flex-row gap-8">

      <div class="flex-1 bg-white rounded-xl shadow-sm border border-slate-100 overflow-hidden">
        <ul class="divide-y divide-slate-100">
          <li v-for="item in store.items" :key="item.id"
            class="p-6 flex items-center gap-4 hover:bg-slate-50 transition">

            <div class="w-20 h-20 bg-slate-50 rounded-lg overflow-hidden flex-shrink-0 border border-slate-200">
              <img v-if="item.imageUrl" :src="item.imageUrl" class="w-full h-full object-cover" />
              <div v-else class="w-full h-full flex items-center justify-center text-2xl">🐟</div>
            </div>

            <div class="flex-1">
              <div class="flex justify-between items-start mb-1">
                <h3 class="font-bold text-slate-800">{{ item.name }}</h3>
                <button @click="store.removeItem(item.id)" class="text-slate-400 hover:text-red-500 text-xs">删除</button>
              </div>
              <p class="text-xs text-slate-500 mb-3">{{ item.origin }}直采</p>

              <div class="flex justify-between items-center">
                <span class="font-bold text-blue-900">¥{{ item.price }}</span>

                <div class="flex items-center bg-white border border-slate-200 rounded h-8">
                  <button @click="store.decreaseItem(item.id)"
                    class="w-8 h-full flex items-center justify-center text-slate-500 hover:bg-slate-50 disabled:opacity-30">-</button>
                  <span class="text-xs font-bold w-8 text-center">{{ item.quantity }}</span>
                  <button @click="store.addToCart(item)"
                    class="w-8 h-full flex items-center justify-center text-slate-500 hover:bg-slate-50"
                    :disabled="item.quantity >= item.stock">+</button>
                </div>
              </div>
            </div>
          </li>
        </ul>
      </div>

      <div class="w-full lg:w-80">
        <div class="bg-white rounded-xl shadow-lg border border-slate-100 p-6 sticky top-24">
          <h3 class="text-lg font-bold text-slate-800 mb-4">订单摘要</h3>
          <div class="flex justify-between mb-2 text-slate-600">
            <span>商品种类</span>
            <span>{{ store.items.length }} 种</span>
          </div>
          <div class="flex justify-between mb-2 text-slate-600">
            <span>商品总数</span>
            <span>{{ store.cartCount }} 件</span>
          </div>

          <div class="flex justify-between mb-6 text-slate-600">
            <span>运费</span>
            <span :class="freight === 0 ? 'text-green-600 font-bold' : 'text-slate-900'">
              {{ freight === 0 ? '免运费' : `+ ¥${freight}` }}
            </span>
          </div>
          <div class="border-t border-slate-100 pt-4 flex justify-between items-end mb-6">
            <span class="font-bold text-slate-800">总计</span>
            <span class="text-3xl font-bold text-blue-900">¥{{ finalPrice }}</span>
          </div>
          <button @click="handleCheckout"
            class="w-full bg-blue-900 hover:bg-blue-800 text-white font-bold py-3 rounded-lg shadow-md transition transform active:scale-95">
            立即支付
          </button>

          <button @click="router.push('/')" class="w-full mt-3 text-sm text-slate-400 hover:text-slate-600">
            继续购物
          </button>
        </div>
      </div>

    </div>

    <div v-else class="flex flex-col items-center justify-center py-24">
      <div class="relative w-40 h-40 mb-6 flex items-center justify-center">
        <div
          class="absolute inset-0 border-2 border-dashed border-slate-200 rounded-full animate-[spin_10s_linear_infinite]">
        </div>
        <img src="/icons/empty-cart.png" class="w-24 h-24 object-contain relative z-10 opacity-80" alt="空购物车" />
      </div>
      <h2 class="text-2xl font-serif-sc font-bold text-slate-800 mb-3">购物车空空如也</h2>
      <button @click="router.push('/')"
        class="group relative px-8 py-3 bg-blue-900 text-white rounded-full font-bold shadow-lg transition overflow-hidden">
        <span class="relative z-10">去逛逛</span>
      </button>
    </div>
  </div>
</template>