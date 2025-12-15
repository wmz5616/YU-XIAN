<script setup>
import { ref, onMounted } from 'vue'
import { request } from '@/utils/request'
import Swal from 'sweetalert2'

const refunds = ref([])
const loading = ref(false)

const fetchRefunds = async () => {
    loading.value = true
    try {
        // 调用刚才写的后端接口
        const res = await request.get('/api/orders/admin/refunds')
        refunds.value = res || []
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

// 同意退款
const approve = async (order) => {
    const result = await Swal.fire({
        title: '同意退款?',
        text: `订单金额 ¥${order.totalPrice} 将原路退回给用户`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#10B981', // Green
        confirmButtonText: '确认同意',
        cancelButtonText: '取消'
    })

    if (result.isConfirmed) {
        try {
            await request.post(`/api/orders/admin/refunds/${order.id}/audit`, { pass: true })
            Swal.fire('已处理', '订单已变更为退款成功', 'success')
            fetchRefunds() // 刷新列表
        } catch (e) {
            Swal.fire('操作失败', e.message, 'error')
        }
    }
}

// 驳回申请
const reject = async (order) => {
    const result = await Swal.fire({
        title: '驳回申请',
        input: 'text',
        inputLabel: '请输入驳回理由',
        inputPlaceholder: '例如：商品已损坏，不符合退货条件...',
        showCancelButton: true,
        confirmButtonColor: '#EF4444', // Red
        confirmButtonText: '确认驳回'
    })

    if (result.isConfirmed) {
        try {
            await request.post(`/api/orders/admin/refunds/${order.id}/audit`, {
                pass: false,
                reason: result.value
            })
            Swal.fire('已驳回', '订单状态已恢复', 'info')
            fetchRefunds()
        } catch (e) {
            Swal.fire('操作失败', e.message, 'error')
        }
    }
}

onMounted(() => {
    fetchRefunds()
})
</script>

<template>
    <div class="p-6 bg-[#F8FAFC] min-h-screen">
        <div class="max-w-7xl mx-auto">
            <h1 class="text-2xl font-bold text-slate-800 mb-6 flex items-center gap-2">
                🛡️ 售后处理中心
                <span class="text-sm font-normal bg-orange-100 text-orange-600 px-3 py-1 rounded-full">{{ refunds.length
                    }} 待处理</span>
            </h1>

            <div class="bg-white rounded-xl shadow-sm border border-slate-100 overflow-hidden">
                <table class="w-full text-left border-collapse">
                    <thead>
                        <tr class="bg-slate-50 text-slate-500 text-sm border-b border-slate-100">
                            <th class="p-4 font-medium">售后单号</th>
                            <th class="p-4 font-medium">商品信息</th>
                            <th class="p-4 font-medium">申请人</th>
                            <th class="p-4 font-medium">退款金额</th>
                            <th class="p-4 font-medium">申请原因</th>
                            <th class="p-4 font-medium text-right">操作</th>
                        </tr>
                    </thead>
                    <tbody class="text-sm divide-y divide-slate-50">
                        <tr v-for="order in refunds" :key="order.id" class="hover:bg-slate-50/50 transition">
                            <td class="p-4 font-mono text-slate-500">#AS{{ 20250000 + order.id }}</td>
                            <td class="p-4">
                                <div class="font-bold text-slate-700 truncate max-w-[200px]">{{ order.productNames }}
                                </div>
                                <div class="text-xs text-slate-400 mt-1">数量: {{ order.items?.length || 1 }}</div>
                            </td>
                            <td class="p-4">
                                <div class="flex items-center gap-2">
                                    <div
                                        class="w-6 h-6 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center text-xs font-bold">
                                        {{ order.username.charAt(0).toUpperCase() }}
                                    </div>
                                    <span>{{ order.username }}</span>
                                </div>
                            </td>
                            <td class="p-4 font-bold text-orange-600 font-serif-sc">¥{{ order.totalPrice }}</td>
                            <td class="p-4">
                                <span class="bg-slate-100 text-slate-600 px-2 py-1 rounded text-xs">仅退款</span>
                                <div class="text-xs text-slate-400 mt-1 max-w-[150px] truncate"
                                    :title="order.refundReason">
                                    {{ order.refundReason || '用户未填写详细原因' }}
                                </div>
                            </td>
                            <td class="p-4 text-right">
                                <div class="flex justify-end gap-2">
                                    <button @click="reject(order)"
                                        class="px-3 py-1.5 border border-slate-200 text-slate-600 rounded-lg hover:bg-red-50 hover:text-red-600 hover:border-red-200 transition text-xs font-bold">
                                        驳回
                                    </button>
                                    <button @click="approve(order)"
                                        class="px-3 py-1.5 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 shadow-md shadow-indigo-200 transition text-xs font-bold">
                                        同意退款
                                    </button>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>

                <div v-if="refunds.length === 0" class="p-12 text-center text-slate-400">
                    <div class="text-4xl mb-2 opacity-50">✨</div>
                    <p>暂无待处理的售后申请</p>
                </div>
            </div>
        </div>
    </div>
</template>