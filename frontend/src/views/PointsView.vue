<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '../store.js'
import { request } from '@/utils/request' // ✅ 引入 request
import Swal from 'sweetalert2'

const router = useRouter()
const loading = ref(false)
const isSigned = ref(false)

// 模拟积分明细数据 (如果需要持久化，也需要后端提供接口，这里暂时保持模拟或从Store读)
const pointLogs = ref(store.pointLogs || [])

// 可兑换列表
const exchangeableCoupons = ref([
    { id: 101, amount: 5, cost: 500, name: '无门槛立减券', color: 'from-orange-400 to-red-500' },
    { id: 102, amount: 20, cost: 1800, name: '满200可用', color: 'from-blue-400 to-indigo-500' },
    { id: 103, amount: 50, cost: 4000, name: '海鲜盛宴专享', color: 'from-purple-400 to-pink-500' },
    { id: 104, amount: 100, cost: 8000, name: '至尊VIP礼券', color: 'from-slate-700 to-slate-900' },
])

onMounted(() => {
    // 检查签到状态
    const today = new Date().toLocaleDateString()
    const lastSign = localStorage.getItem(`sign_date_${store.currentUser?.username}`)
    if (lastSign === today) isSigned.value = true
})

// 兑换逻辑
const handleExchange = async (item) => {
    if (!store.currentUser) {
        Swal.fire('请先登录', '', 'warning');
        router.push('/login');
        return;
    }

    const currentPoints = store.currentUser.points || 0;
    if (currentPoints < item.cost) {
        Swal.fire('积分不足', `还差 ${item.cost - currentPoints} 积分`, 'error');
        return;
    }

    const confirm = await Swal.fire({
        title: '确认兑换?',
        text: `消耗 ${item.cost} 积分兑换 [${item.name}]`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#f59e0b',
        confirmButtonText: '确定'
    });

    if (confirm.isConfirmed) {
        loading.value = true;
        try {
            // ✅ 核心修复：调用后端 API 进行兑换
            const res = await request.post('/api/coupons/exchange', {
                username: store.currentUser.username,
                amount: item.amount,
                cost: item.cost,
                name: item.name
            });

            if (res && res.success) {
                // 1. 更新本地 Store 的积分 (后端返回了最新积分)
                store.currentUser.points = res.points;
                // 触发保存到 LocalStorage
                store.login(store.currentUser);

                // 2. 记录本地日志 (可选，后端如果没做日志接口，前端先记着)
                store.addPointLog({
                    type: 'expense',
                    title: `兑换: ${item.name}`,
                    amount: item.cost
                });

                // 3. 将新券加入本地缓存，防止不刷新页面看不到
                // 注意：这里只是为了立即显示，实际上数据已经进数据库了
                store.addCoupon({
                    name: item.name,
                    amount: item.amount
                });

                Swal.fire('兑换成功', '优惠券已发放', 'success');
            }
        } catch (e) {
            Swal.fire('兑换失败', e.message || '系统繁忙', 'error');
        } finally {
            loading.value = false;
        }
    }
}

// 签到逻辑 (暂时保持简单版，如果需要持久化也需要后端支持)
const handleSignIn = () => {
    if (!store.currentUser) return router.push('/login');

    isSigned.value = true;
    localStorage.setItem(`sign_date_${store.currentUser.username}`, new Date().toLocaleDateString());

    // 增加积分 (建议后续也改为后端接口)
    const reward = 10;
    store.currentUser.points = (store.currentUser.points || 0) + reward;
    store.login(store.currentUser); // 保存

    store.addPointLog({ type: 'income', title: '每日签到', amount: reward });
    Swal.fire('签到成功', `获得 ${reward} 积分`, 'success');
}
</script>

<template>
    <div class="min-h-screen bg-slate-50 pb-20 pt-20">
        <div class="max-w-4xl mx-auto px-4">
            <div
                class="bg-gradient-to-r from-slate-900 to-slate-800 rounded-3xl p-8 text-white shadow-xl mb-8 relative overflow-hidden group">
                <div
                    class="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full -mr-16 -mt-16 blur-3xl group-hover:bg-white/10 transition-all duration-700">
                </div>

                <div class="relative z-10 flex justify-between items-end">
                    <div>
                        <p class="text-slate-400 mb-2 text-sm uppercase tracking-wider">当前可用积分</p>
                        <h1 class="text-5xl font-black font-mono tracking-tight">
                            {{ store.currentUser?.points || 0 }}
                        </h1>
                    </div>
                    <button @click="handleSignIn" :disabled="isSigned"
                        class="px-6 py-2.5 bg-amber-400 hover:bg-amber-300 active:scale-95 text-slate-900 font-bold rounded-full transition-all flex items-center gap-2 shadow-lg shadow-amber-900/20 disabled:opacity-50 disabled:cursor-not-allowed">
                        <span>{{ isSigned ? '今日已签' : '签到领分' }}</span>
                    </button>
                </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
                <div class="md:col-span-1 space-y-6">
                    <h2 class="text-xl font-bold text-slate-800 flex items-center gap-2">
                        <span class="w-1 h-6 bg-amber-400 rounded-full"></span>
                        最近明细
                    </h2>

                    <div class="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 min-h-[300px]">
                        <ul class="space-y-4">
                            <li v-for="log in store.pointLogs" :key="log.id"
                                class="flex justify-between items-center py-3 border-b border-slate-50 last:border-0">
                                <div>
                                    <p class="font-bold text-slate-700 text-sm">{{ log.title }}</p>
                                    <p class="text-xs text-slate-400 mt-0.5">{{ log.time }}</p>
                                </div>
                                <span :class="log.type === 'income' ? 'text-green-500' : 'text-orange-500'"
                                    class="font-mono font-bold">
                                    {{ log.type === 'income' ? '+' : '-' }}{{ log.amount }}
                                </span>
                            </li>
                        </ul>
                    </div>
                </div>

                <div class="md:col-span-2 space-y-6">
                    <h2 class="text-xl font-bold text-slate-800 flex items-center gap-2">
                        <span class="w-1 h-6 bg-blue-500 rounded-full"></span>
                        好券兑换
                    </h2>

                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <div v-for="item in exchangeableCoupons" :key="item.id"
                            class="group bg-white rounded-2xl p-5 border border-slate-100 hover:border-amber-200 hover:shadow-xl hover:shadow-amber-100/50 transition-all duration-300 relative overflow-hidden">

                            <div
                                :class="`absolute top-0 right-0 w-24 h-24 bg-gradient-to-br ${item.color} opacity-10 rounded-bl-full group-hover:scale-110 transition-transform`">
                            </div>

                            <div class="relative z-10">
                                <div
                                    :class="`w-12 h-12 rounded-xl bg-gradient-to-br ${item.color} flex items-center justify-center text-white font-bold text-lg mb-4 shadow-lg`">
                                    ¥{{ item.amount }}
                                </div>

                                <h3 class="font-bold text-slate-800 text-lg mb-1">{{ item.name }}</h3>
                                <p class="text-xs text-slate-400 mb-6">满 ¥{{ item.amount * 10 }} 可用</p>

                                <div class="flex items-center justify-between">
                                    <div class="text-amber-500 font-bold font-mono flex items-center gap-1">
                                        {{ item.cost }} <span class="text-[10px] text-amber-400 font-sans">积分</span>
                                    </div>
                                    <button @click="handleExchange(item)" :disabled="loading"
                                        class="px-4 py-2 bg-slate-900 text-white text-xs font-bold rounded-lg hover:bg-amber-500 transition-colors shadow-lg shadow-slate-200 group-hover:shadow-amber-200/50 transform active:scale-95 disabled:opacity-50">
                                        {{ loading ? '...' : '立即兑换' }}
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
</template>