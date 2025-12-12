<script setup>
import { ref, onMounted, computed } from 'vue'
import { store } from '../store.js'
import { useRouter } from 'vue-router'
import { request } from '@/utils/request'

const router = useRouter()
const orders = ref([])
const showAddressModal = ref(false)
const newAddress = ref({ contact: '', phone: '', detail: '', tag: '家' })
const isLocating = ref(false)
const searchQuery = ref('')

onMounted(async () => {
  if (!store.currentUser) {
    router.push('/login')
    return
  }
  try {
    const username = store.currentUser.username

    const ordersData = await request(`/api/products/orders?username=${username}`)
    if (ordersData) {
      orders.value = ordersData
    }

    const userData = await request(`/api/users/info?username=${username}`)
    if (userData) {
      store.login(userData)
    }
  } catch (error) {
    console.error('数据加载失败', error)
  }
})

const filteredOrders = computed(() => {
  if (!searchQuery.value) return orders.value
  const query = searchQuery.value.toLowerCase()
  return orders.value.filter(order => {
    const displayId = (20250000 + order.id).toString()
    return displayId.includes(query) || order.productNames.toLowerCase().includes(query)
  })
})

const buyAgain = (productNames) => {
  if (!productNames) {
    store.showNotification('该历史订单数据不完整，无法自动加购', 'error')
    return
  }
  const firstProductName = productNames.split('x')[0].replace(/[,，]/g, '').trim()
  router.push({ path: '/', query: { keyword: firstProductName } })
}

const applyRefund = () => {
  alert('售后服务通道正在维护中，请联系客服：400-888-6666')
}

const confirmReceipt = async (order) => {
  if (!confirm(`确认收到货了吗？\n确认后将获得 ${Math.floor(order.totalPrice)} 积分！`)) return

  try {
    const updatedUser = await request(`/api/products/order/${order.id}/receive`, {
      method: 'POST'
    })

    store.login(updatedUser)
    order.status = '已送达'
    store.showNotification(`交易完成！积分 +${Math.floor(order.totalPrice)}`)

  } catch (e) {
    store.showNotification('操作失败', 'error')
  }
}

const deleteOrder = async (orderId) => {
  if (!confirm('确定删除此订单吗？删除后不可恢复。')) return

  try {
    await request(`/api/products/order/${orderId}`, {
      method: 'DELETE'
    })

    store.showNotification('订单已删除')
    orders.value = orders.value.filter(o => o.id !== orderId)

  } catch (e) {
    console.error(e)
    store.showNotification('删除失败', 'error')
  }
}

const locateUser = () => {
  if (typeof AMap === 'undefined') {
    store.showNotification('高德地图加载失败，请检查网络或Key', 'error')
    return
  }

  isLocating.value = true
  store.showNotification('正在调用高德定位...')

  AMap.plugin('AMap.Geolocation', function () {
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      needAddress: true,
      extensions: 'all'
    })

    geolocation.getCurrentPosition(function (status, result) {
      isLocating.value = false
      if (status === 'complete') {
        newAddress.value.detail = result.formattedAddress
        store.showNotification('定位成功')
      } else {
        store.showNotification('定位失败：' + (result.message || '权限被拒绝或超时'), 'error')
      }
    })
  })
}

const saveAddress = async () => {
  if (!newAddress.value.contact || !newAddress.value.phone || !newAddress.value.detail) {
    store.showNotification('请填写完整地址信息', 'error')
    return
  }

  const currentAddresses = store.currentUser.addresses || []
  const isDefault = currentAddresses.length === 0
  const updatedAddresses = [...currentAddresses, { ...newAddress.value, isDefault }]

  try {
    const updatedUser = await request('/api/users/address', {
      method: 'POST',
      body: JSON.stringify({
        username: store.currentUser.username,
        addresses: updatedAddresses
      })
    })

    store.login(updatedUser)
    showAddressModal.value = false
    newAddress.value = { contact: '', phone: '', detail: '', tag: '家' }
    store.showNotification('地址添加成功')

  } catch (e) {
    store.showNotification('保存失败', 'error')
  }
}

const removeAddress = async (index) => {
  if (!confirm('确定要删除这个地址吗？')) return
  const updatedAddresses = [...store.currentUser.addresses]
  updatedAddresses.splice(index, 1)

  try {
    const updatedUser = await request('/api/users/address', {
      method: 'POST',
      body: JSON.stringify({ username: store.currentUser.username, addresses: updatedAddresses })
    })
    store.login(updatedUser)
    store.showNotification('地址已删除')

  } catch (e) { store.showNotification('删除失败', 'error') }
}

const getStatusStyle = (status) => {
  switch (status) {
    case '待发货': return 'bg-amber-50 text-amber-600 border-amber-200'
    case '运输中': return 'bg-blue-50 text-blue-600 border-blue-200'
    case '已送达': return 'bg-green-50 text-green-600 border-green-200'
    default: return 'bg-slate-50 text-slate-500 border-slate-200'
  }
}

const formatDate = (isoString) => {
  if (!isoString) return ''
  return new Date(isoString).toLocaleString('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  })
}


const handleAvatarUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    store.showNotification('图片太大了，请小于 2MB', 'error')
    return
  }
  
  const reader = new FileReader()
  reader.readAsDataURL(file)
  
  reader.onload = async () => {
    try {
      const updatedUser = await request('/api/users/avatar', {
        method: 'POST',
        body: JSON.stringify({ 
          username: store.currentUser.username, 
          avatar: reader.result 
        })
      })
      if (updatedUser) {
        store.login(updatedUser)
        store.showNotification('头像更新成功！')
      }
    } catch (e) {
      console.error(e)
      store.showNotification(e.message || '上传失败', 'error') 
    }
  }
}
</script>

<template>
  <div class="container mx-auto px-4 py-8 max-w-6xl min-h-screen">

    <div v-if="showAddressModal"
      class="fixed inset-0 bg-black/60 z-[100] flex items-center justify-center p-4 backdrop-blur-sm transition-opacity">
      <div class="bg-white rounded-2xl p-8 w-full max-w-md shadow-2xl animate-scale-up border border-slate-100">
        <h3 class="text-xl font-bold mb-6 text-slate-900">新增地址</h3>
        <div class="space-y-4">
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1 block">联系人</label>
            <input v-model="newAddress.contact" placeholder="例如: 张三"
              class="w-full border border-slate-300 p-3 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition">
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1 block">手机号</label>
            <input v-model="newAddress.phone" placeholder="例如: 13800138000"
              class="w-full border border-slate-300 p-3 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition">
          </div>

          <div>
            <div class="flex justify-between items-center mb-1">
              <label class="text-xs font-bold text-slate-500 block">详细地址</label>
              <button @click="locateUser"
                class="flex items-center gap-1 text-xs text-blue-600 hover:text-blue-700 transition font-bold"
                :disabled="isLocating">
                <svg v-if="!isLocating" class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"></path>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"></path>
                </svg>
                <svg v-else class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <span>{{ isLocating ? '高德定位中...' : '定位当前位置' }}</span>
              </button>
            </div>
            <textarea v-model="newAddress.detail" placeholder="点击定位自动填写"
              class="w-full border border-slate-300 p-3 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition resize-none h-24"></textarea>
          </div>

          <div>
            <label class="text-xs font-bold text-slate-500 mb-2 block">标签</label>
            <div class="flex gap-3">
              <button @click="newAddress.tag = '家'"
                :class="`flex-1 py-2.5 rounded-xl border font-bold text-sm transition ${newAddress.tag === '家' ? 'bg-slate-800 text-white border-slate-800' : 'border-slate-200 text-slate-500 hover:border-slate-300'}`">家</button>
              <button @click="newAddress.tag = '公司'"
                :class="`flex-1 py-2.5 rounded-xl border font-bold text-sm transition ${newAddress.tag === '公司' ? 'bg-slate-800 text-white border-slate-800' : 'border-slate-200 text-slate-500 hover:border-slate-300'}`">公司</button>
            </div>
          </div>
        </div>
        <div class="mt-8 flex gap-4">
          <button @click="showAddressModal = false"
            class="flex-1 py-3 text-slate-500 hover:bg-slate-50 rounded-xl font-medium transition">取消</button>
          <button @click="saveAddress"
            class="flex-1 py-3 bg-blue-900 text-white rounded-xl font-bold shadow-lg hover:bg-blue-800 transition transform hover:-translate-y-0.5">保存</button>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
      <div class="lg:col-span-4 space-y-6">
        <div
          class="bg-white rounded-3xl p-6 shadow-xl shadow-slate-200/50 border border-white relative overflow-hidden group">
          <div class="absolute top-0 left-0 w-full h-24 bg-gradient-to-r from-blue-900 to-blue-800"></div>
          <div class="relative z-10 flex flex-col items-center mt-4">
            <div class="relative cursor-pointer">
              <input type="file" accept="image/*" class="absolute inset-0 w-full h-full opacity-0 z-20 cursor-pointer"
                @change="handleAvatarUpload" />
              <div class="w-20 h-20 rounded-full border-4 border-white shadow-lg overflow-hidden bg-slate-100">
                <img v-if="store.currentUser?.avatar" :src="store.currentUser.avatar"
                  class="w-full h-full object-cover" />
                <div v-else class="w-full h-full flex items-center justify-center text-2xl font-bold text-slate-300">
                  {{ store.currentUser?.displayName?.charAt(0) }}
                </div>
              </div>
              <div class="absolute bottom-0 right-0 bg-white rounded-full p-1 shadow-sm border border-slate-100">
                <img src="/icons/icon-search.png" class="w-3 h-3 opacity-50" />
              </div>
            </div>
            <div class="text-center mt-3">
              <h2 class="text-xl font-bold text-slate-800">{{ store.currentUser?.displayName || '未设置昵称' }}</h2>
              <div class="flex items-center justify-center gap-2 mt-1">
                <span class="text-xs text-slate-400">会员名:</span>
                <span class="font-mono text-xs bg-slate-100 px-2 py-0.5 rounded text-slate-600 border border-slate-200">
                  {{ store.currentUser?.username }}
                </span>
              </div>
            </div>
            <div class="flex justify-between w-full mt-6 px-4 pt-6 border-t border-slate-100">
              <div class="text-center">
                <div class="text-lg font-bold text-slate-800">{{ orders.length }}</div>
                <div class="text-xs text-slate-400">全部订单</div>
              </div>
              <div class="text-center">
                <div class="text-lg font-bold text-slate-800">0</div>
                <div class="text-xs text-slate-400">优惠券</div>
              </div>
              <div class="text-center">
                <div class="text-lg font-bold text-slate-800">{{ store.currentUser?.points || 0 }}</div>
                <div class="text-xs text-slate-400">积分</div>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-3xl p-6 shadow-sm border border-slate-100">
          <div class="flex justify-between items-center mb-4">
            <h3 class="font-bold text-slate-800">收货地址</h3>
            <button @click="showAddressModal = true" class="text-xs text-blue-600 hover:underline font-medium">+
              新增</button>
          </div>
          <div v-if="store.currentUser?.addresses?.length > 0" class="space-y-3">
            <div v-for="(addr, index) in store.currentUser.addresses" :key="index"
              class="p-4 rounded-xl bg-slate-50 border border-slate-100 relative group hover:border-blue-300 transition">
              <button @click="removeAddress(index)"
                class="absolute top-2 right-2 text-slate-300 hover:text-red-500 opacity-0 group-hover:opacity-100 transition px-2">×</button>
              <div class="flex items-start gap-4">
                <div class="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 font-bold text-xs"
                  :class="addr.tag === '家' ? 'bg-orange-100 text-orange-700' : 'bg-blue-100 text-blue-700'">
                  {{ addr.tag }}
                </div>
                <div class="flex-1 min-w-0 pt-0.5">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="font-bold text-sm text-slate-800">{{ addr.contact }}</span>
                    <span class="text-xs text-slate-400 font-mono">{{ addr.phone }}</span>
                    <span v-if="addr.isDefault"
                      class="text-[10px] bg-red-50 text-red-500 px-1.5 rounded border border-red-100">默认</span>
                  </div>
                  <p class="text-xs text-slate-500 leading-relaxed truncate">{{ addr.detail }}</p>
                </div>
              </div>
            </div>
          </div>
          <div v-else
            class="text-center py-8 text-slate-400 text-xs bg-slate-50 rounded-xl border border-dashed border-slate-200 cursor-pointer hover:bg-slate-100 transition"
            @click="showAddressModal = true">
            暂无地址，点击添加
          </div>
        </div>
      </div>

      <div class="lg:col-span-8">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-xl font-bold text-slate-800">我的订单记录</h2>
          <div class="flex gap-2">
            <input v-model="searchQuery" type="text" placeholder="搜索订单号/商品..."
              class="bg-white border border-slate-200 rounded-lg px-3 py-1.5 text-sm w-48 focus:outline-none focus:border-blue-500 transition">
          </div>
        </div>
        <div v-if="filteredOrders.length > 0" class="space-y-5">
          <div v-for="order in filteredOrders" :key="order.id"
            class="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm hover:shadow-lg transition-all duration-300 relative group">

            <button @click="deleteOrder(order.id)"
              class="absolute top-2 right-2 z-10 text-slate-400 hover:text-red-600 transition p-1.5 hover:bg-red-50 rounded-full opacity-0 group-hover:opacity-100"
              title="删除订单">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>

            <div class="flex justify-between items-center pb-4 border-b border-slate-50 mb-4">
              <span class="text-xs font-mono text-slate-400">Order #{{ 20250000 + order.id }}</span>
              <div class="flex items-center gap-3 pr-6">
                <span class="text-xs text-slate-400">{{ formatDate(order.createTime) }}</span>
                <span :class="`px-2.5 py-0.5 text-xs font-bold rounded border ${getStatusStyle(order.status)}`">
                  {{ order.status }}
                </span>
              </div>
            </div>
            <div class="flex gap-5">
              <div class="w-20 h-20 bg-slate-100 rounded-lg overflow-hidden border border-slate-200 flex-shrink-0">
                <img :src="order.items && order.items.length > 0 ? order.items[0].imageUrl : '/images/default.jpg'"
                  class="w-full h-full object-cover" />
              </div>
              <div class="flex-1 flex flex-col justify-between">
                <div>
                  <h3 class="font-bold text-slate-800 text-base mb-1 line-clamp-1">{{ order.productNames }}</h3>
                  <p class="text-xs text-slate-400">规格：标准装 | 配送：顺丰冷链</p>
                </div>
                <div class="flex justify-between items-end">
                  <span class="text-lg font-bold text-slate-900 font-serif-sc">¥{{ order.totalPrice }}</span>
                  <div class="flex gap-2">
                    <button v-if="order.status !== '已送达'" @click="confirmReceipt(order)"
                      class="px-3 py-1.5 bg-green-600 text-white rounded-md text-xs hover:bg-green-700 transition shadow-md shadow-green-600/20">确认收货</button>

                    <button @click="applyRefund"
                      class="px-3 py-1.5 border border-slate-200 rounded-md text-xs text-slate-600 hover:bg-slate-50 transition">申请售后</button>
                    <button @click="buyAgain(order.productNames)"
                      class="px-3 py-1.5 bg-blue-900 text-white rounded-md text-xs hover:bg-blue-800 transition shadow-md shadow-blue-900/10">再来一单</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else
          class="bg-white rounded-3xl border border-dashed border-slate-200 p-12 text-center h-96 flex flex-col items-center justify-center">
          <div class="w-20 h-20 bg-slate-50 rounded-full flex items-center justify-center mb-4">
            <img src="/icons/empty-order.png" class="w-10 h-10 opacity-40 grayscale" />
          </div>
          <h3 class="font-bold text-slate-800 mb-1">{{ searchQuery ? '未找到相关订单' : '暂无订单' }}</h3>
          <p class="text-sm text-slate-400 mb-6">{{ searchQuery ? '换个关键词试试？' : '快去挑选您心仪的海鲜吧' }}</p>
          <button @click="router.push('/')"
            class="px-6 py-2 bg-slate-900 text-white text-sm rounded-full hover:bg-slate-800 transition">去逛逛</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.input-field {
  @apply w-full border border-slate-200 bg-slate-50 p-3 rounded-xl focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 outline-none transition text-sm;
}

.tag-btn {
  @apply flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-500 font-bold text-sm hover:border-slate-300 transition;
}

.tag-btn.active {
  @apply bg-slate-900 text-white border-slate-900 shadow-md;
}

.btn-primary {
  @apply flex-1 py-3 bg-blue-600 text-white rounded-xl font-bold shadow-lg shadow-blue-600/30 hover:bg-blue-700 transition active:scale-95;
}

.btn-secondary {
  @apply flex-1 py-3 text-slate-500 hover:bg-slate-50 rounded-xl font-medium transition;
}

@keyframes shine {
  100% {
    left: 125%;
  }
}

.animate-shine {
  animation: shine 2.5s infinite;
}
</style>