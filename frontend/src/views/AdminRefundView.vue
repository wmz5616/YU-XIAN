<script setup>
import { ref, onMounted } from 'vue'
import { request } from '@/utils/request'
import Swal from 'sweetalert2'

const refunds = ref([])
const loading = ref(false)

const adminUser = JSON.parse(localStorage.getItem('yuxian_user') || '{}');
const adminUsername = adminUser.username || 'SystemAdmin';

const fetchRefunds = async () => {
    loading.value = true
    try {
        const res = await request.get('/api/orders/admin/refunds')

        refunds.value = res || []
    } catch (e) {
        console.error("加载售后列表失败:", e)
    } finally {
        loading.value = false
    }
}

const approve = async (order) => {
    const result = await Swal.fire({
        title: '同意退款?',
        html: `<p>订单金额 <span class="text-xl font-bold text-green-600">¥${order.totalPrice}</span> 将原路退回给用户</p>`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#10B981',
        confirmButtonText: '确认同意',
        cancelButtonText: '取消'
    })

    if (result.isConfirmed) {
        try {
            await request.post(`/api/orders/admin/refunds/${order.id}/audit`, {
                pass: true,
                reason: '审核通过，已同意退款。',
                adminUsername: adminUsername
            });

            Swal.fire('已处理', '订单已变更为退款成功', 'success')
            await fetchRefunds()
        } catch (e) {
            Swal.fire('处理失败', e.message || '系统繁忙', 'error')
        }
    }
}

const reject = async (order) => {
    const { value: reason } = await Swal.fire({
        title: '驳回退款申请',
        input: 'textarea',
        inputLabel: `请输入驳回订单 #${20250000 + order.id} 的原因`,
        inputPlaceholder: '退款不通过的具体原因...',
        inputValidator: (value) => {
            if (!value) return '驳回原因不能为空！'
        },
        showCancelButton: true,
        confirmButtonColor: '#EF4444',
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消'
    })

    if (reason) {
        try {
            await request.post(`/api/orders/admin/refunds/${order.id}/audit`, {
                pass: false,
                reason: reason,
                adminUsername: adminUsername
            });

            Swal.fire('已驳回', `订单已恢复为“已送达”，原因：${reason}`, 'success')
            await fetchRefunds()
        } catch (e) {
            Swal.fire('处理失败', e.message || '系统繁忙', 'error')
        }
    }
}

onMounted(() => {
    fetchRefunds()
})

const formatDate = (iso) => new Date(iso).toLocaleString()
</script>

<template>
    <div class="p-6 bg-slate-50 min-h-screen">
        <div class="max-w-7xl mx-auto">
            <header class="flex justify-between items-center mb-8 border-b border-slate-200 pb-6">
                <div>
                    <h1 class="text-3xl font-black text-slate-800 tracking-tight">
                        售后管理中心
                    </h1>
                    <p class="text-slate-500 text-sm mt-1">处理用户的退款与退货申请</p>
                </div>
                <div class="bg-indigo-50 px-4 py-2 rounded-2xl border border-indigo-100">
                    <span class="text-indigo-600 font-bold text-sm">
                        待处理任务: {{ refunds.length }} 笔
                    </span>
                </div>
            </header>

            <div v-if="loading" class="flex flex-col items-center justify-center py-20 text-slate-400">
                <div class="w-12 h-12 border-4 border-indigo-100 border-t-indigo-600 rounded-full animate-spin mb-4">
                </div>
                <p class="font-medium">同步数据中...</p>
            </div>

            <div v-else
                class="bg-white rounded-[24px] shadow-xl shadow-slate-200/50 overflow-hidden border border-slate-100 animate-fade-in">
                <table class="min-w-full divide-y divide-slate-100">
                    <thead class="bg-slate-50/80">
                        <tr>
                            <th class="px-6 py-4 text-left text-xs font-bold text-slate-400 uppercase tracking-wider">
                                订单号</th>
                            <th class="px-6 py-4 text-left text-xs font-bold text-slate-400 uppercase tracking-wider">
                                申请用户</th>
                            <th class="px-6 py-4 text-left text-xs font-bold text-slate-400 uppercase tracking-wider">
                                商品信息</th>
                            <th class="px-6 py-4 text-left text-xs font-bold text-slate-400 uppercase tracking-wider">
                                申请金额</th>
                            <th class="px-6 py-4 text-right text-xs font-bold text-slate-400 uppercase tracking-wider">
                                操作决策</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-slate-50">
                        <tr v-for="order in refunds" :key="order.id"
                            class="hover:bg-indigo-50/30 transition-colors group">
                            <td class="px-6 py-5 whitespace-nowrap">
                                <span
                                    class="px-3 py-1 bg-slate-100 text-slate-600 rounded-lg font-mono text-xs font-bold">
                                    #{{ 20250000 + order.id }}
                                </span>
                            </td>
                            <td class="px-6 py-5">
                                <div class="flex flex-col">
                                    <span class="text-sm font-bold text-slate-700">{{ order.username }}</span>
                                    <span class="text-[10px] text-slate-400 mt-0.5">{{ formatDate(order.createTime)
                                        }}</span>
                                </div>
                            </td>
                            <td class="px-6 py-5">
                                <p class="text-sm text-slate-600 line-clamp-1 font-medium" :title="order.productNames">
                                    {{ order.productNames }}
                                </p>
                            </td>
                            <td class="px-6 py-5 whitespace-nowrap">
                                <span class="text-lg font-black text-red-500 font-serif-sc">
                                    <small class="text-xs font-normal">¥</small>{{ order.totalPrice.toFixed(2) }}
                                </span>
                            </td>
                            <td class="px-6 py-5 text-right">
                                <div
                                    class="flex justify-end gap-3 opacity-0 group-hover:opacity-100 transition-opacity">
                                    <button @click="reject(order)"
                                        class="px-4 py-2 border-2 border-slate-100 text-slate-500 rounded-xl hover:bg-red-50 hover:text-red-600 hover:border-red-100 transition-all text-xs font-bold">
                                        驳回申请
                                    </button>
                                    <button @click="approve(order)"
                                        class="px-4 py-2 bg-slate-900 text-white rounded-xl hover:bg-indigo-600 shadow-lg shadow-slate-200 hover:shadow-indigo-200 transition-all text-xs font-bold">
                                        同意退款
                                    </button>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>

                <div v-if="refunds.length === 0" class="py-24 text-center">
                    <div class="inline-flex items-center justify-center w-20 h-20 bg-slate-50 rounded-full mb-4">
                        <span class="text-3xl">☕</span>
                    </div>
                    <h3 class="text-slate-800 font-bold">暂无售后申请</h3>
                    <p class="text-slate-400 text-sm mt-1">您可以休息一下，或者检查其他订单状态</p>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.animate-fade-in {
    animation: fadeIn 0.5s ease-out forwards;
}

@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.custom-scrollbar::-webkit-scrollbar {
    width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
    background-color: #e2e8f0;
    border-radius: 10px;
}

.font-serif-sc {
    font-family: 'Noto Serif SC', serif;
}

.line-clamp-1 {
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
</style>