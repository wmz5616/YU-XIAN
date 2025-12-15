<script setup>
import { computed, onMounted } from 'vue'
import { store } from '../store.js'
import { useRouter } from 'vue-router'
import Swal from 'sweetalert2'

const router = useRouter()

// ✅ 确保引用的是 store.cart
const cartItems = computed(() => store.cart || [])

const subTotal = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

const freight = computed(() => subTotal.value > 200 ? 0 : 20)
const total = computed(() => subTotal.value + freight.value)

// 现在的 store 已经有了这个方法
const updateQuantity = (id, delta) => {
  store.updateCartItem(id, delta)
}

const removeItem = (id) => {
  Swal.fire({
    title: '移出购物车?',
    text: "心仪的宝贝可能很快被抢光哦",
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#ef4444',
    cancelButtonColor: '#cbd5e1',
    confirmButtonText: '狠心移除',
    cancelButtonText: '再想想'
  }).then((result) => {
    if (result.isConfirmed) {
      store.removeFromCart(id)
      Swal.fire('已移除', '', 'success')
    }
  })
}

const checkout = () => {
  if (cartItems.value.length === 0) return Swal.fire('购物车是空的', '快去选购吧', 'warning')
  router.push('/checkout')
}

// 调试用
onMounted(() => {
  console.log("CartView Loaded. Items:", store.cart);
})
</script>

<template>
  <div class="min-h-screen bg-[#F8FAFC] py-12 px-4 font-sans">
    <div class="max-w-6xl mx-auto">

      <div class="flex items-center justify-between mb-8">
        <h1 class="text-3xl font-black text-slate-800 flex items-center gap-3">
          🛒 我的购物车
          <span v-if="cartItems.length > 0"
            class="text-sm font-medium bg-slate-200 text-slate-600 px-2 py-1 rounded-full">{{ cartItems.length }}</span>
        </h1>
        <button v-if="cartItems.length > 0" @click="store.clearCart"
          class="text-sm text-slate-400 hover:text-red-500 transition">清空购物车</button>
      </div>

      <div v-if="cartItems.length === 0"
        class="bg-white rounded-[32px] p-20 text-center shadow-xl shadow-slate-200/50 border border-white">
        <div class="w-40 h-40 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-6 animate-float">
          <span class="text-6xl opacity-20 grayscale">🛒</span>
        </div>
        <h2 class="text-xl font-bold text-slate-800 mb-2">购物车空空如也</h2>
        <p class="text-slate-400 mb-8">快去挑选您心仪的深海美味吧</p>
        <button @click="router.push('/')"
          class="px-10 py-3 bg-slate-900 text-white rounded-full font-bold hover:bg-indigo-600 hover:shadow-lg hover:shadow-indigo-200 transition-all transform hover:-translate-y-1">去逛逛</button>
      </div>

      <div v-else class="grid grid-cols-1 lg:grid-cols-12 gap-10 items-start">

        <div class="lg:col-span-8 space-y-6">
          <div v-for="item in cartItems" :key="item.id"
            class="group bg-white rounded-[24px] p-6 shadow-sm border border-slate-100 hover:shadow-xl hover:border-indigo-100 transition-all duration-300 relative overflow-hidden">
            <div
              class="absolute right-0 top-0 bottom-0 w-32 bg-gradient-to-l from-indigo-50/50 to-transparent opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none">
            </div>

            <div class="flex gap-6 items-center">
              <div class="relative w-24 h-24 shrink-0">
                <img :src="item.imageUrl"
                  class="w-full h-full object-cover rounded-2xl shadow-md bg-slate-50 border border-slate-100 group-hover:scale-105 transition-transform duration-500">
              </div>

              <div class="flex-1 min-w-0">
                <h3 class="font-bold text-lg text-slate-800 mb-1 truncate">{{ item.name }}</h3>
                <p class="text-xs text-slate-400 mb-4">规格: 标准装 | 配送: 顺丰冷链</p>

                <div class="flex justify-between items-center">
                  <div class="font-serif-sc font-black text-xl text-indigo-600">¥{{ item.price }}</div>

                  <div class="flex items-center bg-slate-50 rounded-xl border border-slate-200 p-1">
                    <button @click="updateQuantity(item.id, -1)"
                      class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-white hover:shadow-sm text-slate-500 transition disabled:opacity-30"
                      :disabled="item.quantity <= 1">-</button>
                    <input type="text" :value="item.quantity" readonly
                      class="w-10 text-center bg-transparent font-bold text-slate-700 text-sm outline-none">
                    <button @click="updateQuantity(item.id, 1)"
                      class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-white hover:shadow-sm text-slate-500 transition">+</button>
                  </div>
                </div>
              </div>

              <button @click="removeItem(item.id)"
                class="absolute top-4 right-4 text-slate-300 hover:text-red-500 hover:bg-red-50 p-2 rounded-xl transition opacity-0 group-hover:opacity-100">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16">
                  </path>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <div class="lg:col-span-4 sticky top-8">
          <div
            class="bg-white rounded-[32px] p-8 shadow-2xl shadow-indigo-100/50 border border-slate-100 relative overflow-hidden">
            <div class="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-indigo-500 to-purple-500"></div>

            <h2 class="font-black text-2xl text-slate-800 mb-6">订单摘要</h2>

            <div class="space-y-4 mb-8">
              <div class="flex justify-between text-slate-500 text-sm">
                <span>商品总额</span>
                <span class="font-medium text-slate-900">¥{{ subTotal.toFixed(2) }}</span>
              </div>
              <div class="flex justify-between text-slate-500 text-sm">
                <span>运费</span>
                <span :class="freight === 0 ? 'text-green-600 font-bold' : 'text-slate-900'">{{ freight === 0 ? '免运费' :
                  `+ ¥${freight}` }}</span>
              </div>
              <div v-if="freight > 0" class="text-xs text-orange-500 bg-orange-50 px-3 py-2 rounded-xl text-center">
                再买 <span class="font-bold">¥{{ (200 - subTotal).toFixed(2) }}</span> 即可免运费
              </div>
            </div>

            <div class="border-t border-dashed border-slate-200 pt-6 mb-8">
              <div class="flex justify-between items-end">
                <span class="text-slate-500 font-medium">总计</span>
                <span class="text-4xl font-black text-indigo-600 font-serif-sc">
                  <span class="text-lg align-top relative top-1">¥</span>{{ total.toFixed(2) }}
                </span>
              </div>
            </div>

            <button @click="checkout"
              class="w-full py-4 bg-slate-900 text-white rounded-2xl font-bold text-lg shadow-xl shadow-indigo-200 hover:bg-indigo-600 hover:scale-[1.02] active:scale-95 transition-all duration-300 flex items-center justify-center gap-2 group">
              去结算
              <svg class="w-5 h-5 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor"
                viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3">
                </path>
              </svg>
            </button>

            <div class="mt-6 flex justify-center gap-4 text-slate-300">
              <span class="text-xs">支持支付宝 / 微信支付</span>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-float {
  animation: float 3s ease-in-out infinite;
}

@keyframes float {

  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-10px);
  }
}
</style>