<script setup>

import { ref, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { store } from '../store.js'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const insightData = ref(null)
const relatedProducts = ref([])
const chartRef = ref(null)
const buyCount = ref(1)
const showStickyBar = ref(false)
const isAdding = ref(false)
const originalPrice = computed(() => product.value ? (product.value.price * 1.2).toFixed(2) : 0)

const stockColor = computed(() => {
  if (!product.value) return ''
  if (product.value.stock < 5) return 'text-red-500 font-bold animate-pulse'
  if (product.value.stock < 20) return 'text-orange-500 font-bold'
  return 'text-green-600'
})

const handleScroll = () => {
  showStickyBar.value = window.scrollY > 300
}

const initChart = () => {
  if (!chartRef.value || !insightData.value) return
  const myChart = echarts.init(chartRef.value)
  const dates = insightData.value.priceHistory.map(item => item.date)
  const prices = insightData.value.priceHistory.map(item => item.price)

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e2e8f0',
      textStyle: { color: '#1e293b' }
    },
    grid: { top: 10, bottom: 0, left: 0, right: 0, containLabel: false },
    xAxis: {
      type: 'category',
      data: dates,
      show: false
    },
    yAxis: {
      type: 'value',
      show: false,
      min: (value) => value.min * 0.9
    },
    series: [{
      data: prices,
      type: 'line',
      smooth: true,
      symbol: 'none',
      lineStyle: { color: '#3b82f6', width: 3 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(59, 130, 246, 0.2)' },
          { offset: 1, color: 'rgba(59, 130, 246, 0)' }
        ])
      }
    }]
  }
  myChart.setOption(option)
}

const changeCount = (delta) => {
  if (!product.value) return
  const newVal = buyCount.value + delta
  if (newVal >= 1 && newVal <= product.value.stock) {
    buyCount.value = newVal
  }
}

const addToCart = (event) => {
  if (!product.value) return
  const inCart = store.getProductCount(product.value.id)
  if (inCart + buyCount.value > product.value.stock) {
    store.showNotification('库存不足啦', 'error')
    return
  }

  isAdding.value = true
  setTimeout(() => { isAdding.value = false }, 1500)

  for (let i = 0; i < buyCount.value; i++) {
    store.addToCart(product.value, i === 0 ? event : null)
  }
  if (!event) store.showNotification(`已加购 ${buyCount.value} 件`)
  buyCount.value = 1
}

const fetchRelated = async (category, currentId) => {
  try {
    const res = await fetch(`http://localhost:8080/api/products/category/${category}`)
    const list = await res.json()
    relatedProducts.value = list.filter(p => p.id !== parseInt(currentId)).slice(0, 4)
  } catch (e) { console.error(e) }
}

onMounted(async () => {
  window.addEventListener('scroll', handleScroll)
  try {
    const id = route.params.id
    const res = await fetch(`http://localhost:8080/api/products/${id}`)
    product.value = await res.json()

    fetchRelated(product.value.category, id)

    const insightRes = await fetch(`http://localhost:8080/api/products/${id}/insight`)
    insightData.value = await insightRes.json()

    nextTick(() => { initChart() })
  } catch (error) { console.error('加载失败', error) }
})

const getUnit = (name) => {
  if (name.includes('蟹') || name.includes('龙虾')) return '只'
  if (name.includes('鲍鱼') || name.includes('生蚝') || name.includes('扇贝')) return '只'
  if (name.includes('多宝鱼') || name.includes('石斑鱼')) return '条'
  return '500g'
}

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

watch(() => route.params.id, () => { window.location.reload() })
</script>

<template>
  <div v-if="product" class="container mx-auto px-4 py-8">

    <Transition name="slide-down">
      <div v-if="showStickyBar"
        class="fixed top-0 left-0 right-0 z-[60] bg-white/90 backdrop-blur-md border-b border-slate-200/50 shadow-lg py-3 px-4">
        <div class="container mx-auto max-w-6xl flex items-center justify-between">
          <div class="flex items-center gap-4">
            <img :src="product.imageUrl" class="w-10 h-10 rounded-lg object-cover border border-slate-200" />
            <div class="hidden md:block">
              <h4 class="font-bold text-slate-800 text-sm">{{ product.name }}</h4>
              <span class="text-xs text-blue-600 font-bold">¥{{ product.price }}/{{ getUnit(product.name) }}</span>
            </div>
          </div>
          <div class="flex items-center gap-4">
            <div class="flex items-center bg-slate-100 rounded-lg h-9"
              :class="{ 'opacity-50 pointer-events-none': product.stock <= 0 }">
              <button @click="changeCount(-1)"
                class="w-8 h-full flex items-center justify-center text-slate-500 hover:text-blue-600 disabled:opacity-30"
                :disabled="buyCount <= 1">-</button>
              <span class="text-xs font-bold w-6 text-center">{{ product.stock > 0 ? buyCount : 0 }}</span>
              <button @click="changeCount(1)"
                class="w-8 h-full flex items-center justify-center text-slate-500 hover:text-blue-600 disabled:opacity-30"
                :disabled="buyCount >= product.stock">+</button>
            </div>

            <button @click="(e) => addToCart(e)" :disabled="product.stock <= 0"
              class="px-6 py-2 rounded-full text-sm font-bold shadow-md transition active:scale-95"
              :class="product.stock > 0 ? 'bg-blue-900 text-white hover:bg-blue-800' : 'bg-slate-300 text-slate-500 cursor-not-allowed'">
              {{ product.stock > 0 ? '立即加购' : '缺货' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <button @click="router.back()"
      class="mb-6 flex items-center gap-2 text-slate-500 hover:text-blue-900 transition group">
      <img src="/icons/icon-back.png" class="w-5 h-5 opacity-60 group-hover:opacity-100 transition" />
      <span class="font-medium">返回列表</span>
    </button>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-12">
      <div class="space-y-8">
        <div class="aspect-[4/3] bg-slate-100 rounded-2xl overflow-hidden relative shadow-lg">
          <div class="absolute top-4 left-4 z-10 bg-blue-600 text-white px-3 py-1 text-xs font-bold rounded shadow-md">
            {{ product.origin }}直采
          </div>
          <img v-if="product.imageUrl" :src="product.imageUrl" class="w-full h-full object-cover" />
          <div v-else class="w-full h-full flex items-center justify-center bg-blue-50 text-blue-200"><span
              class="text-9xl">🐟</span></div>
        </div>
        <div class="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-bold text-slate-800">行情指数</h3>
            <span class="text-xs bg-red-50 text-red-600 px-2 py-0.5 rounded border border-red-100">今日波动实况</span>
          </div>
          <div ref="chartRef" class="w-full h-48"></div>
        </div>
      </div>

      <div class="flex flex-col">
        <h1 class="text-4xl font-serif-sc font-bold text-slate-900 mb-2">{{ product.name }}</h1>
        <div class="flex items-center gap-2 mb-4">
          <div class="h-2 w-24 bg-slate-100 rounded-full overflow-hidden">
            <div class="h-full rounded-full transition-all duration-500"
              :style="`width: ${Math.min(product.stock * 5, 100)}%`"
              :class="product.stock < 5 ? 'bg-red-500' : 'bg-green-500'"></div>
          </div>
          <span class="text-xs font-bold" :class="product.stock < 5 ? 'text-red-500' : 'text-slate-400'">仅剩 {{
            product.stock }} 件</span>
        </div>
        <p class="text-lg text-slate-600 mb-8 leading-relaxed">{{ product.description }}</p>

        <div class="bg-blue-50/50 p-6 rounded-xl border border-blue-100 mb-8">
          <div class="flex justify-between items-end mb-6">
            <div>
              <span class="block text-sm text-slate-500 mb-1">今日实时价</span>
              <span class="text-4xl font-bold text-blue-900 font-serif-sc">
                ¥{{ product.price }}<span class="text-lg font-normal text-slate-500 ml-1">/{{ getUnit(product.name)
                  }}</span>
              </span>
            </div>

            <div class="flex items-center bg-white border border-slate-200 rounded-lg shadow-sm"
              :class="{ 'opacity-50 pointer-events-none': product.stock <= 0 }">
              <button @click="changeCount(-1)"
                class="w-10 h-10 flex items-center justify-center text-slate-500 hover:bg-slate-50 disabled:opacity-30 transition"
                :disabled="buyCount <= 1">-</button>
              <input type="text" readonly :value="product.stock > 0 ? buyCount : 0"
                class="w-10 text-center font-bold text-slate-800 focus:outline-none bg-transparent" />
              <button @click="changeCount(1)"
                class="w-10 h-10 flex items-center justify-center text-slate-500 hover:bg-slate-50 disabled:opacity-30 transition"
                :disabled="buyCount >= product.stock">+</button>
            </div>
          </div>

          <button @click="(e) => addToCart(e)" :disabled="product.stock <= 0"
            class="w-full py-4 rounded-xl font-bold shadow-lg transition transform active:scale-95 flex items-center justify-center gap-2"
            :class="product.stock > 0
              ? 'bg-blue-900 text-white shadow-blue-900/20 hover:bg-blue-800'
              : 'bg-slate-300 text-slate-500 cursor-not-allowed shadow-none'">
            <img v-if="product.stock > 0" src="/icons/icon-cart.png" class="w-5 h-5 brightness-200" />
            <span>{{ product.stock > 0 ? '立即加购' : '暂时缺货' }}</span>
          </button>
        </div>

        <div v-if="insightData" class="flex-1">
          <div class="flex items-center justify-between mb-8">
            <h3 class="font-bold text-slate-900 text-lg">全链路溯源档案</h3>
            <div class="flex items-center gap-1.5 px-2 py-1 bg-green-50 rounded-md border border-green-100">
              <div class="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></div>
              <span class="text-[10px] font-bold text-green-700 tracking-wider">BLOCKCHAIN VERIFIED</span>
            </div>
          </div>

          <div class="relative space-y-8 ml-2">
            <div class="absolute left-3 top-2 bottom-2 w-0.5 bg-slate-100"></div>

            <div v-for="(event, index) in insightData.traceEvents" :key="index" class="relative pl-10 group">
              <div
                class="absolute left-0 top-0 w-6 h-6 rounded-full border-4 border-white shadow-sm flex items-center justify-center z-10 transition-colors duration-300"
                :class="index === insightData.traceEvents.length - 1 ? 'bg-blue-600' : 'bg-slate-200 group-hover:bg-blue-400'">
              </div>

              <div
                class="bg-slate-50 rounded-xl p-4 border border-slate-100 transition-all hover:bg-white hover:shadow-md hover:border-blue-100">
                <div class="flex justify-between items-start mb-1">
                  <h4 class="font-bold text-slate-800 text-sm">{{ event.title }}</h4>
                  <span
                    class="text-xs font-mono text-slate-400 bg-white px-1.5 py-0.5 rounded border border-slate-100">{{
                      event.time.split(' ')[1] }}</span>
                </div>
                <p class="text-xs text-slate-500 leading-relaxed">{{ event.desc }}</p>
                <div class="mt-2 text-[10px] text-slate-400">{{ event.time.split(' ')[0] }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <hr class="my-12 border-slate-100">
    <div v-if="relatedProducts.length > 0">
      <h3 class="text-2xl font-serif-sc font-bold text-slate-800 mb-6">同类甄选</h3>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div v-for="item in relatedProducts" :key="item.id" @click="router.push(`/product/${item.id}`)"
          class="group cursor-pointer bg-white border border-slate-100 rounded-xl overflow-hidden hover:shadow-lg transition-all">
          <div class="h-32 w-full bg-slate-50 relative overflow-hidden">
            <img v-if="item.imageUrl" :src="item.imageUrl"
              class="w-full h-full object-cover group-hover:scale-105 transition duration-500" />
            <div v-else class="w-full h-full flex items-center justify-center text-2xl">🐟</div>
          </div>
          <div class="p-3">
            <h4 class="font-bold text-slate-800 text-sm truncate group-hover:text-blue-700">{{ item.name }}</h4>
            <div class="flex items-center justify-between mt-2">
              <span class="text-blue-900 font-bold text-sm">¥{{ item.price }}</span>
              <button
                class="w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center text-blue-900 hover:bg-blue-600 hover:text-white transition">
                <img src="/icons/icon-add.png" class="w-3 h-3 group-hover:brightness-200 transition" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>