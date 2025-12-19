<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '../store.js'
import { request } from '@/utils/request'
import Swal from 'sweetalert2'

const router = useRouter()
const activeTab = ref('available')
const allCoupons = ref([])

const availableCoupons = computed(() => allCoupons.value.filter(c => c.status === 'UNUSED' && new Date(c.expiryDate || '2999-01-01') > new Date()).sort((a, b) => b.amount - a.amount))
const usedCoupons = computed(() => allCoupons.value.filter(c => c.status === 'USED' || new Date(c.expiryDate || '2999-01-01') <= new Date()).sort((a, b) => new Date(b.receiveTime) - new Date(a.receiveTime)))

const fetchCoupons = async () => {
    try {
        const username = store.currentUser?.username
        if (!username) { return }

        const res = await request.get(`/api/coupons/my?username=${username}`)

        if (res && Array.isArray(res)) {
            allCoupons.value = res
            store.myCoupons = res
        }
    } catch (e) {
        console.error("Error fetching coupons:", e)
        Swal.fire('加载失败', '无法获取优惠券列表，请检查网络', 'error')
        allCoupons.value = []
        store.myCoupons = []
    }
}

onMounted(() => {
    if (!store.currentUser) {
        store.showNotification('请先登录', 'error')
        router.push('/login')
        return
    }
    fetchCoupons()
})

const getTagColor = (tag) => {
    if (tag === 'EXCHANGE') return 'bg-purple-100 text-purple-700'
    return 'bg-blue-100 text-blue-700'
}

const getStatusColor = (status) => {
    if (status === 'UNUSED') return 'bg-green-600'
    if (status === 'USED') return 'bg-slate-400'
    return 'bg-red-500'
}

const useCoupon = (coupon) => {
    if (coupon.status !== 'UNUSED') return
    store.showNotification(`已选中优惠券: ${coupon.couponName}，可在结算页使用`, 'success')
    router.push('/cart')
}
</script>

<template>
    <div class="min-h-screen bg-slate-50 pb-20 relative overflow-hidden font-sans">

        <div class="container mx-auto px-4 py-8 relative z-10 max-w-6xl">
            <div class="flex items-center gap-4 mb-8">
                <button @click="router.back()"
                    class="w-10 h-10 rounded-full bg-white/80 backdrop-blur-md border border-slate-200 flex items-center justify-center hover:bg-slate-100 transition shadow-sm group">
                    <svg xmlns="http://www.w3.org/2000/svg"
                        class="w-5 h-5 text-slate-600 group-hover:-translate-x-1 transition" fill="none"
                        viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                    </svg>
                </button>
                <h1 class="text-2xl font-bold text-slate-800">我的优惠券</h1>
                <button @click="router.push('/points')"
                    class="ml-auto text-blue-600 text-sm font-bold bg-blue-50 px-3 py-1.5 rounded-full hover:bg-blue-100 transition">
                    积分兑换中心 🎁
                </button>
            </div>

            <div class="flex bg-white rounded-xl p-2 mb-6 shadow-md max-w-sm mx-auto border border-slate-100">
                <button @click="activeTab = 'available'"
                    :class="['flex-1 py-2 rounded-lg font-bold transition-all text-sm', activeTab === 'available' ? 'bg-indigo-600 text-white shadow-indigo-200 shadow-md' : 'text-slate-600 hover:bg-slate-50']">
                    可用优惠券 ({{ availableCoupons.length }})
                </button>
                <button @click="activeTab = 'used'"
                    :class="['flex-1 py-2 rounded-lg font-bold transition-all text-sm', activeTab === 'used' ? 'bg-indigo-600 text-white shadow-indigo-200 shadow-md' : 'text-slate-600 hover:bg-slate-50']">
                    已使用/已过期 ({{ usedCoupons.length }})
                </button>
            </div>

            <div class="max-w-4xl mx-auto space-y-5">

                <div v-if="activeTab === 'available'">
                    <div v-if="availableCoupons.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div v-for="coupon in availableCoupons" :key="coupon.id" @click="useCoupon(coupon)"
                            class="coupon-card shadow-lg hover:shadow-xl transition-all duration-300">
                            <div class="flex items-center justify-between p-4 border-b border-dashed border-white/30">
                                <span class="text-xs font-bold text-white/80">{{ coupon.couponName }}</span>
                                <span
                                    :class="['text-xs font-bold px-2 py-0.5 rounded-full', getTagColor(coupon.type)]">{{
                                        coupon.type === 'EXCHANGE' ? '积分兑换' : '系统发放' }}</span>
                            </div>
                            <div class="flex">
                                <div class="flex-1 p-6 flex flex-col justify-center">
                                    <span class="text-xs text-white/70 font-bold mb-1">优惠金额</span>
                                    <div class="text-white font-black tracking-tight flex items-end">
                                        <span class="text-4xl font-serif-sc">¥{{ coupon.amount }}</span>
                                    </div>
                                    <span class="text-xs text-white/70 mt-3">满 {{ coupon.minSpend || 0 }} 可用</span>
                                </div>
                                <div
                                    class="w-24 flex-shrink-0 flex flex-col justify-center items-center bg-white/10 rounded-r-2xl">
                                    <button
                                        class="bg-white text-indigo-600 font-bold text-sm px-3 py-1.5 rounded-full shadow-lg hover:bg-indigo-50 active:scale-95 transition-all">
                                        立即使用
                                    </button>
                                    <span v-if="coupon.expiryDate" class="text-[10px] text-white/70 mt-2">有效期至：{{ new
                                        Date(coupon.expiryDate).toLocaleDateString() }}</span>
                                    <span v-else class="text-[10px] text-white/70 mt-2">长期有效</span>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div v-else class="empty-state">
                        <div class="text-6xl mb-4 opacity-20">🎫</div>
                        <p class="text-slate-500 font-medium">暂无可用优惠券</p>
                        <button @click="router.push('/points')"
                            class="mt-4 text-indigo-600 font-bold hover:underline">去积分中心兑换</button>
                    </div>
                </div>

                <div v-else>
                    <div v-if="usedCoupons.length > 0" class="space-y-4">
                        <div v-for="coupon in usedCoupons" :key="coupon.id"
                            class="coupon-card-used bg-white border border-slate-100 shadow-sm opacity-70">
                            <div class="flex items-center justify-between p-4 border-b border-slate-100">
                                <span class="text-xs font-bold text-slate-700">{{ coupon.couponName }}</span>
                                <span
                                    :class="['text-xs font-bold px-2 py-0.5 rounded-full', getTagColor(coupon.type)]">{{
                                        coupon.type === 'EXCHANGE' ? '积分兑换' : '系统发放' }}</span>
                            </div>
                            <div class="flex">
                                <div class="flex-1 p-6 flex flex-col justify-center">
                                    <span class="text-xs text-slate-500 font-bold mb-1">优惠金额</span>
                                    <div class="text-slate-800 font-black tracking-tight flex items-end">
                                        <span class="text-4xl font-serif-sc">¥{{ coupon.amount }}</span>
                                    </div>
                                    <span class="text-xs text-slate-400 mt-3">满 {{ coupon.minSpend || 0 }} 可用</span>
                                </div>
                                <div
                                    class="w-24 flex-shrink-0 flex flex-col justify-center items-center bg-slate-50 rounded-r-2xl relative">
                                    <div class="absolute inset-0 flex items-center justify-center">
                                        <span
                                            :class="['px-3 py-1 text-xs font-bold rounded-full text-white', getStatusColor(coupon.status)]">
                                            {{ coupon.status === 'USED' ? '已使用' : '已过期' }}
                                        </span>
                                    </div>

                                    <span v-if="coupon.expiryDate" class="text-[10px] text-slate-400 mt-2">有效期至：{{ new
                                        Date(coupon.expiryDate).toLocaleDateString() }}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div v-else class="empty-state">
                        <div class="text-6xl mb-4 opacity-20">🗓️</div>
                        <p class="text-slate-500 font-medium">暂无已使用或已过期优惠券记录</p>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.coupon-card {
    @apply bg-indigo-500 rounded-3xl cursor-pointer relative overflow-hidden text-white;
    background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
}

.coupon-card:before {
    content: '';
    position: absolute;
    top: 50%;
    left: -10px;
    width: 20px;
    height: 20px;
    background-color: #f8fafc;
    border-radius: 50%;
    transform: translateY(-50%);
    z-index: 10;
}

.coupon-card:after {
    content: '';
    position: absolute;
    top: 50%;
    right: -10px;
    width: 20px;
    height: 20px;
    background-color: #f8fafc;
    border-radius: 50%;
    transform: translateY(-50%);
    z-index: 10;
}

.coupon-card-used {
    @apply rounded-3xl relative overflow-hidden;
}

.empty-state {
    @apply flex flex-col items-center justify-center bg-white rounded-3xl border border-dashed border-slate-200 p-12 min-h-[250px] shadow-sm;
}
</style>