<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { store } from '../store.js'
import * as echarts from 'echarts'
import { request } from '@/utils/request'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({ breaks: true })

const route = useRoute()
const router = useRouter()
const product = ref(null)
const insightData = ref(null)
const relatedProducts = ref([])
const chartRef = ref(null)
const buyCount = ref(1)
const isAdding = ref(false)
const mapInstance = ref(null)

const showAiChat = ref(false)
const aiLoading = ref(false)
const aiTyping = ref(false)
const chatHistory = ref([])
const chatBoxRef = ref(null)

const suggestedQuestions = ref(['怎么做最好吃？','需要冷冻保存吗？','发什么快递？'])

const scrollToBottom = () => {
  nextTick(() => {
    if (chatBoxRef.value) {
      chatBoxRef.value.scrollTop = chatBoxRef.value.scrollHeight
    }
  })
}

const typeWriterEffect = async (fullText) => {
  aiTyping.value = true

  const msgIndex = chatHistory.value.push({ role: 'ai', content: '', html: '' }) - 1
  const msg = chatHistory.value[msgIndex]

  const speed = 30
  for (let i = 0; i < fullText.length; i++) {
    if (!showAiChat.value) break;

    msg.content += fullText[i]
    msg.html = md.render(msg.content)
    scrollToBottom()
    await new Promise(r => setTimeout(r, speed))
  }

  aiTyping.value = false
}

const toggleChat = () => {
  showAiChat.value = !showAiChat.value
  if (showAiChat.value) scrollToBottom()
}

const askAi = async (question) => {
  if (!question || aiLoading.value || aiTyping.value) return
  if (!product.value) return

  chatHistory.value.push({ role: 'user', content: question })
  scrollToBottom()

  aiLoading.value = true

  try {
    const res = await request.post('/api/ai/ask', {
      productName: product.value.name,
      question: question
    })

    aiLoading.value = false
    await typeWriterEffect(res.answer)

  } catch (e) {
    console.error(e)
    aiLoading.value = false
    chatHistory.value.push({
      role: 'ai',
      content: '网络开小差了，请重试 😭',
      html: '<p>网络开小差了，请重试 😭</p>'
    })
    scrollToBottom()
  }
}

const SVG_PIN = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="48" height="48" fill="none"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" fill="#0f172a" stroke="white" stroke-width="2"/><circle cx="12" cy="9" r="3" fill="white"/></svg>`

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
  const createLabel = (title, sub) => `<div class="flex flex-col items-center transform transition-transform hover:scale-110" style="pointer-events: none;"><div class="bg-slate-900/90 backdrop-blur text-white px-3 py-1.5 rounded-lg shadow-xl mb-1 flex flex-col items-center border border-slate-700/50"><span class="text-xs font-bold whitespace-nowrap">${title}</span><span class="text-[10px] text-slate-300 uppercase tracking-wider">${sub}</span></div><div class="w-0 h-0 border-l-[6px] border-l-transparent border-r-[6px] border-r-transparent border-t-[6px] border-t-slate-900/90 mb-1"></div><div class="relative top-[-4px] drop-shadow-md">${SVG_PIN}</div></div>`
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
              <span class="text-xs font-bold" :class="product.stock < 10 ? 'text-red-500' : 'text-slate-500'">库存: {{
                product.stock }} {{ getUnit(product.name) }}</span>
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
                <div class="flex justify-between items-center border-b border-white/10 pb-3"><span
                    class="text-sm text-slate-300">海水温度</span><span class="font-mono text-xl font-bold text-blue-200">{{
                      insightData.environment?.waterTemp }}°C</span></div>
                <div class="flex justify-between items-center border-b border-white/10 pb-3"><span
                    class="text-sm text-slate-300">海水盐度</span><span class="font-mono text-xl font-bold text-teal-200">{{
                      insightData.environment?.salinity }}%</span></div>
                <div class="flex justify-between items-center"><span class="text-sm text-slate-300">作业风力</span><span
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

      <transition name="chat-pop">
        <div v-if="showAiChat"
          class="pointer-events-auto w-[380px] h-[550px] bg-white rounded-[24px] shadow-2xl flex flex-col overflow-hidden relative border border-slate-200 ring-4 ring-slate-100">

          <div class="h-14 flex justify-between items-center px-5 border-b border-slate-100 bg-white z-10">
            <div class="flex items-center gap-3">
              <div>
                <span class="block text-sm font-bold text-slate-800">小助手</span>
                <span class="block text-[10px] text-green-500 font-medium">● 在线</span>
              </div>
            </div>
            <button @click="toggleChat"
              class="w-8 h-8 rounded-full bg-slate-50 hover:bg-slate-100 flex items-center justify-center transition text-slate-400">✕</button>
          </div>

          <div ref="chatBoxRef"
            class="flex-1 overflow-y-auto p-4 space-y-4 bg-[#F8FAFC] custom-scrollbar scroll-smooth">
            <div v-if="chatHistory.length === 0"
              class="flex flex-col items-center justify-center h-full opacity-40 space-y-4">
              <p class="text-xs text-slate-500">我是您的AI小助手，尽管问我吧~</p>
            </div>

            <div v-for="(msg, idx) in chatHistory" :key="idx" class="flex gap-3 w-full"
              :class="msg.role === 'user' ? 'flex-row-reverse' : ''">
              <div
                class="w-8 h-8 rounded-full flex-shrink-0 flex items-center justify-center text-xs shadow-sm border border-slate-100"
                :class="msg.role === 'ai' ? 'bg-white' : 'bg-slate-800 text-white'">
                {{ msg.role === 'ai' ? 'AI' : '我' }}
              </div>

              <div :class="[
                'max-w-[80%] px-4 py-3 rounded-2xl text-[13px] leading-relaxed shadow-sm break-words',
                msg.role === 'user'
                  ? 'bg-slate-800 text-white rounded-tr-none'
                  : 'bg-white text-slate-700 border border-slate-200 rounded-tl-none markdown-body'
              ]">
                <div v-if="msg.html" v-html="msg.html"></div>
                <div v-else>{{ msg.content }}</div>
              </div>
            </div>

            <div v-if="aiLoading" class="flex gap-3 w-full">
              <div
                class="w-8 h-8 rounded-full bg-white flex-shrink-0 flex items-center justify-center shadow-sm border border-slate-100">
                🤖</div>
              <div
                class="bg-white px-4 py-3 rounded-2xl rounded-tl-none border border-slate-200 shadow-sm flex items-center gap-1">
                <div class="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce"></div>
                <div class="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce delay-75"></div>
                <div class="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce delay-150"></div>
              </div>
            </div>
          </div>

          <div class="p-4 bg-white border-t border-slate-100">
            <div class="flex gap-2 overflow-x-auto pb-3 no-scrollbar">
              <button v-for="q in suggestedQuestions" :key="q" @click="askAi(q)" :disabled="aiLoading || aiTyping"
                class="flex-shrink-0 text-[11px] px-3 py-1.5 bg-slate-50 text-slate-600 rounded-full border border-slate-200 hover:bg-blue-50 hover:border-blue-200 hover:text-blue-600 transition disabled:opacity-50">
                {{ q }}
              </button>
            </div>
            <div class="relative">
              <input @keyup.enter="askAi($event.target.value); $event.target.value = ''"
                :disabled="aiLoading || aiTyping" placeholder="问点什么..."
                class="w-full bg-slate-100 rounded-xl pl-4 pr-11 py-3 text-sm focus:outline-none focus:bg-white focus:ring-2 focus:ring-blue-500/10 transition disabled:opacity-60 placeholder:text-slate-400">
              <button
                class="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 bg-slate-900 rounded-lg flex items-center justify-center text-white shadow-md hover:bg-blue-600 active:scale-95 transition disabled:bg-slate-300">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none"
                  stroke="currentColor" stroke-width="2">
                  <line x1="22" y1="2" x2="11" y2="13"></line>
                  <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </transition>

      <button @click="toggleChat"
        class="pointer-events-auto w-14 h-14 rounded-full bg-slate-900 text-white shadow-2xl shadow-slate-900/40 flex items-center justify-center hover:scale-105 hover:bg-blue-600 active:scale-95 transition-all duration-300 group z-50">
        <span v-if="!showAiChat" class="text-2xl group-hover:rotate-12 transition-transform">💬</span>
        <span v-else class="text-xl font-bold">✕</span>
      </button>

    </div>
  </div>
</template>

<style scoped>
.chat-pop-enter-active,
.chat-pop-leave-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.chat-pop-enter-from,
.chat-pop-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

:deep(.markdown-body p) {
  margin-bottom: 0.5em;
}

:deep(.markdown-body p:last-child) {
  margin-bottom: 0;
}

:deep(.markdown-body ul) {
  list-style-type: disc;
  padding-left: 1.2em;
  margin-bottom: 0.5em;
}

:deep(.markdown-body ol) {
  list-style-type: decimal;
  padding-left: 1.2em;
  margin-bottom: 0.5em;
}

:deep(.markdown-body strong) {
  font-weight: 700;
  color: #1e293b;
}

:deep(.markdown-body li) {
  margin-bottom: 0.2em;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(203, 213, 225, 0.5);
  border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}

.stat-card {
  @apply bg-white p-4 rounded-2xl border border-slate-100 text-center transition hover:border-slate-200 cursor-default;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
}

.stat-label {
  @apply text-[10px] text-slate-400 uppercase tracking-widest mb-1;
}

.stat-value {
  @apply font-bold text-slate-800 text-sm;
}

.shadow-price-card {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}
</style>