<script setup>
import { ref } from 'vue'
import { store } from '../store.js'
import { useRouter } from 'vue-router'

const props = defineProps({
  isOpen: Boolean,
  product: Object
})

const emit = defineEmits(['close'])
const router = useRouter()
const buyCount = ref(1)

const close = () => {
  emit('close')
  buyCount.value = 1
}

const changeCount = (delta) => {
  const newVal = buyCount.value + delta
  if (newVal >= 1 && newVal <= props.product.stock) {
    buyCount.value = newVal
  }
}

const addToCart = () => {
  const inCart = store.getProductCount(props.product.id)
  if (inCart + buyCount.value > props.product.stock) {
    store.showNotification('库存不足啦', 'error')
    return
  }
  for (let i = 0; i < buyCount.value; i++) {
    store.addToCart(props.product)
  }
  if (buyCount.value > 1) store.showNotification(`成功加购 ${buyCount.value} 件`)
  close()
}

const goToDetail = () => {
  router.push(`/product/${props.product.id}`)
  close() 
}
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div 
        v-if="isOpen && product" 
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
        @click.self="close"
      >
        <div class="bg-white w-full max-w-2xl rounded-2xl shadow-2xl overflow-hidden flex flex-col md:flex-row relative animate-scale-up">
          
          <button @click="close" class="absolute top-4 right-4 z-10 w-8 h-8 rounded-full bg-slate-100 flex items-center justify-center text-slate-500 hover:bg-slate-200 transition">
            ✕
          </button>

          <div class="w-full md:w-1/2 h-64 md:h-auto bg-slate-50 relative group cursor-pointer" @click="goToDetail">
            <img 
              v-if="product.imageUrl" 
              :src="product.imageUrl" 
              class="w-full h-full object-cover transition duration-700 group-hover:scale-105"
            />
            <div v-else class="w-full h-full flex items-center justify-center text-6xl">🐟</div>
            
            <div class="absolute inset-0 bg-black/0 group-hover:bg-black/10 flex items-center justify-center transition-all">
              <span class="opacity-0 group-hover:opacity-100 bg-white/90 px-4 py-2 rounded-full text-xs font-bold shadow-sm translate-y-4 group-hover:translate-y-0 transition-all duration-300">
                查看完整详情 →
              </span>
            </div>
          </div>

          <div class="w-full md:w-1/2 p-8 flex flex-col justify-center">
            <span class="text-xs font-bold text-blue-600 mb-2 uppercase tracking-wider">{{ product.origin }}直采</span>
            <h2 class="text-2xl font-serif-sc font-bold text-slate-900 mb-2">{{ product.name }}</h2>
            <p class="text-sm text-slate-500 mb-6 line-clamp-3 leading-relaxed">{{ product.description }}</p>

            <div class="flex items-center justify-between mb-6">
              <span class="text-3xl font-bold text-slate-900">¥{{ product.price }}</span>
              <span class="text-xs text-slate-400">库存: {{ product.stock }}</span>
            </div>

            <div class="space-y-3">
              <div class="flex items-center justify-between bg-slate-50 rounded-lg p-1 border border-slate-100" :class="{'opacity-50 pointer-events-none': product.stock <= 0}">
                <button @click="changeCount(-1)" class="w-8 h-8 flex items-center justify-center hover:bg-white rounded shadow-sm transition" :disabled="buyCount<=1">-</button>
                <span class="text-sm font-bold w-8 text-center">{{ product.stock > 0 ? buyCount : 0 }}</span>
                <button @click="changeCount(1)" class="w-8 h-8 flex items-center justify-center hover:bg-white rounded shadow-sm transition" :disabled="buyCount>=product.stock">+</button>
              </div>
              
              <button 
                @click="addToCart"
                :disabled="product.stock <= 0"
                class="w-full py-3 rounded-lg font-bold shadow-lg transition active:scale-95"
                :class="product.stock > 0 
                  ? 'bg-blue-900 text-white shadow-blue-900/20 hover:bg-blue-800' 
                  : 'bg-slate-300 text-slate-500 cursor-not-allowed shadow-none'"
              >
                {{ product.stock > 0 ? '加入购物车' : '暂时缺货' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.animate-scale-up { animation: scaleUp 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
@keyframes scaleUp {
  from { opacity: 0; transform: scale(0.95) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
</style>