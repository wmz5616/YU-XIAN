<script setup>
import { ref, onMounted } from 'vue';
import { request } from '@/utils/request';
import Swal from 'sweetalert2';

// 获取当前用户
const userStr = localStorage.getItem('yuxian_user');
const currentUser = userStr ? JSON.parse(userStr) : null;

const activeTab = ref('market');
const marketCoupons = ref([]);
const myCoupons = ref([]);
const loading = ref(false);

const fetchMarket = async () => {
    if (!currentUser) return;
    loading.value = true;
    try {
        const res = await request.get(`/api/coupons/market?username=${currentUser.username}`);
        marketCoupons.value = res || [];
    } catch (e) { console.error(e); } finally { loading.value = false; }
};

const fetchMy = async () => {
    if (!currentUser) return;
    loading.value = true;
    try {
        const res = await request.get(`/api/coupons/my?username=${currentUser.username}`);
        myCoupons.value = res || [];
    } catch (e) { console.error(e); } finally { loading.value = false; }
};

const handleReceive = async (id) => {
    try {
        await request.post(`/api/coupons/${id}/receive`, { username: currentUser.username });
        Swal.fire({ icon: 'success', title: '🎉 领取成功!', timer: 1500, showConfirmButton: false });
        fetchMarket();
    } catch (e) { Swal.fire('领取失败', e.message, 'error'); }
};

const switchTab = (tab) => {
    activeTab.value = tab;
    if (tab === 'market') fetchMarket(); else fetchMy();
};

onMounted(() => {
    if (!currentUser) return;
    fetchMarket();
});
</script>

<template>
    <div class="min-h-screen bg-[#F8FAFC] py-8 px-4 font-sans">
        <div class="max-w-4xl mx-auto">

            <div class="flex items-center justify-between mb-8">
                <h1 class="text-2xl font-bold text-slate-800 flex items-center gap-2">
                    优惠券中心
                </h1>
                <router-link to="/" class="text-sm text-blue-600 hover:text-blue-800 font-medium transition">←
                    返回商城</router-link>
            </div>

            <div class="flex space-x-1 bg-slate-200 p-1 rounded-xl w-fit mb-8 shadow-inner">
                <button @click="switchTab('market')"
                    :class="['px-6 py-2 rounded-lg text-sm font-bold transition-all duration-300',
                        activeTab === 'market' ? 'bg-white text-blue-600 shadow-sm transform scale-105' : 'text-slate-500 hover:text-slate-700']">
                    领券中心
                </button>
                <button @click="switchTab('my')"
                    :class="['px-6 py-2 rounded-lg text-sm font-bold transition-all duration-300',
                        activeTab === 'my' ? 'bg-white text-blue-600 shadow-sm transform scale-105' : 'text-slate-500 hover:text-slate-700']">
                    我的卡包
                </button>
            </div>

            <div v-if="loading" class="py-20 text-center">
                <div class="animate-spin text-3xl mb-2 text-blue-500">↻</div>
                <p class="text-slate-400 text-sm">正在加载优惠券...</p>
            </div>

            <div v-else-if="activeTab === 'market'" class="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div v-for="c in marketCoupons" :key="c.id"
                    class="bg-white rounded-xl shadow-sm border border-slate-100 overflow-hidden flex relative group hover:-translate-y-1 hover:shadow-lg transition-all duration-300">

                    <div
                        class="w-32 bg-gradient-to-br from-blue-500 to-blue-600 text-white flex flex-col items-center justify-center p-4 relative">
                        <div class="text-3xl font-black font-serif">¥{{ c.amount }}</div>
                        <div class="text-xs opacity-90 mt-1 font-medium">满 {{ c.minSpend }} 可用</div>
                        <div class="absolute -right-1 top-0 bottom-0 w-2 bg-white"
                            style="mask-image: radial-gradient(circle at 0 6px, transparent 0, transparent 3px, black 3px); mask-size: 10px 12px;">
                        </div>
                    </div>

                    <div class="flex-1 p-5 flex flex-col justify-between">
                        <div>
                            <h3 class="font-bold text-slate-800 text-lg">{{ c.name }}</h3>
                            <div class="mt-3 flex items-center justify-between text-xs text-slate-400 mb-1">
                                <span>已抢 {{ (c.percent * 100).toFixed(0) }}%</span>
                            </div>
                            <div class="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
                                <div class="bg-blue-500 h-full rounded-full transition-all duration-1000"
                                    :style="{ width: (c.percent * 100) + '%' }"></div>
                            </div>
                            <p class="text-[10px] text-slate-400 mt-2">有效期至: {{ c.validUntil }}</p>
                        </div>

                        <div class="mt-3 flex justify-end">
                            <button v-if="!c.hasReceived" @click="handleReceive(c.id)"
                                class="bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold px-5 py-2 rounded-full shadow-lg shadow-blue-200 active:scale-95 transition-all">
                                立即领取
                            </button>
                            <button v-else disabled
                                class="bg-slate-100 text-slate-400 text-xs font-bold px-5 py-2 rounded-full cursor-not-allowed border border-slate-200">
                                已放入卡包
                            </button>
                        </div>
                    </div>
                </div>

                <div v-if="marketCoupons.length === 0"
                    class="col-span-2 text-center py-20 bg-white rounded-2xl border border-slate-100 border-dashed">
                    <p class="text-slate-400">暂无可领取的优惠券</p>
                </div>
            </div>

            <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div v-for="c in myCoupons" :key="c.id"
                    class="bg-white rounded-xl shadow-sm border border-slate-100 overflow-hidden flex relative transition-all hover:shadow-md">
                    <div :class="['w-32 flex flex-col items-center justify-center p-4 text-white relative',
                        c.status === 'UNUSED' ? 'bg-gradient-to-br from-orange-400 to-orange-500' : 'bg-slate-400']">
                        <div class="text-3xl font-black font-serif">¥{{ c.amount }}</div>
                        <div class="text-xs opacity-90 mt-1">满 {{ c.minSpend }} 可用</div>
                        <div class="absolute -right-1 top-0 bottom-0 w-2 bg-white"
                            style="mask-image: radial-gradient(circle at 0 6px, transparent 0, transparent 3px, black 3px); mask-size: 10px 12px;">
                        </div>
                    </div>

                    <div class="flex-1 p-5 relative">
                        <h3 class="font-bold text-slate-800 text-lg">{{ c.couponName }}</h3>
                        <p class="text-xs text-slate-400 mt-1">领取时间: {{ new Date(c.receiveTime).toLocaleDateString() }}
                        </p>

                        <div v-if="c.status === 'USED'"
                            class="absolute right-4 bottom-2 text-5xl font-black text-slate-100 rotate-[-15deg] select-none pointer-events-none">
                            USED
                        </div>
                        <div v-else-if="c.status === 'UNUSED'" class="mt-4">
                            <span
                                class="bg-orange-50 text-orange-600 px-2 py-1 rounded text-xs font-bold border border-orange-100">
                                待使用
                            </span>
                        </div>
                    </div>
                </div>

                <div v-if="myCoupons.length === 0"
                    class="col-span-2 text-center py-20 bg-white rounded-2xl border border-slate-100 border-dashed">
                    <p class="text-slate-400">您的卡包空空如也，快去领券中心看看吧</p>
                </div>
            </div>

        </div>
    </div>
</template>