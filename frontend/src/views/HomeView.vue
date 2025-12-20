<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import ProductSkeleton from '../components/ProductSkeleton.vue'
import QuickViewModal from '../components/QuickViewModal.vue'
import { store } from '../store.js'
import { request } from '@/utils/request'

const router = useRouter()
const products = ref([])
const recommendations = ref([])
const categories = ['全部', '鱼类', '虾类', '蟹类', '贝类', '头足类']
const currentCategory = ref('全部')
const searchQuery = ref('')
const loading = ref(true)
const showModal = ref(false)
const selectedProduct = ref(null)
const defaultImage = 'https://images.unsplash.com/photo-1534483852723-e696b0106294?q=80&w=600&auto=format&fit=crop'
const isSearchFocused = ref(false)
const searchHistory = ref(JSON.parse(localStorage.getItem('yuxian_search_history') || '[]'))
const hotKeywords = ['帝王蟹', '三文鱼', '波士顿龙虾', '生蚝', '大闸蟹']

const visibleCount = ref(12)
const PAGE_SIZE = 12
const showBackTop = ref(false)

const saveHistory = (keyword) => {
  if (!keyword || !keyword.trim()) return
  const newHistory = [keyword, ...searchHistory.value.filter(k => k !== keyword)].slice(0, 8)
  searchHistory.value = newHistory
  localStorage.setItem('yuxian_search_history', JSON.stringify(newHistory))
}

const clearHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('yuxian_search_history')
}

const quickSearch = (keyword) => {
  searchQuery.value = keyword
  isSearchFocused.value = false
  handleSearch()
}

const fetchRecommendations = async () => {
  try {
    const data = await request('/api/products/recommend')
    recommendations.value = data
  } catch (e) { console.error(e) }
}

const fetchProducts = async () => {
  loading.value = true
  visibleCount.value = PAGE_SIZE
  try {
    await new Promise(resolve => setTimeout(resolve, 600))
    let url = '/api/products'
    if (currentCategory.value !== '全部') {
      url = `/api/products/category/${currentCategory.value}`
    } else if (searchQuery.value) {
      url = `/api/products/search?keyword=${searchQuery.value}`
    }
    const data = await request(url)
    products.value = data
  } catch (e) { console.error(e) } finally { loading.value = false }
}

const displayedProducts = computed(() => products.value.slice(0, visibleCount.value))
const hasMore = computed(() => visibleCount.value < products.value.length)
const loadMore = () => { visibleCount.value += PAGE_SIZE }
const handleScroll = () => { showBackTop.value = window.scrollY > 500 }
const scrollToTop = () => { window.scrollTo({ top: 0, behavior: 'smooth' }) }

const getBadge = (product) => {
  if (product.stock <= 5) return { text: `仅剩 ${product.stock} 件`, color: 'bg-gradient-to-r from-orange-500 to-amber-500 shadow-orange-200' }
  if (product.id % 5 === 0) return { text: '热销 TOP', color: 'bg-gradient-to-r from-red-500 to-rose-500 shadow-red-200' }
  if (product.id % 4 === 0) return { text: '产地直发', color: 'bg-gradient-to-r from-blue-500 to-indigo-500 shadow-blue-200' }
  return null
}

const setCategory = (cat) => {
  currentCategory.value = cat
  searchQuery.value = ''
  fetchProducts()
}

const handleSearch = () => {
  currentCategory.value = '全部'
  saveHistory(searchQuery.value)
  isSearchFocused.value = false
  fetchProducts()
}

const goToDetail = (id) => { router.push(`/product/${id}`) }
const openQuickView = (product, e) => { e.stopPropagation(); selectedProduct.value = product; showModal.value = true }
const addToCart = (product, e) => { e.stopPropagation(); store.addToCart(product, e) }

onMounted(() => {
  const urlParams = new URLSearchParams(window.location.search)
  const keyword = urlParams.get('keyword')
  if (keyword) {
    searchQuery.value = keyword
    handleSearch()
  } else {
    fetchProducts()
  }
  fetchRecommendations()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => { window.removeEventListener('scroll', handleScroll) })
</script>

<template>
  <div class="min-h-screen bg-[#F8FAFC] pb-12" @click="isSearchFocused = false">

    <div class="relative h-[560px] w-full group overflow-hidden">
      <div class="absolute inset-0 overflow-hidden">
        <img src="https://images.unsplash.com/photo-1559742811-822873691df8?q=80&w=2000&auto=format&fit=crop"
          class="w-full h-full object-cover brightness-[0.45] scale-105 group-hover:scale-100 transition-transform duration-[10s]" />

        <div class="absolute inset-0 bg-gradient-to-b from-slate-900/40 via-transparent to-[#F8FAFC]"></div>
      </div>

      <div
        class="relative container mx-auto px-6 h-full flex flex-col justify-center items-center text-center text-white pb-10 pt-10">

        <span
          class="inline-block px-4 py-1.5 mb-6 border border-white/20 bg-white/5 rounded-full text-xs font-medium tracking-[0.3em] backdrop-blur-md shadow-lg">
          EST. 2025 · GLOBAL SELECTION
        </span>

        <h1 class="text-5xl md:text-7xl font-serif-sc font-bold mb-6 leading-tight drop-shadow-2xl tracking-wide">
          臻选全球鲜味
        </h1>

        <p
          class="text-slate-100 max-w-xl text-lg font-light leading-relaxed mb-10 tracking-wide opacity-90 drop-shadow-md">
          汇聚世界顶级渔场，将深海的纯净直接送达您的餐桌。<br>每一次品尝，都是对海洋的礼赞。
        </p>

        <div class="w-full max-w-xl relative" @click.stop>
          <svg class="absolute left-5 top-1/2 -translate-y-1/2 w-5 h-5 text-white/80 z-20 pointer-events-none"
            xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>

          <input v-model="searchQuery" @focus="isSearchFocused = true" @keyup.enter="handleSearch" type="text"
            placeholder="搜索"
            class="shadow-2xl shadow-blue-900/20 focus:shadow-glow-blue w-full pl-14 pr-6 py-4 rounded-full bg-white/10 backdrop-blur-xl border border-white/30 text-white placeholder-white/60 focus:outline-none focus:bg-white/20 focus:border-white/50 focus:ring-4 focus:ring-white/10 transition-all shadow-2xl relative z-10 font-light tracking-wide">

          <Transition name="fade">
            <div v-if="isSearchFocused"
              class="absolute top-full left-0 right-0 mt-3 bg-white/95 backdrop-blur-xl rounded-2xl shadow-2xl border border-slate-100 p-5 z-50 text-slate-800 text-left">
              <div v-if="searchHistory.length > 0" class="mb-5">
                <div class="flex justify-between items-center mb-3">
                  <span class="text-xs font-bold text-slate-400 uppercase tracking-wider">历史搜索</span>
                  <button @click="clearHistory" class="text-xs text-slate-400 hover:text-red-500 transition">清空</button>
                </div>
                <div class="flex flex-wrap gap-2">
                  <span v-for="tag in searchHistory" :key="tag" @click="quickSearch(tag)"
                    class="bg-slate-100/80 hover:bg-blue-50 text-slate-600 hover:text-blue-600 px-3 py-1.5 rounded-lg text-sm cursor-pointer transition">{{
                      tag }}</span>
                </div>
              </div>
              <div>
                <div class="text-xs font-bold text-slate-400 mb-3 uppercase tracking-wider flex items-center gap-2">热门推荐
                </div>
                <div class="flex flex-wrap gap-2">
                  <span v-for="(tag, i) in hotKeywords" :key="tag" @click="quickSearch(tag)"
                    class="px-3 py-1.5 rounded-lg text-sm cursor-pointer transition border font-medium border-transparent"
                    :class="i < 3 ? 'bg-orange-50 text-orange-600 hover:bg-orange-100' : 'bg-slate-50 text-slate-600 hover:bg-slate-100'">{{
                      tag }}</span>
                </div>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </div>

    <div v-if="recommendations.length > 0" class="container mx-auto px-6 -mt-10 relative z-20 mb-16">
      <div class="bg-white rounded-2xl shadow-xl shadow-slate-200/50 p-6 border border-slate-100">
        <div class="flex items-center justify-between mb-6 border-b border-slate-50 pb-4">
          <div class="flex items-center gap-3">
            <img src="/icons/hot.png" class="w-8 h-8 object-contain" alt="推荐" />
            <div>
              <h2 class="text-lg font-bold text-slate-800">推荐</h2>
              <p class="text-xs text-slate-400 font-light">每日精选</p>
            </div>
          </div>
          <span class="text-xs font-bold text-orange-500 bg-orange-50 px-3 py-1 rounded-full">今日特供</span>
        </div>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
          <div v-for="item in recommendations" :key="'rec-' + item.id" @click="goToDetail(item.id)"
            class="group cursor-pointer">
            <div class="aspect-square bg-slate-50 rounded-xl overflow-hidden mb-3 relative border border-slate-100">
              <div class="absolute inset-0 bg-black/0 group-hover:bg-black/5 transition-colors z-10"></div>
              <img :src="item.imageUrl"
                class="w-full h-full object-cover group-hover:scale-110 transition duration-700" />
            </div>
            <h4 class="font-medium text-slate-800 text-sm truncate group-hover:text-blue-600 transition">{{ item.name }}
            </h4>
            <div class="text-orange-600 font-bold text-sm mt-1">¥{{ item.price }}</div>
          </div>
        </div>
      </div>
    </div>

    <div
      class="sticky top-[80px] z-40 bg-[#F8FAFC]/95 backdrop-blur-sm border-b border-slate-200/60 shadow-sm transition-all">
      <div class="container mx-auto px-4 py-3 overflow-x-auto no-scrollbar flex items-center justify-center gap-3">
        <button v-for="cat in categories" :key="cat" @click="setCategory(cat)"
          :class="['flex-shrink-0 px-5 py-2 rounded-full text-sm font-medium transition-all duration-300 border', currentCategory === cat ? 'bg-slate-800 text-white border-slate-800 shadow-lg shadow-slate-900/20 scale-105' : 'bg-white text-slate-600 border-slate-200 hover:border-slate-300 hover:bg-slate-50']">{{
            cat }}</button>
      </div>
    </div>

    <main class="container mx-auto px-4 py-12">
      <div class="flex items-end justify-between mb-10 px-2">
        <div>
          <h2 class="text-3xl font-serif-sc font-bold text-slate-800">{{ currentCategory === '全部' ? '当季热销' :
            currentCategory }}</h2>
          <p class="text-slate-500 mt-2 text-sm font-light">甄选 {{ products.length }} 款顶级食材，源头直供</p>
        </div>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-x-6 gap-y-10">
        <template v-if="loading">
          <ProductSkeleton v-for="n in 8" :key="n" />
        </template>
        <template v-else>
          <div 
  v-for="(product, index) in displayedProducts" 
  :key="product.id" 
  @click="goToDetail(product.id)"
  class="group cursor-pointer animate-fade-in-up opacity-0"
  :style="{ animationDelay: `${index * 100}ms` }" 
>
            <div v-tilt
              class="relative aspect-[4/3] rounded-2xl overflow-hidden mb-4 bg-white border border-slate-100 shadow-sm transition-shadow duration-300 group-hover:shadow-2xl group-hover:shadow-blue-900/20"
              style="transform-style: preserve-3d;">
              <div
                class="absolute top-3 left-3 z-20 px-2.5 py-1 bg-white/90 backdrop-blur-md text-[10px] font-bold tracking-wider uppercase rounded-lg text-slate-700 shadow-sm border border-slate-100/50"
                style="transform: translateZ(20px)">{{ product.origin }}</div>
              <div v-if="getBadge(product)"
                :class="`absolute top-3 right-3 z-20 px-2.5 py-1 text-[10px] font-bold text-white rounded-lg shadow-sm ${getBadge(product).color}`"
                style="transform: translateZ(20px)">{{ getBadge(product).text }}</div>
              <img :src="product.imageUrl || defaultImage"
                class="w-full h-full object-cover transition-transform duration-700 ease-in-out group-hover:scale-105"
                style="transform: translateZ(0px)" />
              <div
                class="absolute inset-0 bg-black/10 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-3 z-20">
                <button @click="(e) => openQuickView(product, e)"
                  class="w-10 h-10 bg-white/90 backdrop-blur text-slate-800 rounded-full flex items-center justify-center shadow-lg hover:bg-blue-600 hover:text-white transition-all transform translate-y-4 group-hover:translate-y-0 duration-300">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24"
                    stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                </button>
              </div>
            </div>
            <div class="px-1">
              <h3
                class="text-[17px] font-bold text-slate-800 group-hover:text-blue-700 transition-colors truncate mb-1">
                {{ product.name }}</h3>
              <p class="text-xs text-slate-400 line-clamp-1 mb-3 font-light">{{ product.description }}</p>
              <div class="flex items-center justify-between border-t border-slate-100/50 pt-3">
                <div><span class="text-xs text-slate-400 font-serif italic mr-0.5">¥</span><span
                    class="text-xl font-bold text-slate-900">{{ product.price }}</span></div>
                <button @click="(e) => addToCart(product, e)"
                  class="w-10 h-10 rounded-full bg-white border border-slate-100 shadow-sm flex items-center justify-center text-slate-400 hover:bg-blue-600 hover:border-blue-600 hover:text-white hover:shadow-lg hover:shadow-blue-600/30 active:scale-75 transition-all duration-300">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24"
                    stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </template>
      </div>

      <div v-if="products.length === 0 && !loading" class="py-32 text-center">
        <div class="text-6xl mb-4 opacity-20 grayscale">🐟</div>
        <p class="text-slate-400 font-serif-sc text-xl">暂无相关海鲜...</p>
      </div>
      <div v-if="hasMore && !loading" class="mt-16 text-center">
        <button @click="loadMore"
          class="group relative inline-flex items-center gap-2 px-8 py-3 bg-white border border-slate-200 rounded-full text-slate-600 font-bold shadow-sm hover:shadow-xl hover:border-blue-200 hover:text-blue-600 transition-all duration-300">
          <span>加载更多鲜货</span><svg xmlns="http://www.w3.org/2000/svg"
            class="w-4 h-4 transition-transform group-hover:translate-y-1" fill="none" viewBox="0 0 24 24"
            stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </button>
        <p class="text-xs text-slate-400 mt-3">已显示 {{ displayedProducts.length }} / {{ products.length }} 件商品</p>
      </div>
      <div v-if="!hasMore && products.length > 0" class="mt-16 flex items-center justify-center gap-4 text-slate-300">
        <span class="h-px w-12 bg-slate-200"></span><span class="text-xs">我们也是有底线的</span><span
          class="h-px w-12 bg-slate-200"></span>
      </div>
    </main>

    <Transition name="fade">
      <button v-if="showBackTop" @click="scrollToTop"
        class="fixed bottom-24 right-6 z-50 w-12 h-12 bg-white/80 backdrop-blur-md border border-slate-200 text-slate-600 rounded-full shadow-2xl flex items-center justify-center hover:bg-blue-600 hover:text-white hover:-translate-y-1 transition-all duration-300 group">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 transition-transform group-hover:-translate-y-0.5"
          fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18" />
        </svg>
      </button>
    </Transition>

    <QuickViewModal :is-open="showModal" :product="selectedProduct" @close="showModal = false" />
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>