<script setup>
import { ref, onMounted } from 'vue'
import { request } from '@/utils/request'
import Swal from 'sweetalert2'

const orders = ref([])
const loading = ref(true)

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/orders')
    orders.value = res || []
  } catch (error) {
    console.error("加载订单失败", error)
  } finally {
    loading.value = false
  }
}

const payOrder = async (order) => {
  try {
    await Swal.fire({
      title: '正在支付...',
      timer: 1000,
      timerProgressBar: true,
      didOpen: () => Swal.showLoading()
    })

    await request.post(`/api/orders/${order.id}/pay`)

    await Swal.fire('支付成功', '订单状态已更新', 'success')
    fetchOrders()
  } catch (e) {
    console.error(e)
  }
}

const confirmReceive = async (order) => {
  try {
    const res = await Swal.fire({
      title: '确认收货吗？',
      text: '确认后积分将自动到账',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: '确认收货'
    })

    if (res.isConfirmed) {
      await request.post(`/api/products/order/${order.id}/receive`)
      await Swal.fire('已收货', '积分已到账', 'success')
      fetchOrders()
    }
  } catch (e) {
    console.error(e)
  }
}

const applyRefund = async (order) => {
  const { value: formValues } = await Swal.fire({
    title: '申请售后',
    html:
      '<select id="swal-type" class="swal2-input">' +
      '<option value="质量问题">质量问题</option>' +
      '<option value="少件/漏发">少件/漏发</option>' +
      '<option value="其他">其他</option>' +
      '</select>' +
      '<input id="swal-reason" class="swal2-input" placeholder="请输入具体原因">',
    focusConfirm: false,
    showCancelButton: true,
    preConfirm: () => {
      return {
        type: document.getElementById('swal-type').value,
        reason: document.getElementById('swal-reason').value
      }
    }
  })

  if (formValues) {
    try {
      await request.post(`/api/orders/${order.id}/refund`, formValues)
      Swal.fire('已提交', '售后申请正在审核中', 'success')
      fetchOrders()
    } catch (e) {
    }
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

const getStatusColor = (status) => {
  switch (status) {
    case 'UNPAID': return 'bg-orange-100 text-orange-600'
    case 'PAID': return 'bg-blue-100 text-blue-600'
    case 'SHIPPED': return 'bg-purple-100 text-purple-600'
    case '已送达': return 'bg-green-100 text-green-600'
    case '售后处理中': return 'bg-red-100 text-red-600'
    case '退款成功': return 'bg-gray-100 text-gray-500'
    default: return 'bg-slate-100 text-slate-600'
  }
}

const getStatusText = (status) => {
  const map = {
    'UNPAID': '待支付',
    'PAID': '待发货',
    'SHIPPED': '运输中',
    'DELIVERED': '已送达'
  }
  return map[status] || status
}

onMounted(() => {
  fetchOrders()
})
</script>

<template>
  <div class="min-h-screen bg-slate-50 pb-20 font-sans">
    <div class="container mx-auto px-4 py-8 max-w-4xl">
      <h1 class="text-2xl font-bold text-slate-800 mb-6">我的订单</h1>

      <div v-if="loading" class="text-center py-20 text-slate-400">
        加载中...
      </div>

      <div v-else-if="orders.length === 0"
        class="text-center py-20 bg-white rounded-3xl shadow-sm border border-slate-100">
        <div class="text-5xl mb-4">📦</div>
        <p class="text-slate-500 mb-6">暂无订单</p>
        <router-link to="/"
          class="px-6 py-2 bg-blue-600 text-white rounded-full font-bold text-sm hover:bg-blue-700 transition">去逛逛</router-link>
      </div>

      <div v-else class="space-y-6">
        <div v-for="order in orders" :key="order.id"
          class="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 hover:shadow-md transition">
          <div class="flex justify-between items-center mb-4 pb-4 border-b border-slate-50">
            <div class="text-sm text-slate-500">
              <span class="font-mono mr-2">#{{ order.id }}</span>
              <span>{{ formatDate(order.createTime) }}</span>
            </div>
            <span :class="`px-3 py-1 rounded-full text-xs font-bold ${getStatusColor(order.status)}`">
              {{ getStatusText(order.status) }}
            </span>
          </div>

          <div class="space-y-4 mb-6">
            <div v-for="item in order.items" :key="item.id" class="flex gap-4">
              <img :src="item.imageUrl" class="w-16 h-16 rounded-xl object-cover bg-slate-100 border border-slate-100">
              <div class="flex-1">
                <h3 class="font-bold text-slate-800 text-sm mb-1">{{ item.productName }}</h3>
                <div class="flex justify-between items-center">
                  <span class="text-xs text-slate-400">x{{ item.quantity }}</span>
                  <span class="font-medium text-slate-800">¥{{ item.price }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="flex flex-col sm:flex-row justify-between items-center gap-4 pt-2">
            <div class="text-slate-800">
              <span class="text-sm text-slate-500 mr-2">合计:</span>
              <span class="text-xl font-bold">¥{{ order.totalPrice }}</span>
            </div>

            <div class="flex gap-3">
              <button v-if="order.status === 'UNPAID'" @click="payOrder(order)"
                class="px-5 py-2 bg-blue-600 text-white rounded-xl text-sm font-bold shadow-lg shadow-blue-500/20 hover:bg-blue-700 transition active:scale-95">
                立即支付
              </button>

              <button v-if="order.status === 'SHIPPED'" @click="confirmReceive(order)"
                class="px-5 py-2 bg-slate-900 text-white rounded-xl text-sm font-bold hover:bg-slate-800 transition active:scale-95">
                确认收货
              </button>

              <button v-if="['已送达', 'DELIVERED'].includes(order.status)" @click="applyRefund(order)"
                class="px-4 py-2 text-slate-500 hover:bg-slate-50 rounded-xl text-sm font-bold border border-slate-200 transition">
                申请售后
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>