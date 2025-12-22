<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '../store.js'
import { request } from '@/utils/request'

const router = useRouter()
const activeTab = ref('available')
const allCoupons = ref([])
const loading = ref(true)

const availableCoupons = computed(() => allCoupons.value.filter(c => c.status === 'UNUSED' && new Date(c.expiryDate || '2999-01-01') > new Date()).sort((a, b) => b.amount - a.amount))
const usedCoupons = computed(() => allCoupons.value.filter(c => c.status === 'USED' || new Date(c.expiryDate || '2999-01-01') <= new Date()).sort((a, b) => new Date(b.receiveTime) - new Date(a.receiveTime)))

const fetchCoupons = async () => {
    loading.value = true
    try {
        const username = store.currentUser?.username
        if (!username) return
        const res = await request.get(`/api/coupons/my?username=${username}`)
        if (res && Array.isArray(res)) {
            allCoupons.value = res
            store.myCoupons = res
        }
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    if (!store.currentUser) { router.push('/login'); return }
    fetchCoupons()
})

const useCoupon = (coupon) => {
    if (coupon.status !== 'UNUSED') return
    store.showNotification(`已激活电子券: ${coupon.couponName}`, 'success')
    router.push('/cart')
}

// 亮色系高饱和渐变主题
const getCardTheme = (type) => {
    if (type === 'EXCHANGE') return 'bg-gradient-to-br from-amber-400 via-orange-500 to-pink-500 shadow-orange-500/30' // 尊贵流光金
    return 'bg-gradient-to-br from-cyan-400 via-blue-500 to-indigo-600 shadow-blue-500/30' // 科技极光蓝
}
</script>

<template>
    <div class="min-h-screen bg-[#F0F4F8] pb-24 relative overflow-hidden font-sans selection:bg-indigo-100 selection:text-indigo-900">
        
        <div class="fixed inset-0 pointer-events-none overflow-hidden">
            <div class="absolute -top-20 -left-20 w-[600px] h-[600px] bg-cyan-300/30 rounded-full blur-[100px] animate-pulse-slow"></div>
            <div class="absolute top-[40%] -right-40 w-[500px] h-[500px] bg-purple-300/30 rounded-full blur-[100px] animate-pulse-slow" style="animation-delay: 2s;"></div>
            <div class="absolute inset-0 bg-[url('/icons/grid.svg')] opacity-[0.03] bg-center invert"></div>
        </div>

        <div class="container mx-auto px-4 py-8 relative z-10 max-w-4xl">
            <div class="flex items-center justify-between mb-10">
                <div class="flex items-center gap-4">
                    <button @click="router.back()"
                        class="w-10 h-10 rounded-full bg-white/60 backdrop-blur-md border border-white/40 flex items-center justify-center text-slate-600 hover:bg-white hover:shadow-md transition-all group">
                        <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 group-hover:-translate-x-0.5 transition" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                        </svg>
                    </button>
                    <div>
                        <h1 class="text-2xl font-black text-slate-800 tracking-tight flex items-center gap-2">
                            电子票夹
                            <span class="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-ping opacity-75"></span>
                        </h1>
                        <p class="text-xs text-slate-400 font-bold tracking-widest uppercase mt-0.5">优惠券</p>
                    </div>
                </div>
                <button @click="router.push('/points')"
                    class="group relative overflow-hidden px-5 py-2.5 rounded-full bg-white border border-indigo-100 shadow-lg shadow-indigo-100/50 hover:shadow-indigo-200 hover:-translate-y-0.5 transition-all">
                    <span class="relative z-10 text-xs font-black text-indigo-600 flex items-center gap-1.5">
                        兑换中心 <span class="text-lg leading-none"></span>
                    </span>
                    <div class="absolute inset-0 bg-indigo-50 transform translate-x-[-100%] group-hover:translate-x-0 transition-transform duration-300"></div>
                </button>
            </div>

            <div class="flex p-1.5 bg-white/70 backdrop-blur-xl rounded-2xl border border-white/60 shadow-xl shadow-slate-200/50 mb-10 mx-auto max-w-sm relative">
                <div class="absolute inset-y-1.5 rounded-xl bg-gradient-to-r from-indigo-500 to-blue-500 shadow-lg shadow-blue-500/30 transition-all duration-500 cubic-bezier"
                    :class="activeTab === 'available' ? 'left-1.5 w-[calc(50%-6px)]' : 'left-[calc(50%+3px)] w-[calc(50%-6px)]'">
                </div>
                <button @click="activeTab = 'available'"
                    class="relative flex-1 py-2.5 rounded-xl font-bold text-xs tracking-wide transition-colors z-10 text-center"
                    :class="activeTab === 'available' ? 'text-white' : 'text-slate-500 hover:text-slate-800'">
                    可用优惠券
                </button>
                <button @click="activeTab = 'used'"
                    class="relative flex-1 py-2.5 rounded-xl font-bold text-xs tracking-wide transition-colors z-10 text-center"
                    :class="activeTab === 'used' ? 'text-white' : 'text-slate-500 hover:text-slate-800'">
                    已使用/过期
                </button>
            </div>

            <div class="min-h-[400px]">
                <Transition name="fade" mode="out-in">
                    
                    <div v-if="activeTab === 'available'" key="available" class="space-y-6">
                        <div v-if="availableCoupons.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-6">
                            
                            <div v-for="coupon in availableCoupons" :key="coupon.id" 
                                @click="useCoupon(coupon)"
                                class="coupon-card group relative h-36 rounded-3xl overflow-hidden cursor-pointer transition-all duration-500 hover:-translate-y-1.5 hover:shadow-2xl text-white border border-white/20 backdrop-blur-sm"
                                :class="getCardTheme(coupon.type)">
                                
                                <div class="absolute inset-0 z-10 bg-gradient-to-r from-transparent via-white/30 to-transparent -translate-x-[150%] skew-x-[-20deg] group-hover:animate-shine pointer-events-none"></div>
                                
                                <div class="absolute inset-0 opacity-20 bg-[url('/icons/noise.png')] mix-blend-overlay z-0"></div>

                                <div class="absolute -right-6 -bottom-6 w-32 h-32 bg-white/20 blur-2xl rounded-full group-hover:scale-110 transition-transform duration-700"></div>
                                
                                <div class="relative z-20 h-full flex">
                                    <div class="w-[36%] flex flex-col items-center justify-center bg-black/5 backdrop-blur-[2px] border-r border-white/20 relative">
                                        <div class="relative flex items-baseline gap-0.5 transform group-hover:scale-105 transition-transform duration-300">
                                            <span class="text-sm font-medium opacity-90">¥</span>
                                            <span class="text-5xl font-black tracking-tighter drop-shadow-md">{{ coupon.amount }}</span>
                                        </div>
                                        <span class="text-[10px] font-bold opacity-70 tracking-widest uppercase mt-1">现金券</span>
                                    </div>

                                    <div class="flex-1 p-5 flex flex-col justify-between">
                                        <div class="flex justify-between items-start">
                                            <div>
                                                <h3 class="font-bold text-lg tracking-wide drop-shadow-sm">{{ coupon.couponName }}</h3>
                                                <p class="text-[11px] font-medium opacity-90 mt-1 flex items-center gap-1.5">
                                                    <span class="w-1.5 h-1.5 rounded-full bg-white shadow-sm animate-pulse"></span>
                                                    {{ coupon.minSpend > 0 ? `满 ¥${coupon.minSpend} 可用` : '全场通用' }}
                                                </p>
                                            </div>
                                        </div>

                                        <div class="flex justify-between items-end">
                                            <div class="text-[10px] font-mono opacity-70 tracking-tight bg-black/10 px-2 py-0.5 rounded">
                                                有效期至: {{ coupon.expiryDate ? new Date(coupon.expiryDate).toLocaleDateString() : '永久有效' }}
                                            </div>
                                            
                                            <button class="bg-white text-indigo-600 text-xs font-black px-4 py-1.5 rounded-full shadow-lg hover:shadow-xl hover:scale-105 active:scale-95 transition-all transform translate-y-1 opacity-90 group-hover:translate-y-0 group-hover:opacity-100">
                                                使用
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div v-else class="flex flex-col items-center justify-center py-24 text-center">
                            <div class="relative mb-6">
                                <div class="w-24 h-24 bg-white rounded-full flex items-center justify-center shadow-[0_10px_40px_-10px_rgba(99,102,241,0.3)] animate-float z-10 relative">
                                    <span class="text-4xl">⚡</span>
                                </div>
                                <div class="absolute inset-0 bg-indigo-400 rounded-full blur-2xl opacity-20 animate-pulse"></div>
                            </div>
                            <h3 class="text-slate-800 font-black text-xl mb-2">空空如也</h3>
                            <p class="text-slate-400 text-sm mb-6">您目前没有可用的电子票券</p>
                            <button @click="router.push('/points')" 
                                class="text-indigo-600 text-sm font-bold border-b-2 border-indigo-100 hover:border-indigo-600 transition-all pb-0.5">
                                前往兑换中心获取 >>
                            </button>
                        </div>
                    </div>

                    <div v-else key="used" class="grid grid-cols-1 md:grid-cols-2 gap-5">
                        <div v-for="coupon in usedCoupons" :key="coupon.id" 
                            class="group h-28 rounded-2xl border border-slate-200 bg-white/60 backdrop-blur-md flex items-center justify-between p-6 hover:shadow-lg transition-all relative overflow-hidden">
                            
                            <div class="opacity-50 group-hover:opacity-100 transition-opacity">
                                <div class="text-3xl font-black text-slate-300 group-hover:text-slate-800 transition-colors">¥{{ coupon.amount }}</div>
                                <div class="text-xs font-bold text-slate-400 mt-1">{{ coupon.couponName }}</div>
                            </div>

                            <div class="border-2 px-3 py-1 rounded-lg text-xs font-black tracking-wider uppercase transform -rotate-12 group-hover:rotate-0 transition-transform"
                                :class="coupon.status === 'USED' ? 'border-slate-200 text-slate-300' : 'border-red-100 text-red-200'">
                                {{ coupon.status === 'USED' ? 'USED' : 'EXPIRED' }}
                            </div>

                            <div class="absolute inset-0 bg-gradient-to-b from-transparent via-indigo-500/10 to-transparent h-[2px] w-full top-0 group-hover:animate-scan pointer-events-none opacity-0 group-hover:opacity-100"></div>
                        </div>
                    </div>

                </Transition>
            </div>
        </div>
    </div>
</template>

<style scoped>
/* 定义缓动曲线 */
.cubic-bezier { transition-timing-function: cubic-bezier(0.34, 1.56, 0.64, 1); }

/* 流光动画 */
@keyframes shine {
    0% { transform: translateX(-150%) skewX(-20deg); }
    100% { transform: translateX(200%) skewX(-20deg); }
}
.group-hover\:animate-shine:hover { animation: shine 0.7s ease-in-out; }

/* 浮动动画 */
@keyframes float {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-12px); }
}
.animate-float { animation: float 4s ease-in-out infinite; }

/* 扫描线 */
@keyframes scan {
    0% { top: -10%; }
    100% { top: 110%; }
}
.group-hover\:animate-scan:hover { animation: scan 1.5s linear infinite; }

/* 呼吸 */
@keyframes pulse-slow {
    0%, 100% { opacity: 0.3; transform: scale(1); }
    50% { opacity: 0.6; transform: scale(1.1); }
}
.animate-pulse-slow { animation: pulse-slow 5s ease-in-out infinite; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(10px); }
</style>