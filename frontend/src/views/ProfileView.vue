<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { store } from '../store.js'
import { useRouter } from 'vue-router'
import { request } from '@/utils/request'
import Swal from 'sweetalert2'

const router = useRouter()
const orders = ref([])
const showAddressModal = ref(false)
const showRefundModal = ref(false)
const newAddress = ref({ contact: '', phone: '', detail: '', tag: '家' })
const refundForm = ref({ orderId: null, productNames: '', amount: 0, reason: '', type: '仅退款' })
const isLocating = ref(false)
const activeTab = ref('orders')
const searchQuery = ref('')

const currentPage = ref(1)
const pageSize = 5

// ✅ 优化修复：优惠券数量只显示【可用】数量 (约 20 行)
const couponCount = computed(() => {
  const now = new Date();
  return store.myCoupons.filter(c =>
    c.status === 'UNUSED' &&
    (
      !c.expiryDate ||
      new Date(c.expiryDate) > now
    )
  ).length
})
// ========================================================

// 动态获取用户地点 (修复 "浙江" 硬编码)
const userLocation = computed(() => {
  if (store.currentUser?.addresses?.length > 0) {
    const addr = store.currentUser.addresses.find(a => a.isDefault) || store.currentUser.addresses[0]
    // 尝试获取城市或地址前部分
    const detailParts = addr.detail.split('省')
    if (detailParts.length > 1) {
      // 如果包含省份，显示省份
      return detailParts[0] + '省'
    }
    // 否则显示地址前 6 个字符
    return addr.detail.length > 6 ? addr.detail.substring(0, 6) + '...' : addr.detail
  }
  return '未设置地址'
})

// === 新增：拉取优惠券逻辑 ===
const fetchCoupons = async () => {
  try {
    const username = store.currentUser?.username;
    if (!username) return;

    const res = await request.get(`/api/coupons/my?username=${username}`);
    if (res && Array.isArray(res)) {
      // 关键：将远程数据同步到 store.myCoupons
      store.myCoupons = res;
    }
  } catch (e) {
    console.error("Failed to fetch coupons:", e);
    store.myCoupons = [];
  }
};
// ========================================================

onMounted(async () => {
  if (!store.currentUser) { router.push('/login'); return }
  try {
    const username = store.currentUser.username
    // 关键修复：在拉取订单数据时，并行拉取优惠券数据
    const [ordersData] = await Promise.all([
      request(`/api/products/orders?username=${username}`),
      fetchCoupons() // 调用拉取优惠券
    ])

    if (ordersData) orders.value = ordersData

  } catch (error) { console.error(error) }
})

const filteredOrders = computed(() => {
  // ... (保持不变)
  let result = orders.value

  if (activeTab.value === 'orders') {
    result = result.filter(o => !['售后处理中', '退款成功', '已退货'].includes(o.status))
  }

  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(o => (20250000 + o.id).toString().includes(q) || o.productNames.toLowerCase().includes(q))
  }
  // 2. 排序：新订单在前
  return result.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
})

const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredOrders.value.slice(start, start + pageSize)
})

const totalPages = computed(() => Math.ceil(filteredOrders.value.length / pageSize))

watch(searchQuery, () => currentPage.value = 1)

const afterSalesOrders = computed(() => {
  return orders.value.filter(o => ['售后处理中', '退款成功', '已退货'].includes(o.status))
})

const openRefundModal = (order) => {
  refundForm.value = {
    orderId: order.id,
    productNames: order.productNames,
    amount: order.totalPrice,
    reason: '',
    type: '仅退款'
  }
  showRefundModal.value = true
}

const submitRefund = async () => {
  // ... (保持不变)
  if (!refundForm.value.reason) return Swal.fire('请填写申请原因', '', 'warning')

  if (!store.currentUser || !store.currentUser.username) {
      return Swal.fire('错误', '用户未登录，无法提交申请', 'error')
  }

  try {
    // ✅ 关键修复：在请求体中添加 username
    const payload = {
      reason: refundForm.value.reason,
      type: refundForm.value.type,
      username: store.currentUser.username // <<< 修复点：添加当前操作人
    };

    await request.post(`/api/orders/${refundForm.value.orderId}/refund`, payload)

    const order = orders.value.find(o => o.id === refundForm.value.orderId)
    if (order) order.status = '售后处理中'

    showRefundModal.value = false

    Swal.fire({
      title: '申请已提交',
      text: '商家将在 24 小时内审核您的请求',
      icon: 'success',
      confirmButtonColor: '#6366f1'
    })
    activeTab.value = 'aftersales'

  } catch (e) {
    Swal.fire('提交失败', e.message || '系统繁忙', 'error')
  }
}

// 确认收货逻辑
const confirmReceipt = async (order) => {
  // ... (保持不变)
  const result = await Swal.fire({
    title: '<span class="text-xl font-bold text-slate-800">确认已收到货品？</span>',
    html: `
      <div class="flex flex-col items-center gap-4 mt-2">
        <div class="w-16 h-16 bg-indigo-50 rounded-2xl flex items-center justify-center shadow-inner">
           <span class="text-3xl animate-bounce">📦</span>
        </div>
        <p class="text-sm text-slate-500">
            订单号 <span class="font-mono text-slate-700 font-bold bg-slate-100 px-2 py-0.5 rounded">#${20250000 + order.id}</span>
        </p>
        <div class="bg-gradient-to-r from-orange-50 to-amber-50 border border-orange-100 rounded-xl p-4 w-full text-center relative overflow-hidden">
            <div class="absolute -right-4 -top-4 w-12 h-12 bg-orange-200 rounded-full blur-xl opacity-50"></div>
            <p class="text-xs text-orange-600 font-bold mb-1 uppercase tracking-wider">本次签收可得</p>
            <p class="text-3xl font-black text-orange-500 flex items-center justify-center gap-1 font-serif-sc">
                <span>+${Math.floor(order.totalPrice)}</span>
                <span class="text-xs font-bold mt-2">积分</span>
            </p>
        </div>
        <p class="text-xs text-slate-400">保障提示：确认收货后资金将结算给商家</p>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: '确认签收 & 领积分',
    cancelButtonText: '还没收到',
    confirmButtonColor: '#4F46E5',
    cancelButtonColor: '#94a3b8',
    focusConfirm: false,
    reverseButtons: true,
    customClass: {
      popup: 'rounded-[32px] p-6',
      actions: 'gap-4',
      confirmButton: 'px-6 py-3 rounded-xl font-bold shadow-lg shadow-indigo-200',
      cancelButton: 'px-6 py-3 rounded-xl font-medium'
    }
  })

  if (!result.isConfirmed) return

  try {
    const updatedUser = await request(`/api/products/order/${order.id}/receive`, { method: 'POST' })
    store.login(updatedUser)
    order.status = '已送达'

    Swal.fire({
      icon: 'success',
      title: '<span class="text-indigo-600 font-bold">交易完成!</span>',
      html: `<div class="py-2"><p class="text-slate-500 mb-2">积分已火速到账</p><div class="inline-block bg-orange-100 text-orange-600 px-4 py-1 rounded-full font-bold">当前积分: ${updatedUser.points}</div></div>`,
      timer: 2500,
      showConfirmButton: false,
      customClass: { popup: 'rounded-[32px]' }
    })

  } catch (e) {
    Swal.fire({ title: '操作失败', text: '网络似乎开了小差，请稍后再试', icon: 'error', customClass: { popup: 'rounded-[24px]' } })
  }
}

const deleteOrder = async (id) => {
  // ... (保持不变)
  if ((await Swal.fire({ title: '删除订单?', icon: 'warning', showCancelButton: true, confirmButtonColor: '#ef4444' })).isConfirmed) {
    try { await request(`/api/products/order/${id}`, { method: 'DELETE' }); orders.value = orders.value.filter(o => o.id !== id); } catch (e) { }
  }
}

// === ✅ 核心修复：更完善的头像上传逻辑 (保持不变) ===
const handleAvatarUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 1. 限制大小 (2MB)
  if (file.size > 2 * 1024 * 1024) {
    Swal.fire('文件过大', '请上传 2MB 以内的图片', 'warning')
    return
  }

  const reader = new FileReader()
  reader.readAsDataURL(file)

  reader.onload = async () => {
    const base64String = reader.result
    try {
      // 2. 发送给后端
      const updatedUser = await request('/api/users/avatar', {
        method: 'POST',
        body: JSON.stringify({
          username: store.currentUser.username,
          avatar: base64String
        })
      })

      // 3. 显式更新 Store 中的头像 (确保 Header 等组件立刻刷新)
      if (store.currentUser) {
        store.currentUser.avatar = base64String
      }

      // 4. 调用 store.login 触发 LocalStorage 持久化
      // (前提：store.js 中必须已移除对 avatar 长度的限制)
      store.login(updatedUser)

      Swal.fire('成功', '头像更新成功', 'success')
    } catch (e) {
      console.error(e)
      Swal.fire('上传失败', '图片上传出错，请稍后重试', 'error')
    }
  }
}

const saveAddress = async () => { if (!newAddress.value.contact) return; const addrs = [...(store.currentUser.addresses || []), { ...newAddress.value, isDefault: (store.currentUser.addresses || []).length === 0 }]; const u = await request('/api/users/address', { method: 'POST', body: JSON.stringify({ username: store.currentUser.username, addresses: addrs }) }); store.login(u); showAddressModal.value = false; }
const removeAddress = async (idx) => { const addrs = [...store.currentUser.addresses]; addrs.splice(idx, 1); const u = await request('/api/users/address', { method: 'POST', body: JSON.stringify({ username: store.currentUser.username, addresses: addrs }) }); store.login(u); }
const locateUser = () => { isLocating.value = true; setTimeout(() => { newAddress.value.detail = "浙江省舟山市普陀区沈家门渔港路88号"; isLocating.value = false; }, 800) }
const formatDate = (iso) => new Date(iso).toLocaleDateString()

// UI 辅助
const getProgressWidth = (status) => {
  if (['待发货', 'PAID'].includes(status)) return '15%'
  if (['运输中', 'SHIPPED'].includes(status)) return '60%'
  if (['已送达', 'DELIVERED'].includes(status)) return '100%'
  return '0%'
}
const isStepActive = (currentStatus, step) => {
  const steps = ['待发货', '运输中', '已送达'];
  const map = { 'PAID': '待发货', 'SHIPPED': '运输中', 'DELIVERED': '已送达', '已送达': '已送达', '待发货': '待发货', '运输中': '运输中' };
  const curr = map[currentStatus] || '待发货';
  return steps.indexOf(curr) >= steps.indexOf(step);
}
const getStatusColor = (s) => {
  if (['已送达', 'DELIVERED'].includes(s)) return 'bg-emerald-100 text-emerald-700 border-emerald-200'
  if (['售后处理中'].includes(s)) return 'bg-orange-100 text-orange-700 border-orange-200'
  return 'bg-blue-100 text-blue-700 border-blue-200'
}
</script>

<template>
  <div class="min-h-screen bg-slate-50 font-sans relative overflow-hidden selection:bg-indigo-500 selection:text-white">

    <div class="fixed inset-0 pointer-events-none">
      <div
        class="absolute top-[-10%] left-[-10%] w-[60vw] h-[60vw] bg-indigo-400/20 rounded-full blur-[120px] animate-blob">
      </div>
      <div
        class="absolute bottom-[-10%] right-[-10%] w-[60vw] h-[60vw] bg-blue-400/20 rounded-full blur-[120px] animate-blob animation-delay-4000">
      </div>
      <div
        class="absolute top-[20%] right-[10%] w-[40vw] h-[40vw] bg-purple-400/20 rounded-full blur-[100px] animate-blob animation-delay-2000">
      </div>
    </div>

    <div class="relative max-w-6xl mx-auto px-6 py-10">

      <header class="flex justify-between items-center mb-10 animate-fade-down">
        <div class="flex items-center gap-4">
          <div
            class="w-12 h-12 bg-white rounded-2xl flex items-center justify-center shadow-xl shadow-indigo-100 text-indigo-600">
            <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
            </svg>
          </div>
          <div>
            <h1 class="text-2xl font-black text-slate-800 tracking-tight">空间</h1>
            <p class="text-sm text-slate-500">欢迎回来，尊贵的会员</p>
          </div>
        </div>
        <button @click="router.push('/')"
          class="px-6 py-2.5 bg-white/80 backdrop-blur-xl border border-white/50 text-slate-600 rounded-full text-sm font-bold shadow-sm hover:shadow-md hover:scale-105 transition-all duration-300">返回商城
          ➜</button>
      </header>

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">

        <aside class="lg:col-span-4 space-y-6 animate-fade-in-up" style="animation-delay: 0.1s;">
          <div
            class="bg-gradient-to-br from-indigo-600 to-blue-700 rounded-[32px] p-8 text-white shadow-2xl shadow-indigo-300/50 relative overflow-hidden group">
            <div
              class="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/carbon-fibre.png')] opacity-10 mix-blend-overlay">
            </div>
            <div class="relative z-10 flex flex-col items-center">
              <div class="relative w-24 h-24 mb-4">
                <img
                  :src="store.currentUser?.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${store.currentUser?.username}`"
                  class="w-full h-full rounded-full border-[3px] border-white/30 object-cover shadow-lg group-hover:scale-105 transition-transform duration-500">
                <label
                  class="absolute bottom-0 right-0 bg-white text-indigo-600 p-2 rounded-full cursor-pointer hover:bg-indigo-50 transition shadow-lg">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z">
                    </path>
                  </svg>
                  <input type="file" @change="handleAvatarUpload" class="hidden">
                </label>
              </div>
              <h2 class="text-2xl font-bold tracking-wide">{{ store.currentUser?.displayName ||
                store.currentUser?.username }}</h2>
              <div
                class="mt-2 px-3 py-1 bg-white/20 backdrop-blur-md rounded-full text-xs font-mono tracking-wider border border-white/10">
                SVIP · {{ store.currentUser?.username }}</div>

              <div
                class="mt-2 px-3 py-1 bg-white/20 backdrop-blur-md rounded-full text-xs tracking-wider border border-white/10 flex items-center gap-1">
                📍 {{ userLocation }}
              </div>

              <div class="grid grid-cols-3 gap-4 w-full mt-8 border-t border-white/10 pt-6">
                <div class="text-center">
                  <div class="text-xl font-bold font-serif-sc">{{ orders.length }}</div>
                  <div class="text-[10px] opacity-70 mt-1 uppercase">订单</div>
                </div>

                <div class="text-center cursor-pointer hover:bg-white/10 rounded-lg transition-colors p-1"
                  @click="router.push('/coupon')">
                  <div class="text-xl font-bold font-serif-sc text-orange-300">{{ couponCount }}</div>
                  <div class="text-[10px] opacity-70 mt-1 uppercase">优惠券</div>
                </div>

                <div class="text-center cursor-pointer hover:bg-white/10 rounded-lg transition-colors p-1"
                  @click="router.push('/points')">
                  <div class="text-xl font-bold font-serif-sc">{{ store.currentUser?.points || 0 }}</div>
                  <div class="text-[10px] opacity-70 mt-1 uppercase">积分</div>
                </div>
              </div>
            </div>
          </div>

          <div
            class="bg-white/70 backdrop-blur-xl border border-white/60 rounded-3xl p-2 shadow-lg shadow-slate-200/50">
            <button @click="activeTab = 'orders'" :class="['nav-btn group', activeTab === 'orders' ? 'active' : '']">
              <span class="text-2xl">📦</span>
              <div class="text-left">
                <div class="font-bold text-sm">我的订单</div>
                <div class="text-[10px] opacity-60">物流与管理</div>
              </div>
            </button>

            <button @click="activeTab = 'aftersales'"
              :class="['nav-btn group', activeTab === 'aftersales' ? 'active' : '']">
              <span class="text-2xl">🛡️</span>
              <div class="text-left">
                <div class="font-bold text-sm">售后服务</div>
                <div class="text-[10px] opacity-60">退款/退货</div>
              </div>
              <div v-if="afterSalesOrders.length > 0"
                class="ml-auto bg-red-500 text-white text-[10px] px-1.5 py-0.5 rounded-full">{{ afterSalesOrders.length
                }}</div>
            </button>

            <button @click="activeTab = 'address'" :class="['nav-btn group', activeTab === 'address' ? 'active' : '']">
              <span class="text-2xl">📍</span>
              <div class="text-left">
                <div class="font-bold text-sm">地址管理</div>
                <div class="text-[10px] opacity-60">收货设置</div>
              </div>
            </button>
          </div>
        </aside>

        <main class="lg:col-span-8 min-h-[500px]">

          <div v-if="activeTab === 'orders'" class="space-y-6 animate-fade-in-up" style="animation-delay: 0.2s;">
            <div class="relative group">
              <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none"><svg
                  class="w-5 h-5 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                </svg></div>
              <input v-model="searchQuery" type="text"
                class="w-full pl-12 pr-4 py-4 rounded-2xl bg-white/80 backdrop-blur-xl border border-white shadow-sm focus:shadow-lg focus:ring-2 focus:ring-indigo-500/20 outline-none transition-all placeholder-slate-400 font-medium"
                placeholder="搜索订单号 / 商品名称...">
            </div>

            <div v-if="paginatedOrders.length === 0" class="empty-state">
              <div class="text-6xl mb-4 opacity-20">🍃</div>
              <p class="text-slate-500 font-medium">{{ searchQuery ? '未找到相关订单' : '暂无订单记录' }}</p>
            </div>

            <div v-for="order in paginatedOrders" :key="order.id"
              class="bg-white/90 backdrop-blur-xl rounded-[24px] p-6 shadow-sm border border-white hover:shadow-xl hover:border-indigo-100 transition-all duration-300 group relative overflow-hidden">
              <div class="flex justify-between items-center mb-6 pb-4 border-b border-slate-100">
                <div class="flex items-center gap-3">
                  <div
                    class="px-3 py-1 rounded-lg bg-slate-100 text-slate-500 text-xs font-mono font-bold tracking-tight">
                    #{{ 20250000 + order.id }}</div>
                  <span class="text-xs text-slate-400">{{ formatDate(order.createTime) }}</span>
                </div>
                <span :class="['px-3 py-1 rounded-full text-xs font-bold border', getStatusColor(order.status)]">{{
                  order.status }}</span>
              </div>

              <div class="flex flex-col sm:flex-row gap-6">
                <div class="flex-1 space-y-4">
                  <div class="flex gap-4 items-center">
                    <div class="relative shrink-0">
                      <img :src="order.items?.[0]?.imageUrl || '/images/default.jpg'"
                        class="w-20 h-20 rounded-2xl object-cover shadow-md border border-white group-hover:scale-105 transition-transform duration-500">
                      <span v-if="(order.items?.length || 0) > 1"
                        class="absolute -bottom-2 -right-2 bg-slate-800 text-white text-[10px] px-1.5 py-0.5 rounded-md shadow-sm">+{{
                          order.items.length - 1 }}</span>
                    </div>
                    <div>
                      <h3 class="font-bold text-slate-800 text-lg line-clamp-1 mb-1">{{ order.productNames }}</h3>
                      <div class="text-xs text-slate-400">共 {{ order.items.length }} 件商品 · 实付 ¥{{ order.totalPrice }}
                      </div>
                    </div>
                  </div>
                  <div class="relative pt-2 pl-1 pr-4">
                    <div class="h-1 bg-slate-100 rounded-full w-full overflow-hidden">
                      <div class="h-full bg-indigo-500 rounded-full transition-all duration-1000"
                        :style="{ width: getProgressWidth(order.status) }"></div>
                    </div>
                    <div class="flex justify-between mt-2 text-[10px] font-bold text-slate-300">
                      <span :class="isStepActive(order.status, '待发货') ? 'text-indigo-600' : ''">待发货</span>
                      <span :class="isStepActive(order.status, '运输中') ? 'text-indigo-600' : ''">运输中</span>
                      <span :class="isStepActive(order.status, '已送达') ? 'text-indigo-600' : ''">已送达</span>
                    </div>
                  </div>
                </div>
                <div class="sm:border-l sm:border-slate-50 sm:pl-6 flex flex-row sm:flex-col justify-end gap-2">
                  <button v-if="order.status === '已送达'" @click="openRefundModal(order)"
                    class="text-xs text-slate-400 hover:text-indigo-600 px-2 py-1 sm:text-right">申请售后</button>
                  <div class="flex gap-2">
                    <button @click="deleteOrder(order.id)"
                      class="p-2 rounded-xl text-slate-300 hover:bg-red-50 hover:text-red-500 transition"><svg
                        class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16">
                        </path>
                      </svg></button>
                    <button v-if="order.status !== '已送达'" @click="confirmReceipt(order)"
                      class="px-4 py-2 bg-slate-900 text-white text-xs font-bold rounded-xl shadow-lg hover:bg-indigo-600 transition-all active:scale-95 whitespace-nowrap">确认收货</button>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="totalPages > 1" class="flex justify-center items-center gap-4 mt-8">
              <button @click="currentPage--" :disabled="currentPage === 1"
                class="w-10 h-10 rounded-full bg-white shadow-sm flex items-center justify-center hover:bg-indigo-50 disabled:opacity-50 transition">←</button>
              <span class="text-sm font-bold text-slate-600 font-mono">{{ currentPage }} / {{ totalPages }}</span>
              <button @click="currentPage++" :disabled="currentPage === totalPages"
                class="w-10 h-10 rounded-full bg-white shadow-sm flex items-center justify-center hover:bg-indigo-50 disabled:opacity-50 transition">→</button>
            </div>
          </div>

          <div v-else-if="activeTab === 'aftersales'" class="space-y-6 animate-fade-in-up">
            <div
              class="bg-indigo-50 border border-indigo-100 rounded-2xl p-4 flex items-center gap-3 text-indigo-700 text-sm">
              <span class="text-xl">🛡️</span>
              <div><strong>售后保障中</strong>
                <p class="text-xs opacity-70">为您提供 7 天无理由退换货服务</p>
              </div>
            </div>
            <div v-if="afterSalesOrders.length === 0" class="empty-state">
              <div class="text-6xl mb-4 opacity-20">📭</div>
              <p class="text-slate-500">暂无售后记录</p>
            </div>
            <div v-for="order in afterSalesOrders" :key="order.id"
              class="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm relative overflow-hidden">
              <div
                class="absolute top-0 right-0 px-4 py-1 bg-orange-100 text-orange-700 text-xs font-bold rounded-bl-xl">
                处理中</div>
              <h3 class="font-bold text-slate-800 mb-2">售后单 #AS{{ 20250000 + order.id }}</h3>
              <div class="flex gap-4 bg-slate-50 p-3 rounded-xl mb-4">
                <img :src="order.items?.[0]?.imageUrl" class="w-12 h-12 rounded-lg object-cover">
                <div>
                  <div class="text-sm font-bold text-slate-700 line-clamp-1">{{ order.productNames }}</div>
                  <div class="text-xs text-slate-400">退款金额: ¥{{ order.totalPrice }}</div>
                </div>
              </div>
              <div class="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
                <div class="bg-orange-500 h-full w-2/3 rounded-full animate-pulse"></div>
              </div>
              <div class="flex justify-between text-[10px] text-slate-400 mt-2"><span>提交申请</span><span
                  class="text-orange-600 font-bold">商家审核中</span><span>退款到账</span></div>
            </div>
          </div>

          <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-5 animate-fade-in-up">
            <div @click="showAddressModal = true"
              class="bg-white/40 border-2 border-dashed border-indigo-200 rounded-[24px] flex flex-col items-center justify-center min-h-[160px] cursor-pointer hover:bg-indigo-50/50 hover:border-indigo-400 transition text-indigo-400 group">
              <div
                class="w-12 h-12 rounded-full bg-indigo-50 flex items-center justify-center mb-3 group-hover:scale-110 transition">
                <span class="text-2xl text-indigo-500">+</span>
              </div>
              <span class="font-bold text-sm">新增收货地址</span>
            </div>
            <div v-for="(addr, idx) in store.currentUser.addresses" :key="idx"
              class="bg-white rounded-[24px] p-6 shadow-sm border border-slate-100 hover:shadow-lg hover:border-indigo-100 transition-all relative group">
              <div class="flex items-start justify-between mb-3">
                <span
                  :class="['text-[10px] font-bold px-2 py-1 rounded border', addr.tag === '家' ? 'bg-orange-50 text-orange-600 border-orange-100' : 'bg-blue-50 text-blue-600 border-blue-100']">{{
                    addr.tag }}</span>
                <div v-if="addr.isDefault" class="text-[10px] text-slate-400 flex items-center gap-1"><span
                    class="w-1.5 h-1.5 bg-green-500 rounded-full"></span> 默认</div>
              </div>
              <div class="font-bold text-slate-800 text-lg mb-1">{{ addr.contact }}</div>
              <div class="text-xs text-slate-400 font-mono mb-3">{{ addr.phone }}</div>
              <p class="text-sm text-slate-600 leading-relaxed border-t border-slate-50 pt-3">{{ addr.detail }}</p>
              <button @click="removeAddress(idx)"
                class="absolute top-4 right-4 text-slate-300 hover:text-red-500 hover:bg-red-50 p-1.5 rounded-lg transition opacity-0 group-hover:opacity-100">✕</button>
            </div>
          </div>

        </main>
      </div>
    </div>

    <div v-if="showAddressModal"
      class="fixed inset-0 bg-slate-900/40 backdrop-blur-md z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-[32px] p-8 w-full max-w-sm shadow-2xl animate-scale-up border border-white/50">
        <h3 class="text-xl font-black text-slate-800 mb-6">新增地址</h3>
        <div class="space-y-4">
          <input v-model="newAddress.contact" placeholder="联系人" class="input-field">
          <input v-model="newAddress.phone" placeholder="手机号" class="input-field">
          <div class="relative">
            <textarea v-model="newAddress.detail" placeholder="详细地址"
              class="input-field h-24 pt-3 resize-none"></textarea>
            <button @click="locateUser"
              class="absolute right-3 top-3 z-10 text-xs bg-blue-50 text-blue-600 px-2 py-1 rounded-md font-bold flex items-center gap-1 hover:bg-blue-100 transition"
              :disabled="isLocating">
              <span v-if="isLocating" class="animate-bounce"><img src="/icons/location.png"
                  class="w-5 h-5 object-contain" alt="定位中" /></span>
              <span v-else><img src="/icons/location.png" class="w-5 h-5 object-contain" alt="定位" /></span>
              <span>{{ isLocating ? '定位中...' : '定位' }}</span>
            </button>
          </div>
          <div class="flex gap-2">
            <span v-for="t in ['家', '公司', '学校']" :key="t" @click="newAddress.tag = t"
              :class="['text-xs px-4 py-2 rounded-xl cursor-pointer border transition font-medium', newAddress.tag === t ? 'bg-slate-800 text-white border-slate-800 shadow-md' : 'bg-slate-50 text-slate-500 border-transparent hover:bg-slate-100']">{{
                t }}</span>
          </div>
        </div>
        <div class="flex gap-3 mt-8">
          <button @click="showAddressModal = false"
            class="flex-1 py-3.5 text-slate-500 hover:bg-slate-50 rounded-2xl font-bold transition">取消</button>
          <button @click="saveAddress"
            class="flex-1 py-3.5 bg-indigo-600 text-white rounded-2xl shadow-lg shadow-indigo-200 hover:bg-indigo-700 transition font-bold transform active:scale-95">保存</button>
        </div>
      </div>
    </div>

    <div v-if="showRefundModal"
      class="fixed inset-0 bg-slate-900/40 backdrop-blur-md z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-[32px] p-8 w-full max-w-sm shadow-2xl animate-scale-up border border-white/50">
        <h3 class="text-xl font-black text-slate-800 mb-2">申请售后</h3>
        <p class="text-sm text-slate-500 mb-6">订单 #{{ 20250000 + refundForm.orderId }}</p>
        <div class="bg-slate-50 p-4 rounded-xl mb-4 flex gap-3">
          <div class="text-2xl">📦</div>
          <div>
            <div class="font-bold text-sm text-slate-700 line-clamp-1">{{ refundForm.productNames }}</div>
            <div class="text-xs text-slate-400">退款金额: ¥{{ refundForm.amount }}</div>
          </div>
        </div>
        <div class="space-y-4">
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1 block">售后类型</label>
            <div class="flex gap-2">
              <button @click="refundForm.type = '仅退款'"
                :class="['flex-1 py-2 text-xs rounded-xl border', refundForm.type === '仅退款' ? 'bg-indigo-50 border-indigo-500 text-indigo-700' : 'border-slate-200 text-slate-500']">仅退款</button>
              <button @click="refundForm.type = '退款退货'"
                :class="['flex-1 py-2 text-xs rounded-xl border', refundForm.type === '退款退货' ? 'bg-indigo-50 border-indigo-500 text-indigo-700' : 'border-slate-200 text-slate-500']">退货退款</button>
            </div>
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1 block">申请原因</label>
            <textarea v-model="refundForm.reason" placeholder="请描述您遇到的问题..."
              class="input-field h-24 resize-none"></textarea>
          </div>
        </div>
        <div class="flex gap-3 mt-8">
          <button @click="showRefundModal = false"
            class="flex-1 py-3.5 text-slate-500 hover:bg-slate-50 rounded-2xl font-bold transition">取消</button>
          <button @click="submitRefund"
            class="flex-1 py-3.5 bg-indigo-600 text-white rounded-2xl shadow-lg hover:bg-indigo-700 transition font-bold">提交申请</button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.nav-btn {
  @apply w-full flex items-center gap-4 px-6 py-4 rounded-2xl transition-all duration-300 hover:bg-white/50 text-slate-500;
}

.nav-btn.active {
  @apply bg-white shadow-md text-indigo-600 scale-[1.02];
}

.input-field {
  @apply w-full bg-slate-50 border border-slate-200 rounded-2xl px-5 py-3.5 text-sm focus:bg-white focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10 outline-none transition-all placeholder-slate-400 font-medium;
}

.empty-state {
  @apply h-full flex flex-col items-center justify-center bg-white/60 rounded-[32px] border border-dashed border-slate-300 p-12 min-h-[300px];
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background-color: transparent;
}

.animate-blob {
  animation: blob 10s infinite alternate cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes blob {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 0.6;
  }

  100% {
    transform: translate(20px, -20px) scale(1.1);
    opacity: 0.8;
  }
}

.animation-delay-2000 {
  animation-delay: 2s;
}

.animation-delay-4000 {
  animation-delay: 4s;
}

.animate-fade-down {
  animation: fadeDown 0.6s ease-out;
}

@keyframes fadeDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in-up {
  animation: fadeInUp 0.6s ease-out forwards;
  opacity: 0;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-scale-up {
  animation: scaleUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes scaleUp {
  from {
    opacity: 0;
    transform: scale(0.9);
  }

  to {
    opacity: 1;
    transform: scale(1);
  }
}
</style>