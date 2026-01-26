<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '../store.js'
import { request } from '@/utils/request'

const router = useRouter()
const logs = ref([])
const loading = ref(true)

const fetchLogs = async () => {
    try {
        const res = await request.get('/api/wallet/logs')
        logs.value = res || []
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

const formatDate = (iso) => new Date(iso).toLocaleString()

onMounted(() => {
    if (!store.currentUser) {
        router.push('/login')
        return
    }
    fetchLogs()
})
</script>

<template>
    <div class="min-h-screen bg-slate-50 font-sans pb-20">
        <header class="bg-slate-900 text-white pt-12 pb-24 relative overflow-hidden">
            <div class="absolute inset-0 bg-gradient-to-br from-indigo-900 to-slate-900"></div>
            <div class="absolute -right-20 -top-20 w-96 h-96 bg-indigo-500/20 rounded-full blur-3xl"></div>
            
            <div class="container mx-auto px-6 relative z-10">
                <div class="flex iems-center gap-4 mb-8 text-slate-400 cursor-pointer hover:text-white transition" @click="router.back()">
                     <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                    </svg>
                    <span>返回</span>
                </div>
                
                <div class="flex justify-between items-end">
                    <div>
                        <div class="text-sm font-bold text-slate-400 tracking-widest uppercase mb-2">My Balance</div>
                        <div class="text-5xl font-black font-serif-sc tracking-tight flex items-baseline gap-2">
                             <span class="text-2xl opacity-60">¥</span>{{ parseFloat(store.currentUser?.balance || 0).toFixed(2) }}
                        </div>
                    </div>
                    <div class="hidden sm:block">
                        <button class="bg-white/10 hover:bg-white/20 border border-white/20 text-white px-6 py-2 rounded-full text-sm font-bold transition backdrop-blur-md">
                            充值 / Recharge
                        </button>
                    </div>
                </div>
            </div>
        </header>

        <main class="container mx-auto px-4 -mt-16 relative z-20">
            <div class="bg-white rounded-3xl shadow-xl border border-slate-100 min-h-[500px] overflow-hidden">
                <div class="p-6 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
                    <h2 class="font-bold text-slate-800 flex items-center gap-2">
                        <span class="w-2 h-6 bg-indigo-500 rounded-full"></span>
                        资金明细
                    </h2>
                     <button class="text-slate-400 hover:text-indigo-600 transition text-sm" @click="fetchLogs">
                        刷新
                    </button>
                </div>

                <div v-if="loading" class="p-12 text-center text-slate-400">
                    加载中...
                </div>

                <div v-else-if="logs.length === 0" class="p-16 text-center text-slate-400 flex flex-col items-center">
                    <div class="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mb-4 text-2xl">🍃</div>
                    暂无资金记录
                </div>

                <div v-else class="divide-y divide-slate-50">
                    <div v-for="log in logs" :key="log.id" class="p-6 hover:bg-slate-50/80 transition-colors group flex items-center justify-between">
                         <div class="flex items-center gap-4">
                            <div class="w-12 h-12 rounded-2xl flex items-center justify-center text-xl shadow-sm border border-slate-100"
                                :class="{
                                    'bg-green-50 text-green-600': log.type === 1,
                                    'bg-orange-50 text-orange-600': log.type === 2,
                                    'bg-blue-50 text-blue-600': log.type === 3
                                }">
                                {{ log.type === 1 ? '↩️' : (log.type === 2 ? '🛍️' : '💳') }}
                            </div>
                            <div>
                                <div class="font-bold text-slate-800 mb-1">
                                    {{ log.type === 1 ? '退款到账' : (log.type === 2 ? '消费支出' : '充值入账') }}
                                </div>
                                <div class="text-xs text-slate-400">{{ log.description || '无备注' }}</div>
                            </div>
                         </div>
                         <div class="text-right">
                            <div class="font-bold font-mono text-lg" 
                                :class="log.amount > 0 ? 'text-green-600' : 'text-slate-900'">
                                {{ log.amount > 0 ? '+' : '' }}{{ log.amount }}
                            </div>
                            <div class="text-xs text-slate-300 mt-1">{{ formatDate(log.createTime) }}</div>
                         </div>
                    </div>
                </div>
            </div>
        </main>
    </div>
</template>
