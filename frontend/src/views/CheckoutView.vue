<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '../store.js'
import { request } from '@/utils/request'

const router = useRouter()
const paymentMethod = ref('alipay')
const selectedAddressId = ref(null)
const showAddressModal = ref(false)
const newAddress = ref({ contact: '', phone: '', detail: '', tag: '家' })
const isLocating = ref(false)

// 优惠券相关状态
const availableCoupons = ref([]);
const selectedCouponId = ref(null);

const myAddresses = computed(() => store.currentUser?.addresses || [])

// ✅ 修复点 1：使用 store.cart 替代 store.items，并增加 || [] 兜底
const cartItems = computed(() => store.cart || [])

// 1. 计算商品总价
const subTotal = computed(() => {
  // ✅ 修复点 2：基于修复后的 cartItems 计算
  return cartItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0);
});

// 2. 计算运费
const freight = computed(() => subTotal.value > 200 ? 0 : 20);

// 3. 计算当前选中的优惠券
const selectedCoupon = computed(() => {
  if (!selectedCouponId.value) return null;
  return availableCoupons.value.find(c => c.id === selectedCouponId.value);
});

// 4. 计算最终实付
const finalPrice = computed(() => {
  let discount = selectedCoupon.value ? selectedCoupon.value.amount : 0;
  let total = subTotal.value - discount + freight.value;
  return total > 0 ? total.toFixed(2) : '0.00';
});

// 拉取我的优惠券
const fetchCoupons = async () => {
  try {
    const username = store.currentUser?.username;
    if (!username) return;

    const res = await request.get(`/api/coupons/my?username=${username}`);
    if (res && Array.isArray(res)) {
      availableCoupons.value = res.filter(c =>
        c.status === 'UNUSED' && c.minSpend <= subTotal.value
      );

      if (availableCoupons.value.length > 0) {
        availableCoupons.value.sort((a, b) => b.amount - a.amount);
        selectedCouponId.value = availableCoupons.value[0].id;
      }
    }
  } catch (e) {
    console.error("加载优惠券失败", e);
  }
};

onMounted(() => {
  if (!store.currentUser) {
    store.showNotification('请先登录', 'error')
    router.push('/login')
    return
  }
  if (myAddresses.value.length > 0) {
    const def = myAddresses.value.find(a => a.isDefault)
    selectedAddressId.value = def ? def.id : myAddresses.value[0].id
  }
  fetchCoupons();
})

// === 高德定位逻辑 (保持不变) ===
const locateUser = () => {
  if (typeof AMap === 'undefined') {
    store.showNotification('地图加载中...请检查网络', 'error')
    return
  }
  isLocating.value = true
  store.showNotification('正在调用高德定位...')
  AMap.plugin('AMap.Geolocation', function () {
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true, timeout: 10000, needAddress: true, extensions: 'all'
    })
    geolocation.getCurrentPosition(function (status, result) {
      isLocating.value = false
      if (status === 'complete') {
        newAddress.value.detail = result.formattedAddress
        store.showNotification('定位成功')
      } else {
        if (confirm('定位失败。是否填入测试地址？')) {
          newAddress.value.detail = "浙江省舟山市普陀区沈家门渔港路88号"
        }
      }
    })
  })
}

// === 保存地址 ===
const saveAddress = async () => {
  if (!newAddress.value.contact || !newAddress.value.phone || !newAddress.value.detail) {
    store.showNotification('请填写完整信息', 'error')
    return
  }
  const isFirst = myAddresses.value.length === 0
  const updatedAddresses = [...myAddresses.value, { ...newAddress.value, isDefault: isFirst }]
  try {
    const updatedUser = await request('/api/users/address', {
      method: 'POST',
      body: JSON.stringify({ username: store.currentUser.username, addresses: updatedAddresses })
    })
    store.login(updatedUser)
    showAddressModal.value = false
    newAddress.value = { contact: '', phone: '', detail: '', tag: '家' }
    if (isFirst && store.currentUser.addresses.length > 0) {
      selectedAddressId.value = store.currentUser.addresses[0].id
    }
    store.showNotification('地址添加成功')
  } catch (e) {
    store.showNotification('保存失败', 'error')
  }
}

// === 提交订单 ===
const submitOrder = async () => {
  // ✅ 修复点 3：使用 cartItems 判断长度
  if (cartItems.value.length === 0) {
    store.showNotification('购物车是空的', 'warning')
    return
  }
  if (!selectedAddressId.value) {
    store.showNotification('请选择收货地址', 'warning')
    return
  }

  const currentAddress = myAddresses.value.find(a => a.id === selectedAddressId.value)
  if (!currentAddress) {
    store.showNotification('地址数据异常', 'error')
    return
  }

  const payload = {
    username: store.currentUser.username,
    // ✅ 修复点 4：使用 cartItems 映射数据
    items: cartItems.value.map(item => ({
      id: parseInt(item.id),
      quantity: parseInt(item.quantity)
    })),
    address: {
      name: currentAddress.contact,
      phone: currentAddress.phone,
      detail: currentAddress.detail
    },
    couponId: selectedCouponId.value
  }

  const currentFinalPrice = finalPrice.value

  try {
    await request('/api/orders', {
      method: 'POST',
      body: JSON.stringify(payload)
    })

    store.clearCart()

    router.push({
      path: '/payment-success',
      query: {
        amount: currentFinalPrice,
        method: paymentMethod.value
      }
    })

  } catch (e) {
    console.error(e)
    store.showNotification(e.message || '下单失败', 'error')
  }
}

const getTagColor = (tag) => {
  if (tag === '家') return 'bg-orange-100 text-orange-700'
  if (tag === '公司') return 'bg-blue-100 text-blue-700'
  return 'bg-slate-100 text-slate-600'
}
</script>

<template>
  <div class="container mx-auto px-4 py-12 max-w-6xl min-h-[80vh]">

    <div v-if="showAddressModal"
      class="fixed inset-0 bg-slate-900/60 z-[100] flex items-center justify-center p-4 backdrop-blur-sm transition-all">
      <div class="bg-white rounded-2xl p-8 w-full max-w-md shadow-2xl animate-scale-up">
        <h3 class="text-xl font-bold mb-6 text-slate-900">添加收货地址</h3>
        <div class="space-y-4">
          <input v-model="newAddress.contact" placeholder="联系人" class="input-field">
          <input v-model="newAddress.phone" placeholder="手机号" class="input-field">
          <div class="relative">
            <button @click="locateUser"
              class="absolute right-3 top-3 text-xs text-blue-600 font-bold flex items-center gap-1"
              :disabled="isLocating">
              <span v-if="isLocating" class="animate-spin">⏳</span><span v-else>📍</span>定位
            </button>
            <textarea v-model="newAddress.detail" placeholder="详细地址"
              class="input-field resize-none h-24 pt-3"></textarea>
          </div>
          <div class="flex gap-3">
            <button @click="newAddress.tag = '家'" :class="`tag-btn ${newAddress.tag === '家' ? 'active' : ''}`">🏠
              家</button>
            <button @click="newAddress.tag = '公司'" :class="`tag-btn ${newAddress.tag === '公司' ? 'active' : ''}`">🏢
              公司</button>
          </div>
        </div>
        <div class="mt-8 flex gap-4">
          <button @click="showAddressModal = false" class="btn-secondary">取消</button>
          <button @click="saveAddress" class="btn-primary">保存</button>
        </div>
      </div>
    </div>

    <div class="flex flex-col lg:flex-row gap-12 items-start">

      <div class="flex-1 w-full space-y-10">
        <button @click="router.back()"
          class="text-sm font-medium text-slate-400 hover:text-slate-800 transition flex items-center gap-1 group">
          <span class="group-hover:-translate-x-1 transition">←</span> 返回购物车
        </button>

        <section>
          <div class="flex justify-between items-end mb-5">
            <h2 class="text-xl font-bold text-slate-900">收货地址</h2>
            <button v-if="myAddresses.length > 0" @click="showAddressModal = true"
              class="text-xs bg-slate-100 text-slate-600 px-3 py-1.5 rounded-full hover:bg-slate-200 transition font-bold">+
              新地址</button>
          </div>

          <div v-if="myAddresses.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div v-for="addr in myAddresses" :key="addr.id" @click="selectedAddressId = addr.id"
              :class="['address-card group', selectedAddressId === addr.id ? 'active ring-2 ring-blue-600 bg-blue-50/30' : 'bg-white hover:border-slate-300']">
              <div v-if="selectedAddressId === addr.id"
                class="absolute top-0 right-0 bg-blue-600 text-white w-6 h-6 flex items-center justify-center rounded-bl-lg shadow-sm z-10">
                <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path>
                </svg>
              </div>
              <div class="flex justify-between items-center mb-3">
                <div class="flex items-center gap-2">
                  <span class="font-bold text-slate-800 text-lg">{{ addr.contact }}</span>
                  <span :class="`px-2 py-0.5 rounded text-[10px] font-bold ${getTagColor(addr.tag)}`">{{ addr.tag
                    }}</span>
                </div>
                <span class="text-slate-400 text-sm font-mono">{{ addr.phone }}</span>
              </div>
              <p class="text-sm text-slate-500 leading-relaxed line-clamp-2">{{ addr.detail }}</p>
            </div>
          </div>

          <div v-else @click="showAddressModal = true"
            class="border-2 border-dashed border-slate-200 rounded-2xl p-8 flex flex-col items-center justify-center cursor-pointer hover:border-blue-400 hover:bg-blue-50/30 transition group">
            <span class="text-3xl mb-2 group-hover:scale-110 transition">🏡</span>
            <p class="text-slate-500 font-bold group-hover:text-blue-600">添加收货地址</p>
          </div>
        </section>

        <section class="bg-orange-50 border border-orange-100 p-5 rounded-2xl relative overflow-hidden">
          <div class="absolute top-0 right-0 w-20 h-20 bg-orange-100 rounded-full -mr-10 -mt-10 blur-xl"></div>
          <h2 class="text-lg font-bold text-orange-800 mb-3 flex items-center gap-2 relative z-10">
            🎟️ 优惠券 <span v-if="availableCoupons.length > 0"
              class="bg-orange-200 text-orange-700 text-[10px] px-2 py-0.5 rounded-full">{{ availableCoupons.length
              }}张可用</span>
          </h2>
          <div v-if="availableCoupons.length > 0" class="relative z-10">
            <select v-model="selectedCouponId"
              class="w-full p-3 border border-orange-200 rounded-xl bg-white text-slate-700 outline-none focus:ring-2 focus:ring-orange-300 appearance-none cursor-pointer font-medium">
              <option :value="null">不使用优惠券</option>
              <option v-for="c in availableCoupons" :key="c.id" :value="c.id">
                - ¥{{ c.amount }} : {{ c.couponName }} (满{{ c.minSpend }}可用)
              </option>
            </select>
            <div class="absolute right-4 top-4 pointer-events-none text-orange-400">▼</div>
            <p v-if="selectedCoupon"
              class="text-xs text-orange-600 mt-3 font-bold flex items-center gap-1 animate-pulse">🎉 已抵扣 ¥{{
                selectedCoupon.amount }}</p>
          </div>
          <div v-else class="text-sm text-orange-400/70 italic relative z-10">暂无可用优惠券 (需满足使用门槛)</div>
        </section>

        <section>
          <h2 class="text-xl font-bold text-slate-900 mb-5">支付方式</h2>
          <div class="flex gap-4">
            <div @click="paymentMethod = 'alipay'"
              :class="['flex-1 p-4 rounded-2xl border-2 cursor-pointer flex items-center justify-center gap-3 transition h-20 shadow-sm relative overflow-hidden', paymentMethod === 'alipay' ? 'border-[#1677FF] bg-[#1677FF]/5 ring-1 ring-[#1677FF]' : 'border-slate-100 bg-white hover:border-slate-300']">
              <div v-if="paymentMethod === 'alipay'"
                class="absolute top-0 right-0 bg-[#1677FF] text-white w-5 h-5 flex items-center justify-center rounded-bl-lg">
                <svg class="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <img src="/icons/alipay.png" class="w-8 h-8 object-contain" alt="Alipay">
              <span class="font-bold text-slate-700">支付宝</span>
            </div>
            <div @click="paymentMethod = 'wechat'"
              :class="['flex-1 p-4 rounded-2xl border-2 cursor-pointer flex items-center justify-center gap-3 transition h-20 shadow-sm relative overflow-hidden', paymentMethod === 'wechat' ? 'border-[#07C160] bg-[#07C160]/5 ring-1 ring-[#07C160]' : 'border-slate-100 bg-white hover:border-slate-300']">
              <div v-if="paymentMethod === 'wechat'"
                class="absolute top-0 right-0 bg-[#07C160] text-white w-5 h-5 flex items-center justify-center rounded-bl-lg">
                <svg class="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <img src="/icons/wechat.png" class="w-8 h-8 object-contain" alt="WeChat">
              <span class="font-bold text-slate-700">微信支付</span>
            </div>
          </div>
        </section>
      </div>

      <div class="w-full lg:w-96 sticky top-24">
        <div
          class="bg-white rounded-3xl shadow-xl shadow-slate-200/50 border border-slate-100 overflow-hidden relative">
          <div class="h-2 bg-slate-900 w-full"></div>
          <div class="p-8">
            <h3 class="font-serif-sc font-bold text-slate-800 text-lg mb-6">订单明细</h3>
            <div class="space-y-4 mb-6">
              <div class="flex justify-between text-slate-500 text-sm"><span>商品小计</span><span
                  class="font-medium text-slate-900">¥{{ subTotal.toFixed(2) }}</span></div>
              <div class="flex justify-between text-slate-500 text-sm"><span>运费</span><span
                  :class="freight === 0 ? 'text-green-600 font-bold' : 'text-slate-900'">{{ freight === 0 ? '免运费' : `+
                  ¥${freight}` }}</span></div>
              <div v-if="selectedCoupon" class="flex justify-between text-orange-500 text-sm font-bold animate-pulse">
                <span>优惠券抵扣</span><span>- ¥{{ selectedCoupon.amount }}</span></div>
            </div>
            <div class="border-b-2 border-dashed border-slate-100 -mx-8 mb-6 relative">
              <div class="absolute -left-2 -top-2 w-4 h-4 bg-slate-50 rounded-full"></div>
              <div class="absolute -right-2 -top-2 w-4 h-4 bg-slate-50 rounded-full"></div>
            </div>
            <div class="flex justify-between items-baseline mb-8">
              <span class="font-bold text-slate-800 text-lg">实付款</span>
              <span class="text-4xl font-bold text-blue-900 font-serif-sc"><span class="text-2xl mr-1">¥</span>{{
                finalPrice }}</span>
            </div>
            <button @click="submitOrder"
              class="w-full bg-slate-900 hover:bg-blue-900 text-white font-bold py-4 rounded-xl shadow-lg transition-all transform active:scale-95 flex items-center justify-center gap-2 group">
              <span>{{ selectedAddressId ? '确认支付' : '添加地址并支付' }}</span>
              <svg class="w-5 h-5 group-hover:translate-x-1 transition" fill="none" stroke="currentColor"
                viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3">
                </path>
              </svg>
            </button>
            <p class="text-[10px] text-center text-slate-400 mt-4">支付即代表同意《用户购买协议》</p>
          </div>
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
  @apply flex-1 py-3 bg-blue-600 text-white rounded-xl font-bold shadow-lg hover:bg-blue-700 transition active:scale-95;
}

.btn-secondary {
  @apply flex-1 py-3 text-slate-500 hover:bg-slate-50 rounded-xl font-medium transition;
}

.address-card {
  @apply p-5 rounded-2xl border-2 cursor-pointer transition-all relative overflow-hidden;
}

.animate-scale-up {
  animation: scaleUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes scaleUp {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }

  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
</style>