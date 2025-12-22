<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { store } from '../store.js'
import * as echarts from 'echarts'
import { request } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const insightData = ref(null)
const relatedProducts = ref([])
const chartRef = ref(null)
const buyCount = ref(1)
const isAdding = ref(false)
const mapInstance = ref(null)

// --- AI 助手逻辑 (升级版) ---
const showAiChat = ref(false)
const aiLoading = ref(false)
const chatHistory = ref([]) // 初始为空，由逻辑控制欢迎语
const suggestedQuestions = ref(['最适合的做法是什么？', '处理时要注意什么？', '哪怕是清蒸需要放什么佐料？', '配什么酒最好？'])

// 问候语逻辑
const greetingText = ref('')
const showGreetingBubble = ref(false)
const fullGreeting = "您好！我是您的小助理，请问有什么可以帮您的吗？"

const startGreetingTypewriter = async () => {
  // 延迟 800ms 开始，让页面先渲染完
  await new Promise(r => setTimeout(r, 800))
  showGreetingBubble.value = true

  for (let i = 0; i < fullGreeting.length; i++) {
    greetingText.value += fullGreeting[i]
    // 打字速度
    await new Promise(r => setTimeout(r, 50))
  }

  // 自动收起逻辑 (可选：10秒后自动淡出，保持页面清爽)
  // setTimeout(() => { showGreetingBubble.value = false }, 10000)
}

const toggleChat = () => {
  showAiChat.value = !showAiChat.value
  // 打开聊天框时，隐藏外部的气泡
  if (showAiChat.value) {
    showGreetingBubble.value = false
    // 如果没有历史消息，AI 先说话
    if (chatHistory.value.length === 0) {
      chatHistory.value.push({ role: 'ai', content: fullGreeting })
    }
  }
}

// 发送消息
const askAi = async (question) => {
  if (!question) return
  if (!product.value) return

  chatHistory.value.push({ role: 'user', content: question })
  aiLoading.value = true

  try {
    const res = await request.post('/api/ai/ask', {
      productName: product.value.name,
      question: question
    })
    chatHistory.value.push({ role: 'ai', content: res.answer })
    nextTick(() => {
      const chatBox = document.getElementById('chat-box-container')
      if (chatBox) chatBox.scrollTop = chatBox.scrollHeight
    })
  } catch (e) {
    console.error(e)
    chatHistory.value.push({ role: 'ai', content: '网络信号不佳，请稍后重试...' })
  } finally {
    aiLoading.value = false
  }
}
// --- AI 逻辑结束 ---

// --- 原有地图与图表逻辑 (保持不变) ---
const SVG_PIN = `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="48" height="48" fill="none">
  <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" fill="#0f172a" stroke="white" stroke-width="2"/>
  <circle cx="12" cy="9" r="3" fill="white"/>
</svg>`

const initMap = () => {
  if (!insightData.value || !insightData.value.trajectory || !window.AMap) return
  const traj = insightData.value.trajectory
  const endPoint = traj[traj.length - 1]
  const map = new window.AMap.Map('traceMap', {
    zoom: 6, center: endPoint, viewMode: '3D', pitch: 20,
    mapStyle: 'amap://styles/whitesmoke', dragEnable: false, zoomEnable: false, showLabel: false
  })
  mapInstance.value = map
  const polyline = new window.AMap.Polyline({
    path: traj, isOutline: true, outlineColor: '#ffffff', borderWeight: 2,
    strokeColor: "#2563eb", strokeOpacity: 1, strokeWeight: 5, strokeStyle: "dashed", strokeDasharray: [15, 10], lineJoin: 'round'
  })
  map.add(polyline)
  // ... (Marker logic omitted for brevity, keeping same logic)
  const createLabel = (title, sub) => `
    <div class="flex flex-col items-center transform transition-transform hover:scale-110" style="pointer-events: none;">
      <div class="bg-slate-900/90 backdrop-blur text-white px-3 py-1.5 rounded-lg shadow-xl mb-1 flex flex-col items-center border border-slate-700/50">
        <span class="text-xs font-bold whitespace-nowrap">${title}</span>
        <span class="text-[10px] text-slate-300 uppercase tracking-wider">${sub}</span>
      </div>
      <div class="w-0 h-0 border-l-[6px] border-l-transparent border-r-[6px] border-r-transparent border-t-[6px] border-t-slate-900/90 mb-1"></div>
      <div class="relative top-[-4px] drop-shadow-md">${SVG_PIN}</div>
    </div>`
  const startMarker = new window.AMap.Marker({ position: traj[0], content: createLabel('捕捞海域', 'Origin'), offset: new window.AMap.Pixel(-24, -85), zIndex: 10 })
  const endMarker = new window.AMap.Marker({ position: endPoint, content: createLabel(`${product.value.origin}港`, 'Port'), offset: new window.AMap.Pixel(-24, -85), zIndex: 100 })
  map.add([startMarker, endMarker])
  map.setFitView(null, false, [80, 60, 60, 60])
}

const initChart = () => {
  if (!chartRef.value || !insightData.value) return
  const myChart = echarts.init(chartRef.value)
  const dates = insightData.value.priceHistory.map(item => item.date)
  const prices = insightData.value.priceHistory.map(item => item.price)
  const minPrice = Math.min(...prices)
  myChart.setOption({
    grid: { top: 30, bottom: 0, left: 0, right: 0 },
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255, 255, 255, 0.95)', borderColor: '#e2e8f0', textStyle: { color: '#0f172a', fontSize: 12 }, formatter: '{b} <br/> <span style="font-weight:bold;color:#2563eb">¥{c}</span>' },
    xAxis: { type: 'category', data: dates, axisTick: { show: false }, axisLine: { show: false }, axisLabel: { show: false } },
    yAxis: { type: 'value', min: Math.floor(minPrice * 0.9), splitLine: { show: false }, axisLabel: { show: false } },
    series: [{ data: prices, type: 'line', smooth: 0.6, symbol: 'none', symbolSize: 8, itemStyle: { color: '#2563eb', borderWidth: 2, borderColor: '#fff' }, lineStyle: { width: 0 }, areaStyle: { opacity: 0.8, color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#3b82f6' }, { offset: 1, color: '#93c5fd' }]) }, markPoint: { data: [{ type: 'max', name: 'Max', itemStyle: { color: '#ef4444' } }], label: { color: '#fff', fontSize: 10 } } }]
  })
}

const changeCount = (delta) => {
  if (!product.value) return
  const newVal = buyCount.value + delta
  if (newVal > product.value.stock) { store.showNotification(`手慢了！仅剩 ${product.value.stock} 件`, 'warning'); buyCount.value = product.value.stock; return }
  if (newVal < 1) { buyCount.value = 1; return }
  buyCount.value = newVal
}
const addToCart = async (event) => {
  if (!product.value) return
  const inCartCount = store.getProductCount(product.value.id)
  const totalDemand = inCartCount + buyCount.value
  if (totalDemand > product.value.stock) {
    const canBuy = product.value.stock - inCartCount
    if (canBuy > 0) { store.showNotification(`库存紧张，您还能再买 ${canBuy} 件`, 'error'); buyCount.value = canBuy } else { store.showNotification('您购物车里的数量已达库存上限！', 'error') }
    return
  }
  isAdding.value = true
  await new Promise(r => setTimeout(r, 600))
  for (let i = 0; i < buyCount.value; i++) { store.addToCart(product.value, i === 0 ? event : null) }
  isAdding.value = false; if (!event) store.showNotification(`成功加购 ${buyCount.value} 件`); buyCount.value = 1
}

const fetchRelated = async (category, currentId) => {
  const list = await request(`/api/products/category/${category}`)
  relatedProducts.value = list.filter(p => p.id !== parseInt(currentId)).slice(0, 4)
}

const getUnit = (name) => (name.includes('蟹') || name.includes('龙虾') || name.includes('鲍鱼')) ? '只' : '500g'

onMounted(async () => {
  try {
    const id = route.params.id
    product.value = await request(`/api/products/${id}`)
    fetchRelated(product.value.category, id)
    insightData.value = await request(`/api/products/${id}/insight`)
    nextTick(() => { initChart(); initMap() })

    // 启动 Siri 问候逻辑
    startGreetingTypewriter()

  } catch (error) { console.error(error) }
})

onUnmounted(() => { if (mapInstance.value) mapInstance.value.destroy() })
watch(() => route.params.id, () => { window.location.reload() })
</script>

<template>
  <div v-if="product" class="min-h-screen bg-[#F8FAFC] font-sans text-slate-800 pb-24 relative overflow-hidden">

    <nav
      class="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-slate-200/60 transition-all duration-300">
      <div class="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        <button @click="router.back()" class="flex items-center gap-2 text-slate-500 hover:text-slate-900 transition">
          <span class="text-xl">←</span> <span class="text-sm font-medium">返回</span>
        </button>
        <span class="font-serif-sc font-bold text-slate-900">{{ product.name }}</span>
        <div class="w-10"></div>
      </div>
    </nav>

    <main class="max-w-7xl mx-auto px-6 py-10 relative z-10">
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-12 mb-16">
        <div class="lg:col-span-7 space-y-6">
          <div
            class="aspect-[4/3] bg-white rounded-[2rem] overflow-hidden shadow-xl shadow-slate-200/50 border border-white relative group">
            <div class="absolute top-6 left-6 z-10 flex gap-2">
              <span
                class="bg-black/80 text-white text-xs px-3 py-1.5 rounded-full backdrop-blur-md font-bold tracking-wide shadow-lg">
                {{ product.origin }} · ORIGIN
              </span>
            </div>
            <img :src="product.imageUrl"
              class="w-full h-full object-cover transition duration-1000 group-hover:scale-105" />
          </div>

          <div class="grid grid-cols-3 gap-4">
            <div class="stat-card">
              <div class="stat-label">WEIGHT</div>
              <div class="stat-value">约 {{ getUnit(product.name) === '只' ? '400-600g' : '500g' }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">STORAGE</div>
              <div class="stat-value">-18°C 冷冻</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">LOGISTICS</div>
              <div class="stat-value">顺丰冷链</div>
            </div>
          </div>
        </div>

        <div class="lg:col-span-5 flex flex-col gap-8">
          <div>
            <h1 class="text-5xl font-serif-sc font-bold text-slate-900 mb-4 leading-tight">{{ product.name }}</h1>
            <p class="text-slate-500 text-sm leading-relaxed mb-8">{{ product.description }}</p>

            <div class="flex items-baseline gap-1 pb-4 border-b border-slate-100">
              <span class="text-3xl font-bold text-slate-900">¥</span>
              <span class="text-6xl font-serif-sc font-bold text-slate-900 tracking-tighter">{{ product.price }}</span>
              <span class="text-lg text-slate-400 font-normal">/{{ getUnit(product.name) }}</span>
            </div>

            <div class="flex items-center gap-2 mt-4">
              <div class="h-2 flex-1 bg-slate-100 rounded-full overflow-hidden max-w-[200px]">
                <div class="h-full bg-slate-800 rounded-full transition-all duration-500"
                  :style="`width: ${Math.min(product.stock, 100)}%`"></div>
              </div>
              <span class="text-xs font-bold" :class="product.stock < 10 ? 'text-red-500' : 'text-slate-500'">
                库存: {{ product.stock }} {{ getUnit(product.name) }}
              </span>
            </div>
          </div>

          <div v-if="insightData" class="bg-white rounded-2xl p-5 border border-slate-100 shadow-price-card">
            <div class="flex justify-between items-center mb-2">
              <div>
                <div class="text-xs font-bold text-slate-900 uppercase tracking-wider">Price Trend</div>
                <div class="text-[10px] text-slate-400">近7日产地收购行情</div>
              </div>
              <span class="flex h-2 w-2 relative">
                <span
                  class="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
                <span class="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
              </span>
            </div>
            <div ref="chartRef" class="w-full h-40"></div>
          </div>

          <div class="space-y-4 pt-4">
            <div class="flex items-center justify-between">
              <span class="text-sm font-bold text-slate-900">选择数量</span>
              <div class="flex items-center gap-4 bg-white px-4 py-2 rounded-full border border-slate-200 shadow-sm">
                <button @click="changeCount(-1)" class="text-slate-400 hover:text-slate-900 px-2"
                  :disabled="buyCount <= 1">-</button>
                <span class="w-6 text-center font-bold text-slate-900">{{ buyCount }}</span>
                <button @click="changeCount(1)" class="text-slate-400 hover:text-slate-900 px-2"
                  :disabled="buyCount >= product.stock">+</button>
              </div>
            </div>

            <button @click="(e) => addToCart(e)" :disabled="product.stock <= 0"
              class="w-full py-4 bg-slate-900 text-white rounded-2xl font-bold text-lg shadow-xl shadow-slate-900/20 hover:bg-blue-900 hover:shadow-2xl hover:shadow-blue-900/20 hover:-translate-y-0.5 active:scale-95 active:bg-slate-800 transition-all duration-200 ease-out disabled:bg-slate-300 disabled:cursor-not-allowed disabled:shadow-none disabled:translate-y-0 flex items-center justify-center gap-2">
              <span v-if="isAdding"
                class="animate-spin w-5 h-5 border-2 border-white/30 border-t-white rounded-full"></span>
              <span>{{ product.stock > 0 ? '立即购买' : '暂时缺货' }}</span>
            </button>
          </div>
        </div>
      </div>

      <div v-if="insightData" class="mb-20">
        <div class="flex items-center gap-3 mb-6">
          <div class="w-1.5 h-6 bg-blue-600 rounded-full"></div>
          <h2 class="text-2xl font-serif-sc font-bold text-slate-900">全链路数字档案</h2>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div
            class="md:col-span-2 bg-white rounded-[2rem] border border-slate-200 shadow-sm overflow-hidden relative h-[500px]">
            <div
              class="absolute top-5 left-5 z-10 bg-white/90 backdrop-blur px-4 py-2 rounded-xl shadow-sm border border-slate-100 flex flex-col">
              <span class="text-[10px] text-slate-400 uppercase tracking-wider font-bold">Trace ID</span>
              <span class="font-mono text-xs font-bold text-slate-700">{{ insightData.blockchainHash.substring(0, 18)
                }}...</span>
            </div>
            <div id="traceMap" class="w-full h-full"></div>
          </div>
          <div class="flex flex-col gap-6 h-[500px]">
            <div
              class="bg-slate-900 rounded-[2rem] p-6 text-white relative overflow-hidden shadow-xl shadow-slate-900/10 flex-shrink-0">
              <div class="absolute top-0 right-0 w-40 h-40 bg-blue-500 rounded-full blur-[80px] opacity-30"></div>
              <h3 class="text-xs font-bold text-slate-400 uppercase mb-6 relative z-10">实时环境</h3>
              <div class="space-y-5 relative z-10">
                <div class="flex justify-between items-center border-b border-white/10 pb-3">
                  <span class="text-sm text-slate-300">海水温度</span><span
                    class="font-mono text-xl font-bold text-blue-200">{{ insightData.environment?.waterTemp }}°C</span>
                </div>
                <div class="flex justify-between items-center border-b border-white/10 pb-3">
                  <span class="text-sm text-slate-300">海水盐度</span><span
                    class="font-mono text-xl font-bold text-teal-200">{{ insightData.environment?.salinity }}%</span>
                </div>
                <div class="flex justify-between items-center">
                  <span class="text-sm text-slate-300">作业风力</span><span
                    class="font-mono text-xl font-bold text-orange-200">{{ insightData.environment?.windSpeed }}级</span>
                </div>
              </div>
            </div>
            <div
              class="flex-1 bg-white rounded-[2rem] p-6 border border-slate-200 shadow-sm overflow-hidden flex flex-col">
              <h3 class="text-xs font-bold text-slate-400 uppercase mb-4 flex-shrink-0">Logistics Timeline</h3>
              <div class="overflow-y-auto pr-2 no-scrollbar flex-1">
                <div class="relative pl-8 space-y-8">
                  <div class="absolute left-[11px] top-2 bottom-2 w-0.5 bg-slate-100"></div>
                  <div v-for="(event, index) in insightData.traceEvents" :key="index" class="relative z-10">
                    <div
                      class="absolute -left-[27px] top-1.5 w-4 h-4 bg-white border-2 rounded-full transition-colors duration-300"
                      :class="index === 0 ? 'border-blue-500 shadow-[0_0_0_3px_rgba(59,130,246,0.2)]' : 'border-slate-300'">
                    </div>
                    <div>
                      <h4 class="font-bold text-slate-800 text-sm mb-1">{{ event.title }}</h4>
                      <p
                        class="text-xs text-slate-500 leading-relaxed bg-slate-50 p-2 rounded-lg border border-slate-100 mb-1">
                        {{ event.desc }}</p>
                      <span class="text-[10px] font-mono text-slate-400 block">{{ event.time }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <div class="fixed bottom-10 right-8 z-50 flex flex-col items-end gap-6 pointer-events-none">

      <transition enter-active-class="transform transition duration-500 ease-out"
        enter-from-class="opacity-0 translate-y-4 scale-90" enter-to-class="opacity-100 translate-y-0 scale-100"
        leave-active-class="transform transition duration-300 ease-in" leave-from-class="opacity-100 scale-100"
        leave-to-class="opacity-0 scale-90">
        <div v-if="showGreetingBubble"
          class="pointer-events-auto bg-white/80 backdrop-blur-xl border border-white/50 shadow-[0_8px_32px_rgba(31,38,135,0.15)] rounded-2xl p-4 max-w-[280px] relative mb-2">
          <div class="text-sm font-medium text-slate-800 leading-relaxed tracking-wide">
            {{ greetingText }}<span class="animate-pulse">|</span>
          </div>
          <div
            class="absolute -bottom-2 right-8 w-4 h-4 bg-white/80 border-b border-r border-white/50 transform rotate-45">
          </div>
        </div>
      </transition>

      <transition enter-active-class="transform transition duration-500 cubic-bezier(0.34, 1.56, 0.64, 1)"
        enter-from-class="opacity-0 translate-y-20 scale-90 blur-sm"
        enter-to-class="opacity-100 translate-y-0 scale-100 blur-0"
        leave-active-class="transform transition duration-300 ease-in"
        leave-from-class="opacity-100 translate-y-0 scale-100 blur-0"
        leave-to-class="opacity-0 translate-y-20 scale-90 blur-sm">
        <div v-if="showAiChat"
          class="pointer-events-auto w-[360px] h-[550px] rounded-[40px] shadow-[0_20px_60px_-15px_rgba(0,0,0,0.3)] flex flex-col overflow-hidden relative border border-white/40 siri-glass-panel">

          <div
            class="h-16 flex justify-between items-center px-6 border-b border-white/10 bg-white/10 backdrop-blur-md">
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-full siri-gradient flex items-center justify-center animate-spin-slow">
                <svg class="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M19.428 15.428a2 2 0 00-1.022-.547l-2.384-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
                </svg>
              </div>
              <div>
                <span class="block text-sm font-bold text-slate-800">AI Chef</span>
                <span class="block text-[10px] text-slate-500 font-medium tracking-wider uppercase">Online</span>
              </div>
            </div>
            <button @click="toggleChat"
              class="w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 flex items-center justify-center transition text-slate-500">✕</button>
          </div>

          <div id="chat-box-container" class="flex-1 overflow-y-auto p-5 space-y-6 custom-scrollbar bg-white/40">
            <div v-for="(msg, idx) in chatHistory" :key="idx" class="flex gap-4"
              :class="msg.role === 'user' ? 'flex-row-reverse' : ''">
              <div class="w-9 h-9 rounded-full flex-shrink-0 flex items-center justify-center shadow-sm text-sm"
                :class="msg.role === 'ai' ? 'bg-gradient-to-br from-white to-slate-100 text-blue-500' : 'bg-slate-800 text-white'">
                {{ msg.role === 'ai' ? '🤖' : 'Me' }}
              </div>

              <div
                class="max-w-[80%] p-3.5 rounded-2xl text-[13px] leading-relaxed shadow-sm border border-white/50 backdrop-blur-sm"
                :class="msg.role === 'ai' ? 'bg-white/80 text-slate-700 rounded-tl-none' : 'bg-slate-800/90 text-white rounded-tr-none'">
                {{ msg.content }}
              </div>
            </div>

            <div v-if="aiLoading" class="flex gap-4 items-center pl-2">
              <div
                class="w-9 h-9 rounded-full bg-white flex items-center justify-center shadow-sm border border-white/50">
                🤖</div>
              <div class="flex items-center gap-1 h-8">
                <div class="w-1 bg-cyan-400 rounded-full animate-wave h-3"></div>
                <div class="w-1 bg-blue-500 rounded-full animate-wave h-5 delay-75"></div>
                <div class="w-1 bg-purple-500 rounded-full animate-wave h-4 delay-150"></div>
                <div class="w-1 bg-pink-500 rounded-full animate-wave h-2 delay-200"></div>
              </div>
            </div>
          </div>

          <div class="p-5 bg-white/60 backdrop-blur-xl border-t border-white/50">
            <div class="flex gap-2 overflow-x-auto pb-3 no-scrollbar mask-gradient-right">
              <button v-for="q in suggestedQuestions" :key="q" @click="askAi(q)" :disabled="aiLoading"
                class="flex-shrink-0 text-[11px] px-3 py-1.5 bg-white/80 text-slate-600 font-medium rounded-full border border-slate-200/60 hover:bg-blue-50 hover:border-blue-200 hover:text-blue-600 transition shadow-sm active:scale-95">
                {{ q }}
              </button>
            </div>
            <div class="relative group">
              <input @keyup.enter="askAi($event.target.value); $event.target.value = ''" placeholder="Ask Siri..."
                class="w-full bg-white/50 rounded-2xl pl-4 pr-10 py-3 text-sm focus:outline-none focus:bg-white focus:ring-2 focus:ring-cyan-400/30 transition shadow-inner placeholder:text-slate-400">
              <button
                class="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 bg-blue-500 rounded-xl flex items-center justify-center text-white shadow-lg shadow-blue-500/30 hover:scale-105 active:scale-95 transition">↑</button>
            </div>
          </div>
        </div>
      </transition>

      <button @click="toggleChat" class="pointer-events-auto relative w-16 h-16 group outline-none">
        <div
          class="absolute inset-0 rounded-full siri-gradient opacity-80 blur-lg animate-pulse-slow group-hover:opacity-100 transition-opacity">
        </div>

        <div
          class="absolute inset-1 rounded-full siri-gradient border border-white/20 shadow-[inset_0_2px_10px_rgba(255,255,255,0.4)] flex items-center justify-center z-10 overflow-hidden backdrop-blur-sm group-active:scale-95 transition-transform duration-200">
          <div
            class="absolute inset-0 bg-gradient-to-tr from-transparent via-white/30 to-transparent w-[200%] h-[200%] animate-shimmer">
          </div>

          <span v-if="!showAiChat" class="text-2xl drop-shadow-md relative z-20">🤖</span>
          <span v-else class="text-xl font-bold text-white drop-shadow-md relative z-20">✕</span>
        </div>
      </button>

    </div>
  </div>
</template>

<style scoped>
/* Siri 核心渐变 */
.siri-gradient {
  background: linear-gradient(135deg, #22d3ee, #3b82f6, #a855f7, #ec4899);
  background-size: 300% 300%;
  animation: gradientMove 6s ease infinite;
}

/* 毛玻璃面板背景 */
.siri-glass-panel {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
}

@keyframes gradientMove {
  0% {
    background-position: 0% 50%;
  }

  50% {
    background-position: 100% 50%;
  }

  100% {
    background-position: 0% 50%;
  }
}

@keyframes pulse-slow {

  0%,
  100% {
    transform: scale(1);
    opacity: 0.6;
  }

  50% {
    transform: scale(1.15);
    opacity: 0.9;
  }
}

@keyframes shimmer {
  from {
    transform: translate(-50%, -50%) rotate(0deg);
  }

  to {
    transform: translate(-50%, -50%) rotate(360deg);
  }
}

/* 思考波形动画 */
@keyframes wave {

  0%,
  100% {
    height: 8px;
  }

  50% {
    height: 20px;
  }
}

.animate-wave {
  animation: wave 1s ease-in-out infinite;
}

/* 自定义滚动条 */
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(148, 163, 184, 0.5);
  border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background-color: transparent;
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}

.stat-card {
  @apply bg-white p-4 rounded-2xl border border-slate-100 text-center transition hover:border-slate-200 cursor-default;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
}

.shadow-price-card {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.stat-label {
  @apply text-[10px] text-slate-400 uppercase tracking-widest mb-1;
}

.stat-value {
  @apply font-bold text-slate-800 text-sm;
}
</style>