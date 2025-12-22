<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '../store.js'
import { request } from '@/utils/request'
import QuickViewModal from '../components/QuickViewModal.vue'

const router = useRouter()
const recommendations = ref([])
const showModal = ref(false)
const selectedProduct = ref(null)

const selectedIds = ref(new Set())

const initSelection = () => {
  store.cart.forEach(item => selectedIds.value.add(item.id))
}

const toggleSelection = (id) => {
  if (selectedIds.value.has(id)) {
    selectedIds.value.delete(id)
  } else {
    selectedIds.value.add(id)
  }
}

const toggleAll = () => {
  if (selectedIds.value.size === store.cart.length) {
    selectedIds.value.clear()
  } else {
    store.cart.forEach(item => selectedIds.value.add(item.id))
  }
}

const isAllSelected = computed(() => {
  return store.cart.length > 0 && selectedIds.value.size === store.cart.length
})

const selectedItems = computed(() => {
  return store.cart.filter(item => selectedIds.value.has(item.id))
})

const itemsTotal = computed(() => {
  return selectedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

const shippingFee = computed(() => {

  return itemsTotal.value >= 200 ? 0 : 20
})

const finalTotal = computed(() => {
  return itemsTotal.value + shippingFee.value
})

const FREE_SHIPPING_THRESHOLD = 200
const progressPercentage = computed(() => {
  const percent = (itemsTotal.value / FREE_SHIPPING_THRESHOLD) * 100
  return Math.min(percent, 100)
})

const diffForFreeShipping = computed(() => {
  return Math.max(0, FREE_SHIPPING_THRESHOLD - itemsTotal.value).toFixed(2)
})

const updateQuantity = (item, change) => {
  const newQty = item.quantity + change
  if (newQty > 0) {
    store.updateCartItem(item.id, newQty)
  }
}

const removeItem = (id) => {
  store.removeFromCart(id)
  selectedIds.value.delete(id)
}

const checkout = () => {
  if (selectedItems.value.length === 0) {
    store.showNotification('请至少选择一件商品', 'warning')
    return
  }

  router.push('/checkout')
}

const fetchRecommendations = async () => {
  try {
    const data = await request('/api/products/recommend')
    recommendations.value = data.slice(0, 4)
  } catch (e) { console.error(e) }
}

const goToDetail = (id) => router.push(`/product/${id}`)
const addToCart = (p, e) => { e.stopPropagation(); store.addToCart(p, e) }

onMounted(() => {
  initSelection()
  fetchRecommendations()
})
</script>

<template>
  <div class="min-h-screen bg-[#F8FAFC] pb-20 relative overflow-hidden">

    <div
      class="absolute top-0 left-0 w-full h-[500px] bg-gradient-to-b from-blue-50/80 to-transparent pointer-events-none">
    </div>
    <div class="absolute -top-20 -right-20 w-96 h-96 bg-blue-200/30 rounded-full blur-3xl animate-pulse"></div>
    <div class="absolute top-40 -left-20 w-72 h-72 bg-purple-200/30 rounded-full blur-3xl animate-pulse delay-1000">
    </div>

    <div class="container mx-auto px-4 py-8 relative z-10">

      <div class="flex items-center gap-3 mb-8">
        <h1 class="text-2xl font-bold text-slate-800">我的购物车</h1>
        <span class="px-2 py-0.5 bg-blue-100 text-blue-600 text-xs font-bold rounded-full">
          {{ store.cart.length }} 件商品
        </span>
      </div>

      <div class="flex flex-col lg:flex-row gap-8">

        <div class="flex-1 space-y-4">

          <div v-if="store.cart.length > 0"
            class="bg-white/80 backdrop-blur-xl rounded-2xl p-4 flex items-center gap-3 shadow-sm border border-slate-200/60">
            <div @click="toggleAll"
              class="cursor-pointer flex items-center gap-2 text-sm font-bold text-slate-600 hover:text-blue-600 transition">
              <div class="w-5 h-5 rounded-full border-2 flex items-center justify-center transition-colors"
                :class="isAllSelected ? 'bg-blue-600 border-blue-600' : 'border-slate-300 bg-white'">
                <svg v-if="isAllSelected" class="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24"
                  stroke="currentColor" stroke-width="3">
                  <path d="M5 13l4 4L19 7" />
                </svg>
              </div>
              全选
            </div>
          </div>

          <div v-if="store.cart.length === 0"
            class="bg-white/60 backdrop-blur-md rounded-3xl p-16 text-center border border-slate-100 border-dashed">
            <div class="text-6xl mb-4 opacity-20"></div>
            <h3 class="text-lg font-bold text-slate-600 mb-2">购物车还是空的</h3>
            <p class="text-slate-400 text-sm mb-6">快去挑选深海美味吧</p>
            <button @click="router.push('/')"
              class="px-8 py-2.5 bg-slate-900 text-white rounded-full font-bold hover:bg-blue-600 transition shadow-lg">
              去逛逛
            </button>
          </div>

          <div v-else class="space-y-4">
            <div v-for="item in store.cart" :key="item.id"
              class="group bg-white/80 backdrop-blur-xl rounded-2xl p-4 border border-slate-200/60 shadow-sm hover:shadow-lg transition-all duration-300 flex items-center gap-4">

              <div @click="toggleSelection(item.id)" class="cursor-pointer p-2 -ml-2">
                <div class="w-5 h-5 rounded-full border-2 flex items-center justify-center transition-colors"
                  :class="selectedIds.has(item.id) ? 'bg-blue-600 border-blue-600' : 'border-slate-300 bg-white group-hover:border-blue-400'">
                  <svg v-if="selectedIds.has(item.id)" class="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24"
                    stroke="currentColor" stroke-width="3">
                    <path d="M5 13l4 4L19 7" />
                  </svg>
                </div>
              </div>

              <div @click="goToDetail(item.id)"
                class="w-24 h-24 bg-slate-100 rounded-xl overflow-hidden cursor-pointer border border-slate-100 flex-shrink-0">
                <img :src="item.imageUrl"
                  class="w-full h-full object-cover group-hover:scale-110 transition duration-500" />
              </div>

              <div class="flex-1 min-w-0">
                <div class="flex justify-between items-start">
                  <div>
                    <h3 @click="goToDetail(item.id)"
                      class="font-bold text-slate-800 cursor-pointer hover:text-blue-600 transition truncate pr-4">
                      {{ item.name }}
                    </h3>
                    <p class="text-xs text-slate-400 mt-1">规格: 标准装 | 配送: 顺丰冷链</p>
                  </div>
                  <button @click="removeItem(item.id)"
                    class="text-slate-300 hover:text-red-500 transition p-1 opacity-0 group-hover:opacity-100">
                    <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24"
                      stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>

                <div class="flex items-center justify-between mt-4">
                  <div class="text-blue-600 font-bold text-lg">¥{{ item.price }}</div>

                  <div class="flex items-center bg-slate-50 rounded-lg border border-slate-200">
                    <button @click="updateQuantity(item, -1)"
                      class="w-8 h-8 flex items-center justify-center text-slate-500 hover:text-blue-600 disabled:opacity-50"
                      :disabled="item.quantity <= 1">-</button>
                    <span class="w-8 text-center text-sm font-bold text-slate-800">{{ item.quantity }}</span>
                    <button @click="updateQuantity(item, 1)"
                      class="w-8 h-8 flex items-center justify-center text-slate-500 hover:text-blue-600">+</button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="recommendations.length > 0" class="mt-12">
            <div class="flex items-center gap-2 mb-4">
              <h3 class="font-bold text-slate-800">猜你喜欢</h3>
            </div>
            <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <div v-for="rec in recommendations" :key="rec.id" @click="goToDetail(rec.id)"
                class="group bg-white rounded-xl p-3 border border-slate-100 hover:shadow-lg hover:-translate-y-1 transition-all cursor-pointer">
                <div class="aspect-square bg-slate-50 rounded-lg overflow-hidden mb-2">
                  <img :src="rec.imageUrl"
                    class="w-full h-full object-cover group-hover:scale-105 transition duration-500" />
                </div>
                <h4 class="text-xs font-bold text-slate-800 truncate">{{ rec.name }}</h4>
                <div class="flex justify-between items-center mt-1">
                  <span class="text-xs font-bold text-blue-600">¥{{ rec.price }}</span>
                  <button @click="(e) => addToCart(rec, e)"
                    class="w-6 h-6 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center hover:bg-blue-600 hover:text-white transition">
                    <span class="text-xs">+</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="lg:w-96 flex-shrink-0">
          <div class="sticky top-24 space-y-4">

            <div
              class="bg-gradient-to-br from-blue-600 to-blue-800 rounded-2xl p-5 text-white shadow-xl shadow-blue-900/20 relative overflow-hidden">
              <div class="absolute -right-6 -top-6 w-24 h-24 bg-white/10 rounded-full blur-2xl"></div>

              <div v-if="itemsTotal >= FREE_SHIPPING_THRESHOLD" class="flex items-center gap-2 mb-3">
                <span class="text-2xl">🎉</span>
                <div class="font-bold">已享免运费</div>
              </div>
              <div v-else class="mb-3">
                <div class="text-sm text-blue-100 mb-1">再买 <span class="font-bold text-white text-lg">¥{{
                  diffForFreeShipping }}</span> 免运费</div>
                <div class="text-xs text-blue-200 opacity-80">去凑单更划算 ></div>
              </div>

              <div class="h-2 bg-black/20 rounded-full overflow-hidden">
                <div
                  class="h-full bg-white/90 shadow-[0_0_10px_rgba(255,255,255,0.5)] transition-all duration-1000 ease-out"
                  :style="{ width: `${progressPercentage}%` }"></div>
              </div>
            </div>

            <div class="bg-white/80 backdrop-blur-xl rounded-2xl p-6 border border-slate-200/60 shadow-lg">
              <h3 class="font-bold text-slate-800 mb-6">订单摘要</h3>

              <div class="space-y-3 mb-6">
                <div class="flex justify-between text-sm text-slate-500">
                  <span>商品总额</span>
                  <span class="font-medium text-slate-800">¥{{ itemsTotal.toFixed(2) }}</span>
                </div>
                <div class="flex justify-between text-sm text-slate-500">
                  <span>运费</span>
                  <span v-if="shippingFee === 0" class="text-green-600 font-bold">免运费</span>
                  <span v-else class="font-medium text-slate-800">+ ¥{{ shippingFee }}</span>
                </div>
                <div class="flex justify-between text-sm text-slate-500">
                  <span>优惠券</span>
                  <span class="text-slate-400">无可用</span>
                </div>
              </div>

              <div class="border-t border-slate-100 pt-4 mb-6">
                <div class="flex justify-between items-end">
                  <span class="text-slate-600 font-bold">总计</span>
                  <span class="text-3xl font-bold text-slate-900 tracking-tight">
                    <span class="text-base align-top mt-2 inline-block">¥</span>{{ finalTotal.toFixed(2) }}
                  </span>
                </div>
                <div class="text-right text-xs text-slate-400 mt-1">已包含增值税</div>
              </div>

              <button @click="checkout"
                class="w-full py-4 bg-slate-900 text-white rounded-xl font-bold text-lg hover:bg-blue-600 hover:shadow-lg hover:shadow-blue-500/30 active:scale-[0.98] transition-all duration-300 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                :disabled="selectedItems.length === 0">
                去结算
                <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24"
                  stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                </svg>
              </button>
            </div>
          </div>
        </div>

      </div>
    </div>

    <QuickViewModal :is-open="showModal" :product="selectedProduct" @close="showModal = false" />
  </div>
</template>