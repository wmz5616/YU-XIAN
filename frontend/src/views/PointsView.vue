<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '../store.js'
import Swal from 'sweetalert2'

const router = useRouter()
const loading = ref(false)
const isSigned = ref(false)

// 模拟积分明细数据 (实际开发中建议从 API 获取)
const pointLogs = ref([
    { id: 1, type: 'income', title: '订单完成奖励', amount: 190, time: '2025-12-15 14:30' },
    { id: 2, type: 'income', title: '每日签到', amount: 10, time: '2025-12-15 09:00' },
])

// 可兑换列表
const exchangeableCoupons = ref([
    { id: 101, amount: 5, cost: 500, name: '无门槛立减券', color: 'from-orange-400 to-red-500' },
    { id: 102, amount: 20, cost: 1800, name: '满200可用', color: 'from-blue-400 to-indigo-500' },
    { id: 103, amount: 50, cost: 4000, name: '海鲜盛宴专享', color: 'from-purple-400 to-pink-500' },
])

onMounted(async () => {
    if (!store.currentUser) {
        store.showNotification('请先登录', 'warning')
        router.push('/login')
        return
    }

    // 检查今日是否已签到 (这里用本地缓存模拟，实际应查后端)
    const lastSignDate = localStorage.getItem(`sign_date_${store.currentUser.username}`)
    const today = new Date().toLocaleDateString()
    if (lastSignDate === today) {
        isSigned.value = true
    }
})

// === ✅ 核心逻辑：签到 (含持久化) ===
const handleSignIn = async () => {
    if (isSigned.value) return

    const bonus = 20
    loading.value = true

    try {
        // 1. 更新本地状态 (前后端同步)
        // 确保 points 是数字
        const currentPoints = Number(store.currentUser.points) || 0
        store.currentUser.points = currentPoints + bonus

        // 2. 触发 store 的持久化保存 (关键步骤！)
        store.login(store.currentUser)

        // 3. 记录签到状态
        localStorage.setItem(`sign_date_${store.currentUser.username}`, new Date().toLocaleDateString())
        isSigned.value = true

        // 4. 更新日志视图
        pointLogs.value.unshift({
            id: Date.now(),
            type: 'income',
            title: '每日签到',
            amount: bonus,
            time: new Date().toLocaleString()
        })

        Swal.fire({
            icon: 'success',
            title: '签到成功',
            text: `获得 ${bonus} 积分`,
            timer: 1500,
            showConfirmButton: false
        })

    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

// === ✅ 核心逻辑：兑换 (含持久化) ===
const handleExchange = async (coupon) => {
    const currentPoints = Number(store.currentUser.points) || 0

    if (currentPoints < coupon.cost) {
        Swal.fire({
            icon: 'error',
            title: '积分不足',
            text: `还需要 ${coupon.cost - currentPoints} 积分`,
            confirmButtonColor: '#fbbf24'
        })
        return
    }

    const result = await Swal.fire({
        title: '确认兑换?',
        html: `消耗 <b style="color:#f59e0b">${coupon.cost}</b> 积分<br>兑换 <b>${coupon.name}</b>`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#F59E0B',
        confirmButtonText: '立即兑换',
        cancelButtonText: '再想想'
    })

    if (result.isConfirmed) {
        loading.value = true
        try {
            // 1. ✅ 关键：更新用户积分并写入 LocalStorage
            store.currentUser.points = currentPoints - coupon.cost
            store.login(store.currentUser) // 这一步会将最新的 user 对象写入 localStorage

            // 2. ✅ 关键：将优惠券真正存入 Store
            store.addCoupon({
                name: coupon.name,
                amount: coupon.amount,
                minSpend: coupon.amount * 10, // 假设门槛是面值10倍
                type: 'EXCHANGE' // 标记来源
            })

            // 3. 更新日志
            pointLogs.value.unshift({
                id: Date.now(),
                type: 'expense',
                title: `兑换 ${coupon.name}`,
                amount: -coupon.cost,
                time: new Date().toLocaleString()
            })

            Swal.fire('兑换成功', '优惠券已发放至您的账户', 'success')

        } catch (e) {
            Swal.fire('兑换失败', '网络异常，请稍后重试', 'error')
        } finally {
            loading.value = false
        }
    }
}
</script>

<template>
    <div class="min-h-screen bg-[#F8FAFC] pb-20 relative font-sans">

        <div
            class="absolute top-0 left-0 w-full h-[400px] bg-gradient-to-b from-amber-50 to-transparent pointer-events-none">
        </div>
        <div
            class="absolute top-0 right-0 w-full h-[300px] bg-[url('https://www.transparenttextures.com/patterns/cubes.png')] opacity-20 pointer-events-none">
        </div>

        <div class="container mx-auto px-4 py-8 relative z-10 max-w-5xl">

            <div class="flex items-center gap-4 mb-8">
                <button @click="router.back()"
                    class="w-10 h-10 rounded-full bg-white border border-slate-200 flex items-center justify-center hover:bg-slate-100 transition shadow-sm group">
                    <svg xmlns="http://www.w3.org/2000/svg"
                        class="w-5 h-5 text-slate-600 group-hover:-translate-x-1 transition" fill="none"
                        viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                    </svg>
                </button>
                <h1 class="text-2xl font-bold text-slate-800">会员积分中心</h1>
            </div>

            <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">

                <div class="lg:col-span-1 space-y-6">
                    <div
                        class="bg-gradient-to-br from-amber-400 to-orange-500 rounded-[32px] p-8 text-white shadow-2xl shadow-orange-200 relative overflow-hidden transform hover:scale-[1.02] transition-transform duration-300">
                        <div class="absolute -right-6 -top-6 w-32 h-32 bg-white/20 rounded-full blur-2xl"></div>
                        <div class="absolute bottom-0 right-0 text-9xl opacity-10 rotate-12">💎</div>

                        <div class="relative z-10">
                            <div class="flex items-center gap-3 mb-2 opacity-90">
                                <div class="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center text-sm">
                                    👑</div>
                                <span class="font-bold tracking-widest text-xs uppercase">Premium Member</span>
                            </div>

                            <div class="mt-6">
                                <div class="text-sm opacity-80 font-medium mb-1">可用积分</div>
                                <div class="text-5xl font-black font-serif-sc tracking-tight">{{
                                    store.currentUser?.points || 0 }}</div>
                            </div>

                            <div class="mt-8 pt-6 border-t border-white/20 flex justify-between items-center">
                                <div class="text-xs opacity-80">再积 <span class="font-bold">896</span> 分升级黑金会员</div>
                                <button @click="handleSignIn" :disabled="isSigned || loading"
                                    class="bg-white text-orange-600 px-5 py-2 rounded-full text-xs font-bold shadow-lg hover:bg-orange-50 transition active:scale-95 disabled:opacity-80 disabled:cursor-not-allowed">
                                    {{ isSigned ? '今日已签' : '立即签到' }}
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class="bg-white rounded-3xl p-6 shadow-sm border border-slate-100">
                        <h3 class="font-bold text-slate-800 mb-4 flex justify-between items-center">
                            <span>积分明细</span>
                            <span class="text-xs text-slate-400 font-normal">最近记录</span>
                        </h3>
                        <div class="space-y-4 max-h-[400px] overflow-y-auto custom-scrollbar pr-2">
                            <div v-for="log in pointLogs" :key="log.id"
                                class="flex justify-between items-center p-3 hover:bg-slate-50 rounded-xl transition-colors">
                                <div class="flex items-center gap-3">
                                    <div
                                        :class="['w-10 h-10 rounded-full flex items-center justify-center text-lg', log.type === 'income' ? 'bg-amber-50 text-amber-500' : 'bg-slate-100 text-slate-500']">
                                        {{ log.type === 'income' ? '+' : '-' }}
                                    </div>
                                    <div>
                                        <div class="text-sm font-bold text-slate-700">{{ log.title }}</div>
                                        <div class="text-[10px] text-slate-400">{{ log.time }}</div>
                                    </div>
                                </div>
                                <div
                                    :class="['font-mono font-bold', log.type === 'income' ? 'text-amber-500' : 'text-slate-900']">
                                    {{ log.type === 'income' ? '+' : '' }}{{ log.amount }}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="lg:col-span-2">
                    <div class="bg-white/60 backdrop-blur-xl rounded-[32px] p-8 border border-white shadow-sm">
                        <h2 class="text-xl font-bold text-slate-800 mb-6 flex items-center gap-2">
                            <span class="text-2xl">🎁</span> 积分兑换好礼
                        </h2>

                        <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">
                            <div v-for="item in exchangeableCoupons" :key="item.id"
                                class="group bg-white rounded-2xl p-1 shadow-sm hover:shadow-xl transition-all duration-300 border border-slate-100">
                                <div
                                    class="relative p-5 flex flex-col h-full bg-slate-50 rounded-xl overflow-hidden group-hover:bg-white transition-colors">

                                    <div :class="`absolute top-0 left-0 w-full h-1.5 bg-gradient-to-r ${item.color}`">
                                    </div>

                                    <div class="flex justify-between items-start mb-4">
                                        <div
                                            :class="`w-12 h-12 rounded-xl bg-gradient-to-br ${item.color} flex items-center justify-center text-white text-xl shadow-lg`">
                                            🎫
                                        </div>
                                        <div class="text-right">
                                            <div class="text-2xl font-black text-slate-800 font-serif-sc">{{ item.amount
                                                }}<span class="text-sm align-top ml-0.5">元</span></div>
                                            <div class="text-[10px] text-slate-400 uppercase tracking-wider">Coupon
                                            </div>
                                        </div>
                                    </div>

                                    <h3 class="font-bold text-slate-700 text-lg mb-1">{{ item.name }}</h3>
                                    <p class="text-xs text-slate-400 mb-6">全场通用 · 有效期7天</p>

                                    <div
                                        class="mt-auto flex items-center justify-between pt-4 border-t border-slate-200/50">
                                        <div class="text-amber-500 font-bold font-mono flex items-center gap-1">
                                            {{ item.cost }} <span class="text-[10px] text-amber-400 font-sans">积分</span>
                                        </div>
                                        <button @click="handleExchange(item)"
                                            class="px-4 py-2 bg-slate-900 text-white text-xs font-bold rounded-lg hover:bg-amber-500 transition-colors shadow-lg shadow-slate-200 group-hover:shadow-amber-200/50 transform active:scale-95">
                                            立即兑换
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="mt-8 p-8 border-2 border-dashed border-slate-200 rounded-2xl text-center">
                            <div class="text-4xl mb-3 grayscale opacity-30">🦑</div>
                            <p class="text-slate-400 text-sm font-medium">更多实物礼品即将上架...</p>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
    width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-track {
    background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
    background-color: #cbd5e1;
    border-radius: 4px;
}
</style>