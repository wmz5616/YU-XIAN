<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ProductSkeleton from '../components/ProductSkeleton.vue'
import QuickViewModal from '../components/QuickViewModal.vue'
import { store } from '../store.js' // 【关键】引入 store 以使用飞入特效

const router = useRouter()
const products = ref([])
const categories = ['全部', '鱼类', '虾类', '蟹类', '贝类', '头足类']
const currentCategory = ref('全部')
const searchQuery = ref('')
const loading = ref(true)

// 弹窗状态
const showModal = ref(false)
const selectedProduct = ref(null)

const defaultImage = 'https://images.unsplash.com/photo-1534483852723-e696b0106294?q=80&w=600&auto=format&fit=crop'

const fetchProducts = async () => {
  loading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 800))
    let url = 'http://localhost:8080/api/products'
    if (searchQuery.value.trim()) {
      url += `/search?keyword=${searchQuery.value}`
    } else if (currentCategory.value !== '全部') {
      url += `/category/${currentCategory.value}`
    }
    const res = await fetch(url)
    products.value = await res.json()
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

const getBadge = (product) => {
  if (product.stock <= 5) return { text: `仅剩 ${product.stock} 件`, color: 'bg-orange-500' }
  if (product.id % 5 === 0) return { text: '热销 TOP', color: 'bg-red-500' }
  if (product.id % 4 === 0) return { text: '产地直发', color: 'bg-blue-500' }
  return null
}

const setCategory = (cat) => {
  currentCategory.value = cat
  searchQuery.value = '' 
  fetchProducts()
}

const handleSearch = () => {
  currentCategory.value = '全部'
  fetchProducts()
}

const goToDetail = (id) => {
  router.push(`/product/${id}`)
}

// 打开快速预览
const openQuickView = (product, event) => {
  event.stopPropagation()
  selectedProduct.value = product
  showModal.value = true
}

// 【新增】首页直接加购（飞入特效）
const addToCart = (product, event) => {
  event.stopPropagation() // 阻止跳转详情页
  store.addToCart(product, event)
}

onMounted(() => {
  fetchProducts()
})
</script>

<template>
  <div class="min-h-screen bg-[#F8FAFC]">
    
    <div class="relative h-[500px] w-full overflow-hidden">
      <img src="https://images.unsplash.com/photo-1534068590799-09895a701e3e?q=80&w=2000&auto=format&fit=crop" class="absolute inset-0 w-full h-full object-cover brightness-75 scale-105 hover:scale-100 transition-transform duration-[10s]" />
      <div class="absolute inset-0 bg-gradient-to-t from-[#0F172A] via-transparent to-black/30"></div>
      <div class="relative container mx-auto px-6 h-full flex flex-col justify-center items-start text-white pt-20">
        <span class="inline-block px-3 py-1 mb-4 border border-white/30 rounded-full text-xs tracking-[0.2em] backdrop-blur-sm">EST. 2025 · ZHOU SHAN</span>
        <h1 class="text-5xl md:text-7xl font-serif-sc font-bold mb-6 leading-tight drop-shadow-lg">寻味<br>东海之鲜</h1>
        <p class="text-slate-300 max-w-md text-lg font-light leading-relaxed mb-8">不仅仅是食材，更是大海的馈赠。</p>
        <div class="w-full max-w-lg relative group">
          <img src="/icons/icon-search.png" class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 opacity-60 z-10" />
          <input v-model="searchQuery" @keyup.enter="handleSearch" type="text" placeholder="搜索珍稀食材 (如: 帝王蟹)..." class="w-full pl-12 pr-12 py-4 rounded-full bg-white/10 backdrop-blur-md border border-white/20 text-white placeholder-slate-300 focus:outline-none focus:bg-white focus:text-slate-900 transition-all shadow-2xl">
        </div>
      </div>
    </div>

    <div class="sticky top-[64px] z-40 bg-white/80 backdrop-blur-xl border-b border-slate-200/60 shadow-sm">
      <div class="container mx-auto px-4 py-4 overflow-x-auto no-scrollbar flex items-center gap-4">
        <button v-for="cat in categories" :key="cat" @click="setCategory(cat)" :class="['flex-shrink-0 px-6 py-2 rounded-full text-sm font-medium transition-all duration-300 border', currentCategory === cat ? 'bg-slate-900 text-white border-slate-900 shadow-lg shadow-slate-900/20' : 'bg-white text-slate-600 border-slate-200 hover:border-slate-400 hover:text-slate-900']">{{ cat }}</button>
      </div>
    </div>

    <main class="container mx-auto px-4 py-12">
      <div class="flex items-end justify-between mb-8">
        <div>
          <h2 class="text-3xl font-serif-sc font-bold text-slate-800">{{ currentCategory === '全部' ? '当季热销' : currentCategory }}</h2>
          <p class="text-slate-500 mt-2 text-sm">甄选 {{ products.length }} 款顶级食材</p>
        </div>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-x-8 gap-y-12">
        <template v-if="loading">
          <ProductSkeleton v-for="n in 8" :key="n" />
        </template>

        <template v-else>
          <div 
            v-for="(product, index) in products" 
            :key="product.id"
            @click="goToDetail(product.id)"
            v-scroll-reveal
            :class="`group cursor-pointer delay-${(index % 4) * 100}`"
          >
            <div v-tilt class="relative aspect-[4/3] rounded-2xl overflow-hidden mb-4 bg-white border border-slate-100 shadow-sm transition-shadow duration-300 group-hover:shadow-2xl group-hover:shadow-blue-900/20" style="transform-style: preserve-3d;">
              <div class="absolute top-3 left-3 z-20 px-3 py-1 bg-white/90 backdrop-blur-md border border-white/50 text-[10px] font-bold tracking-wider uppercase rounded-lg text-slate-800 shadow-sm" style="transform: translateZ(20px)">{{ product.origin }}</div>
              <div v-if="getBadge(product)" :class="`absolute top-3 right-3 z-20 px-2 py-1 text-[10px] font-bold text-white rounded-lg shadow-sm ${getBadge(product).color}`" style="transform: translateZ(20px)">{{ getBadge(product).text }}</div>
              <img :src="product.imageUrl || defaultImage" loading="lazy" class="w-full h-full object-cover transition-transform duration-700 ease-in-out" style="transform: translateZ(0px)" />
              
              <div class="absolute bottom-4 left-0 right-0 flex justify-center gap-3 opacity-0 group-hover:opacity-100 transition-all duration-300 translate-y-4 group-hover:translate-y-0 z-20">
                <button 
                  @click="(e) => openQuickView(product, e)"
                  class="w-10 h-10 bg-white/90 backdrop-blur text-slate-800 rounded-full flex items-center justify-center shadow-lg hover:bg-blue-600 hover:text-white transition transform hover:scale-110 tooltip-trigger"
                  title="快速预览"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                </button>
              </div>
            </div>
            
            <div class="px-2">
              <h3 class="text-lg font-serif-sc font-bold text-slate-900 group-hover:text-blue-800 transition-colors truncate">{{ product.name }}</h3>
              <p class="text-xs text-slate-400 mt-1 line-clamp-1 mb-3 font-light">{{ product.description }}</p>
              <div class="flex items-center justify-between border-t border-slate-100/50 pt-3">
                <div class="flex flex-col">
                  <span class="text-[10px] text-slate-400 uppercase tracking-wide scale-90 origin-left">Price</span>
                  <span class="text-xl font-bold text-slate-900">¥{{ product.price }}</span>
                </div>
                
                <button 
                  @click="(e) => addToCart(product, e)"
                  class="w-10 h-10 rounded-full bg-white border border-slate-100 shadow-sm flex items-center justify-center text-slate-400 hover:bg-blue-600 hover:border-blue-600 hover:text-white hover:shadow-lg hover:shadow-blue-600/30 active:scale-75 transition-all duration-300"
                >
                  <img src="/icons/icon-add.png" class="w-4 h-4 brightness-0 opacity-50 group-hover:brightness-200 group-hover:opacity-100 transition" />
                </button>
              </div>
            </div>
          </div>
        </template>
      </div>
      
      <div v-if="products.length === 0" class="py-32 text-center">
        <div class="text-6xl mb-4 opacity-20">🌊</div>
        <p class="text-slate-400 font-serif-sc text-xl">暂无相关海鲜...</p>
      </div>
    </main>

    <QuickViewModal 
      :is-open="showModal" 
      :product="selectedProduct" 
      @close="showModal = false" 
    />
  </div>
</template>