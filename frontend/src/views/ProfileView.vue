<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { store } from '../store.js'
import { useRouter } from 'vue-router'
import { request } from '@/utils/request'
import Swal from 'sweetalert2'

const formatNumber = (num) => {
  if (num === null || num === undefined) return '0'
  const n = Number(num)
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toString()
}

const getAvatarUrl = computed(() => {
  const avatar = store.currentUser?.avatar
  if (!avatar) return `https://api.dicebear.com/7.x/avataaars/svg?seed=${store.currentUser?.username}`
  if (avatar.startsWith('http')) return avatar
  return `http://localhost:8080${avatar}`
})

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
const pageSize = 3
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

const userLocation = computed(() => {
  if (store.currentUser?.addresses?.length > 0) {
    const addr = store.currentUser.addresses.find(a => a.isDefault) || store.currentUser.addresses[0]
    if (addr.detail) {
      const cityMatch = addr.detail.match(/省(.*?市)/)
      if (cityMatch && cityMatch[1]) return cityMatch[1]
      const directCity = addr.detail.match(/(.*?市)/)
      if (directCity && directCity[1]) return directCity[1]
      return addr.detail.substring(0, 4)
    }
  }
  return '未设置地址'
})

const fetchCoupons = async () => {
  try {
    const username = store.currentUser?.username;
    if (!username) return;
    const res = await request.get(`/api/coupons/my?username=${username}`);
    if (res && Array.isArray(res)) {
      store.myCoupons = res;
    }
  } catch (e) {
    console.error("Failed to fetch coupons:", e);
    store.myCoupons = [];
  }
};

onMounted(async () => {
  if (!store.currentUser) { router.push('/login'); return }
  try {
    const username = store.currentUser.username
    const [ordersData] = await Promise.all([
      request(`/api/products/orders?username=${username}`),
      fetchCoupons()
    ])
    if (ordersData) orders.value = ordersData
  } catch (error) { console.error(error) }
})

const filteredOrders = computed(() => {
  let result = orders.value
  if (activeTab.value === 'orders') {
    result = result.filter(o => !['售后处理中', '退款成功', '已退货'].includes(o.status))
  }
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(o => (20250000 + o.id).toString().includes(q) || o.productNames.toLowerCase().includes(q))
  }
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
    .sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
})

const afterSalesPage = ref(1)
const afterSalesPageSize = 3
const paginatedAfterSales = computed(() => {
  const start = (afterSalesPage.value - 1) * afterSalesPageSize
  return afterSalesOrders.value.slice(start, start + afterSalesPageSize)
})
const afterSalesTotalPages = computed(() => Math.ceil(afterSalesOrders.value.length / afterSalesPageSize))

const deleteAfterSalesOrder = async (order) => {
  if (order.status === '售后处理中') {
    return Swal.fire('无法删除', '售后处理中的订单无法删除', 'warning')
  }
  const confirm = await Swal.fire({
    title: '删除售后记录?',
    text: '此操作不可恢复',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#ef4444',
    confirmButtonText: '删除'
  })
  if (!confirm.isConfirmed) return
  
  try {
    await request.delete(`/api/orders/${order.id}`)
    orders.value = orders.value.filter(o => o.id !== order.id)
    Swal.fire('已删除', '售后记录已移除', 'success')
  } catch (e) {
    Swal.fire('删除失败', e.message || '系统错误', 'error')
  }
}

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
  if (!refundForm.value.reason) return Swal.fire('请填写申请原因', '', 'warning')
  if (!store.currentUser || !store.currentUser.username) {
    return Swal.fire('错误', '用户未登录，无法提交申请', 'error')
  }
  try {
    const payload = {
      reason: refundForm.value.reason,
      type: refundForm.value.type,
      username: store.currentUser.username
    };
    await request.post(`/api/orders/${refundForm.value.orderId}/refund`, payload)
    const order = orders.value.find(o => o.id === refundForm.value.orderId)
    if (order) order.status = '售后处理中'
    showRefundModal.value = false
    Swal.fire({
      title: '申请已提交',
      text: '商家将在 24 小时内审核您的请求',
      icon: 'success',
      confirmButtonColor: '#0f172a'
    })
    activeTab.value = 'aftersales'
  } catch (e) {
    Swal.fire('提交失败', e.message || '系统繁忙', 'error')
  }
}

const confirmReceipt = async (order) => {
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
    focusConfirm: false,
    reverseButtons: true,
  })

  if (!result.isConfirmed) return

  try {
    const updatedUser = await request(`/api/products/order/${order.id}/receive`, { method: 'POST' })
    store.login(updatedUser, true)
    order.status = '已送达'

    Swal.fire({
      icon: 'success',
      title: '<span class="text-indigo-600 font-bold">交易完成!</span>',
      html: `<div class="py-2"><p class="text-slate-500 mb-2">积分已火速到账</p><div class="inline-block bg-orange-100 text-orange-600 px-4 py-1 rounded-full font-bold">当前积分: ${updatedUser.points}</div></div>`,
      timer: 2500,
      showConfirmButton: false
    })
  } catch (e) {
    Swal.fire({ title: '操作失败', text: '网络似乎开了小差，请稍后再试', icon: 'error' })
  }
}

const deleteOrder = async (id) => {
  if ((await Swal.fire({ title: '删除订单?', icon: 'warning', showCancelButton: true, confirmButtonColor: '#ef4444' })).isConfirmed) {
    try { await request(`/api/products/order/${id}`, { method: 'DELETE' }); orders.value = orders.value.filter(o => o.id !== id); } catch (e) { }
  }
}

const handleAvatarUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    Swal.fire('文件过大', '请上传 2MB 以内的图片', 'warning')
    return
  }
  const reader = new FileReader()
  reader.readAsDataURL(file)
  reader.onload = async () => {
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('username', store.currentUser.username);
      const token = localStorage.getItem('yuxian_token');
      const response = await fetch('http://localhost:8080/api/users/upload-avatar', {
        method: 'POST',
        headers: { 'Authorization': token ? `Bearer ${token}` : '' },
        body: formData
      });
      if (!response.ok) throw new Error('上传失败');
      const updatedUser = await response.json();
      if (store.currentUser) store.currentUser.avatar = updatedUser.avatar
      store.login(updatedUser, true)
      Swal.fire('成功', '头像更新成功', 'success')
    } catch (e) {
      console.error(e)
      Swal.fire('上传失败', '图片上传出错，请稍后重试', 'error')
    }
  }
}

const saveAddress = async () => { if (!newAddress.value.contact) return; const addrs = [...(store.currentUser.addresses || []), { ...newAddress.value, isDefault: (store.currentUser.addresses || []).length === 0 }]; const u = await request('/api/users/address', { method: 'POST', body: { username: store.currentUser.username, addresses: addrs } }); store.login(u, true); showAddressModal.value = false; }
const removeAddress = async (idx) => { const addrs = [...store.currentUser.addresses]; addrs.splice(idx, 1); const u = await request('/api/users/address', { method: 'POST', body: { username: store.currentUser.username, addresses: addrs } }); store.login(u, true); }

const locateUser = () => {
  if (typeof AMap === 'undefined') {
    Swal.fire('错误', '地图组件未加载，请刷新页面重试', 'error')
    return
  }

  isLocating.value = true
  if (newAddress.value) newAddress.value.detail = ''

  AMap.plugin(['AMap.Geolocation', 'AMap.Geocoder'], function () {

    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      zoomToAccuracy: true,
      showButton: false,
      showMarker: false,
      showCircle: false,
      panToLocation: false
    })

    geolocation.getCurrentPosition(function (status, result) {
      if (status === 'complete') {
        console.log('定位成功:', result.position)

        const geocoder = new AMap.Geocoder({ 
          radius: 1000, 
          extensions: 'all',
          timeout: 20000 
        })

        geocoder.getAddress(result.position, function (status, data) {
          isLocating.value = false
          if (status === 'complete' && data.regeocode) {
            const addr = data.regeocode.formattedAddress
            newAddress.value.detail = addr
            Swal.fire({
              toast: true,
              position: 'top',
              icon: 'success',
              title: '定位成功',
              text: addr,
              timer: 3000,
              showConfirmButton: false
            })
          } else {
            newAddress.value.detail = `(已定位到经纬度: ${result.position}, 但地址获取失败，请手动输入)`
            Swal.fire('提示', '地址解析失败 (可能因网络原因)，请手动补充', 'warning')
          }
        })
      } else {
        isLocating.value = false
        console.error("定位失败:", result.message)
        Swal.fire('定位失败', '请检查GPS或网络设置', 'error')
      }
    })
  })
}

const formatDate = (iso) => new Date(iso).toLocaleDateString()

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
        <aside class="lg:col-span-4 space-y-6 animate-fade-in-up sticky top-28 h-fit z-20"
          style="animation-delay: 0.1s;">
          <div class="card-glass bg-aurora p-8 group relative isolate">
            <div class="blob w-32 h-32 bg-blue-500 top-[-20%] left-[-10%]"></div>
            <div class="blob w-24 h-24 bg-cyan-400 bottom-[-10%] right-[-10%] animation-delay-2000"></div>
            <div class="absolute inset-0 opacity-20"
              style="background-image: url('data:image/svg+xml,%3Csvg viewBox=\'0 0 200 200\' xmlns=\'http://www.w3.org/2000/svg\'%3E%3Cfilter id=\'noiseFilter\'%3E%3CfeTurbulence type=\'fractalNoise\' baseFrequency=\'0.8\' numOctaves=\'3\' stitchTiles=\'stitch\'/%3E%3C/filter%3E%3Crect width=\'100%25\' height=\'100%25\' filter=\'url(%23noiseFilter)\' opacity=\'1\'/%3E%3C/svg%3E'); mix-blend-mode: overlay;">
            </div>

            <div class="relative z-10 flex flex-col items-center">
              <div class="relative w-24 h-24 mb-5 group/avatar">
                <div
                  class="absolute -inset-8 bg-cyan-400/20 rounded-full blur-2xl animate-pulse-halo pointer-events-none">
                </div>
                <div
                  class="absolute -inset-1 bg-gradient-to-tr from-transparent via-white/80 to-transparent rounded-full blur-md opacity-0 animate-shimmer-ring">
                </div>
                <div
                  class="absolute -inset-[3px] rounded-full bg-gradient-to-tr from-cyan-500 via-blue-500 to-indigo-500 opacity-60 blur-[2px] animate-spin-slow">
                </div>
                <img
                  :src="getAvatarUrl"
                  class="relative w-full h-full rounded-full border-[3px] border-white/40 object-cover shadow-2xl z-10 group-hover:scale-105 transition-transform duration-500">
                <label
                  class="absolute bottom-0 right-0 z-20 bg-white/10 backdrop-blur-md border border-white/30 text-white p-1.5 rounded-full cursor-pointer hover:bg-white/30 transition-all shadow-lg hover:scale-110">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z">
                    </path>
                  </svg>
                  <input type="file" @change="handleAvatarUpload" class="hidden">
                </label>
              </div>

              <h2 class="text-3xl font-black tracking-wide shimmer-text mb-1 drop-shadow-md">{{
                store.currentUser?.displayName || store.currentUser?.username }}</h2>
              <div
                class="mt-2 text-sm font-mono font-bold tracking-[0.2em] opacity-90 shimmer-svip cursor-default select-none mb-4">
                SVIP · {{ store.currentUser?.username }}</div>
              <div
                class="flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/5 border border-white/5 text-[10px] text-slate-300 backdrop-blur-sm">
                <svg class="w-3 h-3 opacity-70" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"></path>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"></path>
                </svg>
                <span class="tracking-wider">{{ userLocation }}</span>
              </div>

              <div class="grid grid-cols-4 gap-1 w-full mt-8 border-t border-white/10 pt-6 px-1">
                <div class="flex flex-col items-center cursor-default group/item">
                  <div
                    class="h-7 flex items-end text-base font-bold font-serif-sc text-white group-hover/item:text-cyan-300 transition-colors drop-shadow-sm text-center">
                    {{ orders.length }}</div>
                  <div class="text-[10px] text-slate-400 font-medium mt-1 uppercase tracking-widest scale-90">订单</div>
                </div>
                
                <div
                  class="flex flex-col items-center cursor-pointer group/item hover:bg-white/5 rounded-xl py-2 -my-2 transition-all"
                  @click="router.push('/coupon')">
                  <div
                    class="h-7 flex items-end text-base font-bold font-serif-sc text-white group-hover/item:text-cyan-300 transition-colors drop-shadow-sm text-center">
                    {{ couponCount }}</div>
                  <div class="text-[10px] text-slate-400 font-medium mt-1 uppercase tracking-widest scale-90">优惠券</div>
                </div>
                
                <div
                  class="flex flex-col items-center cursor-pointer group/item hover:bg-white/5 rounded-xl py-2 -my-2 transition-all relative"
                  @click="router.push('/wallet')">
                  <div
                    class="h-7 flex items-end text-base font-bold font-serif-sc text-white group-hover/item:text-cyan-300 transition-colors drop-shadow-sm text-center whitespace-nowrap">
                    <span class="text-[10px] opacity-60 mr-0.5">¥</span>{{ formatNumber(store.currentUser?.balance || 0) }}</div>
                  <div class="text-[10px] text-slate-400 font-medium mt-1 uppercase tracking-widest scale-90">余额</div>
                </div>
                
                <div
                  class="flex flex-col items-center cursor-pointer group/item hover:bg-white/5 rounded-xl py-2 -my-2 transition-all"
                  @click="router.push('/points')">
                  <div
                    class="h-7 flex items-end text-base font-bold font-serif-sc text-white group-hover/item:text-cyan-300 transition-colors drop-shadow-sm text-center">
                    {{ formatNumber(store.currentUser?.points || 0) }}</div>
                  <div class="text-[10px] text-slate-400 font-medium mt-1 uppercase tracking-widest scale-90">积分</div>
                </div>
              </div>
            </div>
          </div>

          <div
            class="bg-white/70 backdrop-blur-xl border border-white/60 rounded-3xl p-2 shadow-lg shadow-slate-200/50">
            <button @click="activeTab = 'orders'" :class="['nav-btn group', activeTab === 'orders' ? 'active' : '']">
              <span class="text-2xl"></span>
              <div class="text-left">
                <div class="font-bold text-sm">我的订单</div>
                <div class="text-[10px] opacity-60">物流与管理</div>
              </div>
            </button>
            <button @click="activeTab = 'aftersales'"
              :class="['nav-btn group', activeTab === 'aftersales' ? 'active' : '']">
              <span class="text-2xl"></span>
              <div class="text-left">
                <div class="font-bold text-sm">售后服务</div>
                <div class="text-[10px] opacity-60">退款/退货</div>
              </div>
              <div v-if="afterSalesOrders.length > 0"
                class="ml-auto bg-red-500 text-white text-[10px] px-1.5 py-0.5 rounded-full">{{ afterSalesOrders.length
                }}</div>
            </button>
            <button @click="activeTab = 'address'" :class="['nav-btn group', activeTab === 'address' ? 'active' : '']">
              <span class="text-2xl"></span>
              <div class="text-left">
                <div class="font-bold text-sm">地址管理</div>
                <div class="text-[10px] opacity-60">收货设置</div>
              </div>
            </button>
          </div>
        </aside>

        <main class="lg:col-span-8 min-h-[600px]">
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
              <div class="text-6xl mb-4 opacity-20"></div>
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
              <span class="text-xl"></span>
              <div><strong>售后保障中</strong>
                <p class="text-xs opacity-70">为您提供 7 天无理由退换货服务</p>
              </div>
            </div>
            <div v-if="afterSalesOrders.length === 0" class="empty-state">
              <div class="text-6xl mb-4 opacity-20">📭</div>
              <p class="text-slate-500">暂无售后记录</p>
            </div>
            <div v-for="order in paginatedAfterSales" :key="order.id"
              class="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm relative overflow-hidden group">
              
              <button v-if="order.status !== '售后处理中'" @click="deleteAfterSalesOrder(order)"
                class="absolute top-4 right-4 text-slate-300 hover:text-red-500 hover:bg-red-50 p-2 rounded-full transition opacity-0 group-hover:opacity-100 z-10">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                </svg>
              </button>

              <div class="absolute top-0 right-0 px-4 py-1 text-xs font-bold rounded-bl-xl"
                :class="{ 'bg-orange-100 text-orange-700': order.status === '售后处理中', 'bg-green-100 text-green-700': order.status === '退款成功', 'bg-slate-100 text-slate-700': order.status === '已退货' }">
                {{ order.status === '售后处理中' ? '处理中' : (order.status === '退款成功' ? '退款成功' : order.status) }}</div>
              <h3 class="font-bold text-slate-800 mb-2">售后单 #AS{{ 20250000 + order.id }}</h3>
              <div class="flex gap-4 bg-slate-50 p-3 rounded-xl mb-4"><img :src="order.items?.[0]?.imageUrl"
                  class="w-12 h-12 rounded-lg object-cover">
                <div>
                  <div class="text-sm font-bold text-slate-700 line-clamp-1">{{ order.productNames }}</div>
                  <div class="text-xs text-slate-400">退款金额: ¥{{ order.totalPrice }}</div>
                </div>
              </div>
              <div class="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
                <div class="h-full rounded-full transition-all duration-500"
                  :class="{ 'bg-orange-500 w-2/3 animate-pulse': order.status === '售后处理中', 'bg-green-500 w-full': order.status === '退款成功' }">
                </div>
              </div>
              <div class="flex justify-between text-[10px] text-slate-400 mt-2"><span>提交申请</span><span
                  :class="{ 'text-orange-600 font-bold': order.status === '售后处理中', 'text-green-600': order.status === '退款成功' }">商家审核{{
                    order.status === '售后处理中' ? '中' : '通过' }}</span><span
                  :class="{ 'text-green-600 font-bold': order.status === '退款成功' }">退款到账</span></div>
            </div>

            <div v-if="afterSalesTotalPages > 1" class="flex justify-center mt-6 gap-3">
              <button @click="afterSalesPage--" :disabled="afterSalesPage === 1"
                class="px-3 py-1 rounded bg-white border text-sm disabled:opacity-50 hover:bg-slate-50">上一页</button>
              <span class="text-sm text-slate-500 py-1">{{ afterSalesPage }} / {{ afterSalesTotalPages }}</span>
              <button @click="afterSalesPage++" :disabled="afterSalesPage === afterSalesTotalPages"
                class="px-3 py-1 rounded bg-white border text-sm disabled:opacity-50 hover:bg-slate-50">下一页</button>
            </div>
          </div>

          <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-5 animate-fade-in-up">
            <div @click="showAddressModal = true"
              class="bg-white/40 border-2 border-dashed border-indigo-200 rounded-[24px] flex flex-col items-center justify-center min-h-[160px] cursor-pointer hover:bg-indigo-50/50 hover:border-indigo-400 transition text-indigo-400 group">
              <div
                class="w-12 h-12 rounded-full bg-indigo-50 flex items-center justify-center mb-3 group-hover:scale-110 transition">
                <span class="text-2xl text-indigo-500">+</span></div><span class="font-bold text-sm">新增收货地址</span>
            </div>
            <div v-for="(addr, idx) in store.currentUser.addresses" :key="idx"
              class="bg-white rounded-[24px] p-6 shadow-sm border border-slate-100 hover:shadow-lg hover:border-indigo-100 transition-all relative group">
              <div class="flex items-start justify-between mb-3"><span
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

    <Teleport to="body">
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
    </Teleport>

    <Teleport to="body">
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
            <div><label class="text-xs font-bold text-slate-500 mb-1 block">申请原因</label><textarea
                v-model="refundForm.reason" placeholder="请描述您遇到的问题..." class="input-field h-24 resize-none"></textarea>
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
    </Teleport>
  </div>
</template>

<style>
.amap-logo,
.amap-copyright,
.amap-geolocation-con,
.amap-call {
  display: none !important;
  visibility: hidden !important;
  opacity: 0 !important;
  width: 0 !important;
  height: 0 !important;
}

iframe[id^="amap"] {
  display: none !important;
  width: 0 !important;
  height: 0 !important;
  opacity: 0 !important;
  pointer-events: none !important;
  position: absolute;
  top: -9999px;
  left: -9999px;
}
</style>

<style scoped>
.bg-aurora {
  background: linear-gradient(-45deg, #0f172a, #1e3a8a, #0ea5e9, #0f172a);
  background-size: 400% 400%;
  animation: auroraFlow 15s ease infinite;
}

@keyframes auroraFlow {
  0% {
    background-position: 0% 50%;
  }

  50% {
    background-position: 100% 50%;
  }

  100% {
    background-position: 0% 50%;
  }
}

.shimmer-text {
  background: linear-gradient(110deg, #e2e8f0 30%, #ffffff 50%, #e2e8f0 70%);
  background-size: 200% auto;
  color: transparent;
  background-clip: text;
  -webkit-background-clip: text;
  animation: shine 4s linear infinite;
}

.shimmer-svip {
  background: linear-gradient(110deg, #67e8f9 35%, #ffffff 50%, #67e8f9 65%);
  background-size: 200% auto;
  color: transparent;
  background-clip: text;
  -webkit-background-clip: text;
  animation: shine 3s linear infinite;
  text-shadow: 0 0 20px rgba(103, 232, 249, 0.5);
}

@keyframes shine {
  to {
    background-position: 200% center;
  }
}

.card-glass {
  @apply relative overflow-hidden rounded-[32px] text-white shadow-2xl shadow-blue-900/40;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.4;
  animation: float 10s infinite ease-in-out;
}

@keyframes float {

  0%,
  100% {
    transform: translate(0, 0);
  }

  50% {
    transform: translate(20px, -20px);
  }
}

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

.animate-pulse-halo {
  animation: pulseHalo 4s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulseHalo {

  0%,
  100% {
    opacity: 0.3;
    transform: scale(0.9);
  }

  50% {
    opacity: 0.6;
    transform: scale(1.1);
  }
}

.animate-shimmer-ring {
  animation: shimmerRing 3s ease-in-out infinite;
}

@keyframes shimmerRing {

  0%,
  100% {
    opacity: 0;
    transform: rotate(0deg);
  }

  25% {
    opacity: 0.8;
  }

  50% {
    opacity: 0.2;
  }

  75% {
    opacity: 0.6;
  }

  100% {
    transform: rotate(180deg);
  }
}

.animate-spin-slow {
  animation: spin 10s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}
</style>