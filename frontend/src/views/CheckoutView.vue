<script setup>
import { ref, computed, onMounted, watch, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '../store.js'
import { request } from '@/utils/request'

// ==========================================
// 🔴 高德地图配置区域 (请在此处填入 Key)
// ==========================================
window._AMapSecurityConfig = {
  // 请替换为你在高德开放平台申请的 Key
  securityJsCode: '在此填入你的安全密钥_例如_a1b2c3d4e5f6',
};
// 确保在 index.html 中正确引入了高德地图 JS API
// ==========================================

const router = useRouter()
const paymentMethod = ref('alipay')
const selectedAddressId = ref(null)
const showAddressModal = ref(false)

// ✅ 新增：地址模式切换
const addressMode = ref('map') // 'map' | 'manual'

// 地址表单 (使用 reactive 以便在不同模式下共享和更新)
const newAddress = reactive({
  contact: '',
  phone: '',
  detail: '',
  tag: '家'
})

const isLocating = ref(false)
const loading = ref(false)

// 优惠券相关
const availableCoupons = ref([]);
const selectedCouponId = ref(null);

const myAddresses = computed(() => store.currentUser?.addresses || [])
const cartItems = computed(() => store.cart || [])

// 计算逻辑
const subTotal = computed(() => cartItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0));
const freight = computed(() => subTotal.value > 200 ? 0 : 20);
const selectedCoupon = computed(() => {
  if (!selectedCouponId.value) return null;
  return availableCoupons.value.find(c => c.id === selectedCouponId.value);
});
const finalPrice = computed(() => {
  let discount = selectedCoupon.value ? selectedCoupon.value.amount : 0;
  // 优惠券不能抵扣运费，且不能超出商品总价
  const actualDiscount = Math.min(discount, subTotal.value);
  let total = subTotal.value - actualDiscount + freight.value;
  return total > 0 ? total.toFixed(2) : '0.00';
});

// 拉取优惠券 (移除对 store.myCoupons 的合并，仅依赖远程数据)
const fetchCoupons = async () => {
  try {
    const username = store.currentUser?.username;
    if (!username) return;

    let remoteCoupons = [];
    try {
      // ✅ 依赖服务器获取优惠券，不再合并本地缓存
      const res = await request.get(`/api/coupons/my?username=${username}`);
      if (res && Array.isArray(res)) remoteCoupons = res;
      // ✅ 修复：将远程获取的优惠券同步到 store 内存，以供 ProfileView 的 couponCount 使用
      store.myCoupons = remoteCoupons;
    } catch (e) { console.error("API Error", e); }

    availableCoupons.value = remoteCoupons.filter(c => {
      const isUnused = c.status === 'UNUSED';
      // 优惠券门槛校验
      const isThresholdMet = (c.minSpend || 0) <= subTotal.value;
      return isUnused && isThresholdMet;
    });

    if (availableCoupons.value.length > 0) {
      availableCoupons.value.sort((a, b) => b.amount - a.amount);
      if (!selectedCouponId.value) selectedCouponId.value = availableCoupons.value[0].id;
    }
  } catch (e) { console.error(e); }
};

watch(subTotal, () => {
  fetchCoupons();
  if (selectedCouponId.value && !availableCoupons.value.find(c => c.id === selectedCouponId.value)) {
    selectedCouponId.value = null;
  }
});

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

// === 定位逻辑 (核心修复) ===
const locateUser = () => {
  if (typeof AMap === 'undefined') {
    store.showNotification('地图组件未加载，请检查 Key 或网络', 'error')
    addressMode.value = 'manual' // 自动降级
    return
  }

  // 修复：每次点击时清空旧地址
  newAddress.detail = ''

  isLocating.value = true
  AMap.plugin('AMap.Geolocation', function () {
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 8000 // 8秒超时
    })

    geolocation.getCurrentPosition(function (status, result) {
      isLocating.value = false
      if (status === 'complete') {
        newAddress.detail = result.formattedAddress
        store.showNotification('定位成功')
      } else {
        console.error("定位失败:", result.message || '未知错误')
        store.showNotification('定位失败，请切换到手动输入', 'warning')
        // 修复：定位失败，自动切换到手动输入模式
        addressMode.value = 'manual'
      }
    })
  })
}

// === 打开地址弹窗 (新增) ===
const openAddressModal = () => {
  // 重置表单
  newAddress.contact = ''
  newAddress.phone = ''
  newAddress.detail = ''
  newAddress.tag = '家'
  // 默认模式
  addressMode.value = 'map'
  showAddressModal.value = true
}

// === 保存地址 (修改：使用 newAddress) ===
const saveAddress = async () => {
  if (!newAddress.contact || !newAddress.detail || !newAddress.phone) {
    store.showNotification('请完整填写联系人、电话和详细地址', 'warning')
    return
  }
  const isFirst = myAddresses.value.length === 0

  // 将 reactive 对象解构为普通对象以保存
  const addressToSave = { ...newAddress, isDefault: isFirst }
  const updatedAddresses = [...myAddresses.value, addressToSave]

  try {
    const updatedUser = await request('/api/users/address', {
      method: 'POST',
      body: JSON.stringify({ username: store.currentUser.username, addresses: updatedAddresses })
    })
    store.login(updatedUser)
    showAddressModal.value = false
    if (isFirst && store.currentUser.addresses.length > 0) {
      // 找到新添加的地址并设为选中
      selectedAddressId.value = store.currentUser.addresses.find(a =>
        a.detail === newAddress.detail && a.contact === newAddress.contact
      )?.id || store.currentUser.addresses[0].id
    }
    store.showNotification('地址保存成功')
  } catch (e) { store.showNotification('保存失败', 'error') }
}

// === 提交订单 ===
const submitOrder = async () => {
  if (cartItems.value.length === 0) return store.showNotification('购物车是空的', 'warning')
  if (!selectedAddressId.value) return store.showNotification('请选择收货地址', 'warning')

  const currentAddress = myAddresses.value.find(a => a.id === selectedAddressId.value)
  if (!currentAddress) return store.showNotification('地址数据异常', 'error')

  loading.value = true

  // 1. 构造符合 OrderController 的 Payload
  const payload = {
    username: store.currentUser.username,
    items: cartItems.value.map(item => ({
      id: item.id,
      quantity: item.quantity
    })),
    address: {
      name: currentAddress.contact,
      phone: currentAddress.phone,
      detail: currentAddress.detail
    },
    couponId: selectedCouponId.value || null
  }

  const currentFinalPrice = finalPrice.value

  try {
    // 2. 发送请求
    await request('/api/orders', { method: 'POST', body: JSON.stringify(payload) })

    // 3. 处理本地优惠券状态 (前端视觉更新)
    if (selectedCouponId.value) {
      const localCoupon = store.myCoupons.find(c => c.id === selectedCouponId.value);
      if (localCoupon) localCoupon.status = 'USED';
    }

    store.clearCart()
    router.push({ path: '/payment-success', query: { amount: currentFinalPrice, method: paymentMethod.value } })
  } catch (e) {
    console.error("Order Submit Error:", e);
    // 提示用户可能的原因
    store.showNotification(e.message || '下单失败', 'error')
  } finally {
    loading.value = false
  }
}

const getTagColor = (tag) => {
  if (tag === '家') return 'bg-orange-100 text-orange-700'
  if (tag === '公司') return 'bg-blue-100 text-blue-700'
  return 'bg-slate-100 text-slate-600'
}
</script>

<template>
  <div class="min-h-screen bg-[#F8FAFC] pb-20 relative overflow-hidden font-sans">

    <div
      class="absolute top-0 left-0 w-full h-[600px] bg-gradient-to-b from-blue-50/80 to-transparent pointer-events-none">
    </div>
    <div class="absolute -top-40 -right-40 w-[500px] h-[500px] bg-blue-200/20 rounded-full blur-3xl animate-pulse">
    </div>
    <div class="absolute top-20 -left-20 w-96 h-96 bg-purple-200/20 rounded-full blur-3xl animate-pulse delay-1000">
    </div>

    <div class="container mx-auto px-4 py-8 relative z-10 max-w-6xl">

      <div class="flex items-center gap-4 mb-8">
        <button @click="router.back()"
          class="w-10 h-10 rounded-full bg-white/80 backdrop-blur-md border border-slate-200 flex items-center justify-center hover:bg-slate-100 transition shadow-sm group">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-slate-600 group-hover:-translate-x-1 transition"
            fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <h1 class="text-2xl font-bold text-slate-800">确认订单</h1>
      </div>

      <Transition name="fade">
        <div v-if="showAddressModal"
          class="fixed inset-0 bg-slate-900/60 z-[100] flex items-center justify-center p-4 backdrop-blur-sm">
          <div
            class="bg-white rounded-3xl p-6 w-full max-w-md shadow-2xl animate-scale-up border border-slate-100 flex flex-col max-h-[90vh]">
            <h3 class="text-xl font-bold mb-4 text-slate-900">添加收货地址</h3>

            <div class="flex p-1 bg-slate-100 rounded-xl mb-6">
              <button @click="addressMode = 'map'"
                :class="['flex-1 py-2 text-sm font-bold rounded-lg transition-all', addressMode === 'map' ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500']">
                📍 智能定位
              </button>
              <button @click="addressMode = 'manual'"
                :class="['flex-1 py-2 text-sm font-bold rounded-lg transition-all', addressMode === 'manual' ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500']">
                📝 手动输入
              </button>
            </div>

            <div class="space-y-4 overflow-y-auto pr-1 custom-scrollbar">
              <input v-model="newAddress.contact" placeholder="联系人姓名" class="input-field-glass">
              <input v-model="newAddress.phone" placeholder="手机号码" class="input-field-glass">

              <div class="relative">
                <textarea v-model="newAddress.detail"
                  :placeholder="addressMode === 'map' ? '点击右侧定位图标获取地址，或切换手动输入' : '请输入省/市/区/街道/门牌号'"
                  class="input-field-glass resize-none h-24 pt-3 pr-16"></textarea>

                <button v-if="addressMode === 'map'" @click="locateUser"
                  class="absolute right-3 top-3 z-10 text-xs bg-blue-50 text-blue-600 px-2 py-1 rounded-md font-bold flex items-center gap-1 hover:bg-blue-100 transition"
                  :disabled="isLocating">
                  <span v-if="isLocating" class="animate-bounce">
                    <img src="/icons/location.png" class="w-5 h-5 object-contain" alt="定位中" />
                  </span>
                  <span v-else>
                    <img src="/icons/location.png" class="w-5 h-5 object-contain" alt="定位" />
                  </span>
                  <span>{{ isLocating ? '定位中...' : '定位' }}</span>
                </button>
              </div>

              <div class="flex gap-3 pt-2">
                <span class="text-xs font-bold text-slate-500 py-2.5">标签:</span>
                <button v-for="tag in ['家', '公司', '学校']" :key="tag" @click="newAddress.tag = tag"
                  :class="`tag-btn ${newAddress.tag === tag ? 'active' : ''}`">
                  {{ tag }}
                </button>
              </div>
            </div>

            <div class="mt-8 flex gap-4">
              <button @click="showAddressModal = false"
                class="flex-1 py-3 text-slate-500 hover:bg-slate-50 rounded-xl font-medium transition">取消</button>
              <button @click="saveAddress"
                class="flex-1 py-3 bg-blue-600 text-white rounded-xl font-bold shadow-lg hover:bg-blue-700 transition active:scale-95">保存地址</button>
            </div>
          </div>
        </div>
      </Transition>

      <div class="flex flex-col lg:flex-row gap-8">

        <div class="flex-1 space-y-6">

          <div class="bg-white/70 backdrop-blur-xl rounded-3xl p-6 border border-white/50 shadow-sm">
            <div class="flex justify-between items-center mb-6">
              <h2 class="text-lg font-bold text-slate-800 flex items-center gap-2"><span></span> 收货地址</h2>
              <button v-if="myAddresses.length > 0" @click="openAddressModal"
                class="text-xs font-bold text-blue-600 bg-blue-50 px-3 py-1.5 rounded-full hover:bg-blue-100 transition">+
                新增地址</button>
            </div>

            <div v-if="myAddresses.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div v-for="addr in myAddresses" :key="addr.id" @click="selectedAddressId = addr.id"
                class="relative p-5 rounded-2xl border-2 cursor-pointer transition-all duration-300 group overflow-hidden"
                :class="selectedAddressId === addr.id ? 'border-blue-500 bg-blue-50/50 shadow-lg shadow-blue-500/10' : 'border-slate-100 bg-white hover:border-blue-200'">
                <div v-if="selectedAddressId === addr.id"
                  class="absolute top-0 right-0 bg-blue-500 text-white w-6 h-6 flex items-center justify-center rounded-bl-xl shadow-sm z-10">
                  <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path>
                  </svg>
                </div>
                <div class="flex justify-between items-start mb-2">
                  <div class="flex items-center gap-2">
                    <span class="font-bold text-slate-800 text-lg">{{ addr.contact }}</span>
                    <span :class="`px-2 py-0.5 rounded text-[10px] font-bold ${getTagColor(addr.tag)}`">{{ addr.tag
                    }}</span>
                  </div>
                  <span class="text-slate-500 font-mono text-sm">{{ addr.phone }}</span>
                </div>
                <p class="text-sm text-slate-500 leading-relaxed line-clamp-2">{{ addr.detail }}</p>
              </div>
            </div>
            <div v-else @click="openAddressModal"
              class="border-2 border-dashed border-slate-300 rounded-2xl p-8 flex flex-col items-center justify-center cursor-pointer hover:border-blue-400 hover:bg-blue-50/30 transition group h-40">
              <span class="text-3xl mb-2 group-hover:scale-110 transition"></span>
              <p class="text-slate-500 font-bold group-hover:text-blue-600">添加收货地址</p>
            </div>
          </div>

          <div
            class="bg-white/70 backdrop-blur-xl rounded-3xl p-6 border border-white/50 shadow-sm relative overflow-hidden">
            <div
              class="absolute -right-10 -top-10 w-32 h-32 bg-orange-100 rounded-full blur-2xl opacity-50 pointer-events-none">
            </div>
            <h2 class="text-lg font-bold text-slate-800 mb-4 flex items-center gap-2 relative z-10">
              <span></span> 优惠券 <span v-if="availableCoupons.length > 0"
                class="bg-orange-100 text-orange-600 text-[10px] px-2 py-0.5 rounded-full">{{ availableCoupons.length
                }}张可用</span>
            </h2>
            <div class="relative z-10">
              <div v-if="availableCoupons.length > 0" class="relative group">
                <select v-model="selectedCouponId"
                  class="w-full p-4 pl-12 border border-orange-200 rounded-xl bg-gradient-to-r from-orange-50/50 to-white text-slate-700 outline-none focus:ring-2 focus:ring-orange-300 appearance-none cursor-pointer font-medium shadow-sm transition-all hover:border-orange-300">
                  <option :value="null">不使用优惠券</option>
                  <option v-for="c in availableCoupons" :key="c.id" :value="c.id">
                    - ¥{{ c.amount }} : {{ c.couponName }} (满{{ c.minSpend }}可用)
                  </option>
                </select>
                <div class="absolute left-4 top-1/2 -translate-y-1/2 text-xl">🎁</div>
                <div class="absolute right-4 top-1/2 -translate-y-1/2 text-orange-400 text-xs">▼</div>
                <p v-if="selectedCoupon"
                  class="text-xs text-orange-600 mt-2 font-bold flex items-center gap-1 animate-pulse pl-1">🎉 已成功抵扣 ¥{{
                    selectedCoupon.amount }}</p>
              </div>
              <div v-else
                class="flex items-center gap-3 p-4 bg-slate-50 rounded-xl border border-slate-100 text-slate-400">
                <span class="grayscale text-xl"><img src="/icons/nocode.png" class="w-5 h-5 object-contain"
                    alt="微信" /></span><span class="text-sm italic">暂无可用优惠券 (需满足使用门槛)</span>
              </div>
            </div>
          </div>

          <div class="bg-white/70 backdrop-blur-xl rounded-3xl p-6 border border-white/50 shadow-sm">
            <h2 class="text-lg font-bold text-slate-800 mb-4 flex items-center gap-2">
              <span></span> 支付方式
            </h2>
            <div class="grid grid-cols-2 gap-4">
              <div @click="paymentMethod = 'alipay'"
                class="flex items-center justify-center gap-3 py-4 rounded-xl border-2 cursor-pointer transition-all hover:shadow-md relative overflow-hidden"
                :class="paymentMethod === 'alipay' ? 'border-[#1677FF] bg-[#1677FF]/5' : 'border-slate-100 bg-white hover:border-slate-200'">
                <div v-if="paymentMethod === 'alipay'"
                  class="absolute top-0 right-0 bg-[#1677FF] w-6 h-6 flex items-center justify-center rounded-bl-xl shadow-sm z-10">
                  <span class="text-white text-xs font-bold">✓</span>
                </div>
                <img src="/icons/alipay.png" class="w-8 h-8 object-contain" alt="支付宝" />
                <span class="font-bold text-slate-700">支付宝</span>
              </div>

              <div @click="paymentMethod = 'wechatpay'"
                class="flex items-center justify-center gap-3 py-4 rounded-xl border-2 cursor-pointer transition-all hover:shadow-md relative overflow-hidden"
                :class="paymentMethod === 'wechatpay' ? 'border-[#07C160] bg-[#07C160]/5' : 'border-slate-100 bg-white hover:border-slate-200'">
                <div v-if="paymentMethod === 'wechatpay'"
                  class="absolute top-0 right-0 bg-[#07C160] w-6 h-6 flex items-center justify-center rounded-bl-xl shadow-sm z-10">
                  <span class="text-white text-xs font-bold">✓</span>
                </div>
                <img src="/icons/wechatpay.png" class="w-8 h-8 object-contain" alt="微信支付" />
                <span class="font-bold text-slate-700">微信支付</span>
              </div>
            </div>
          </div>
        </div>

        <div class="lg:w-96 flex-shrink-0">
          <div class="sticky top-24">
            <div
              class="bg-white/90 backdrop-blur-xl rounded-3xl shadow-2xl shadow-slate-200/50 overflow-hidden border border-slate-100/60">
              <div class="h-2 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500"></div>
              <div class="p-8">
                <h3 class="font-bold text-slate-800 mb-6 text-lg flex justify-between items-center">
                  订单明细 <span class="text-xs font-normal text-slate-400 bg-slate-100 px-2 py-1 rounded-full">{{
                    cartItems.length }} 件商品</span>
                </h3>

                <div class="space-y-3 mb-6 max-h-48 overflow-y-auto pr-2 custom-scrollbar">
                  <div v-for="item in cartItems" :key="item.id" class="flex justify-between items-center text-sm group">
                    <div class="flex items-center gap-3 overflow-hidden">
                      <img :src="item.imageUrl"
                        class="w-10 h-10 rounded-lg bg-slate-100 object-cover flex-shrink-0 border border-slate-100" />
                      <span class="text-slate-600 truncate group-hover:text-blue-600 transition">{{ item.name }}</span>
                    </div>
                    <div class="flex flex-col items-end flex-shrink-0">
                      <span class="font-bold text-slate-800">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
                      <span class="text-slate-400 text-xs">x{{ item.quantity }}</span>
                    </div>
                  </div>
                </div>

                <div class="border-t-2 border-dashed border-slate-200 my-6 relative">
                  <div class="absolute -left-10 -top-2 w-4 h-4 bg-[#F8FAFC] rounded-full"></div>
                  <div class="absolute -right-10 -top-2 w-4 h-4 bg-[#F8FAFC] rounded-full"></div>
                </div>

                <div class="space-y-3 mb-6">
                  <div class="flex justify-between text-sm text-slate-500"><span>商品小计</span><span
                      class="font-medium text-slate-800">¥{{ subTotal.toFixed(2) }}</span></div>
                  <div class="flex justify-between text-sm text-slate-500"><span>运费</span><span
                      :class="freight === 0 ? 'text-green-600 font-bold bg-green-50 px-2 rounded text-xs py-0.5' : 'font-medium text-slate-800'">{{
                        freight === 0 ? '免运费' : `+ ¥${freight}` }}</span></div>
                  <div v-if="selectedCoupon" class="flex justify-between text-sm text-orange-500"><span>优惠券</span><span
                      class="font-bold">- ¥{{ selectedCoupon.amount }}</span></div>
                </div>

                <div class="flex justify-between items-end mb-8 pt-4 border-t border-slate-100">
                  <span class="text-slate-800 font-bold text-lg">实付款</span>
                  <span class="text-4xl font-bold text-blue-600 tracking-tight font-serif-sc"><span
                      class="text-2xl align-top mt-2 inline-block font-normal text-slate-400 mr-1">¥</span>{{ finalPrice
                      }}</span>
                </div>

                <button @click="submitOrder"
                  class="w-full py-4 bg-slate-900 text-white rounded-xl font-bold text-lg hover:bg-blue-600 hover:shadow-lg hover:shadow-blue-500/30 active:scale-[0.98] transition-all duration-300 flex items-center justify-center gap-2 relative overflow-hidden disabled:opacity-70 disabled:cursor-not-allowed"
                  :disabled="loading">
                  <span v-if="!loading">立即支付</span>
                  <span v-else class="flex items-center gap-2">
                    <svg class="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none"
                      viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z">
                      </path>
                    </svg>处理中...
                  </span>
                </button>
                <p class="text-center text-[10px] text-slate-400 mt-4 flex items-center justify-center gap-1"><svg
                    class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z">
                    </path>
                  </svg> SSL加密 · 支付即代表同意《用户协议》</p>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
.input-field-glass {
  @apply w-full border border-slate-200 bg-white/50 p-3 rounded-xl focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 outline-none transition text-sm backdrop-blur-sm;
}

.tag-btn {
  @apply flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-500 font-bold text-sm hover:border-slate-300 transition bg-white/50;
}

.tag-btn.active {
  @apply bg-slate-900 text-white border-slate-900 shadow-md;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: #e2e8f0;
  border-radius: 20px;
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>