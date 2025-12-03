<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '../store.js'

const router = useRouter()

// 1. 模拟几个收货地址 (为了前端展示，暂时写死)
const addresses = ref([
  { id: 1, name: '张老板', phone: '138****8888', location: '上海市 浦东新区 陆家嘴环路 1000 号', tag: '公司' },
  { id: 2, name: '张总', phone: '139****9999', location: '浙江省 舟山市 普陀区 东港街道', tag: '家' }
])
const selectedAddress = ref(addresses.value[0].id)

// 2. 支付方式
const paymentMethod = ref('alipay')

// 3. 计算最终金额 (含运费逻辑)
const freight = computed(() => store.totalPrice > 200 ? 0 : 20) // 满200免运费
const finalPrice = computed(() => (parseFloat(store.totalPrice) + freight.value).toFixed(2))

// 4. 提交订单
const submitOrder = async () => {
  if (store.items.length === 0) return
  
  // 这里的逻辑和之前一样，发给后端
  try {
    const payload = {
      username: store.currentUser?.username || 'guest',
      items: store.items.map(item => ({
        id: item.id,
        quantity: item.quantity // 把数量带上
      }))
    }

    const res = await fetch('http://localhost:8080/api/products/order', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    if (res.ok) {
      const amount = finalPrice.value
      store.clearCart()
      router.push(`/payment-success?amount=${amount}`)
    } else {
      // 【新增】如果后端报库存不足，弹窗提示
      const errorMsg = await res.text()
      alert('下单失败：' + errorMsg) 
    }
  } catch (e) {
    alert('网络错误')
  }
}
</script>

<template>
  <div class="container mx-auto px-4 py-8 max-w-5xl">
    <button @click="router.back()" class="mb-6 flex items-center gap-2 text-slate-500 hover:text-blue-900 transition">
      ← 返回购物车
    </button>
    
    <h1 class="text-3xl font-serif-sc font-bold text-slate-900 mb-8">填写订单信息</h1>

    <div class="flex flex-col lg:flex-row gap-12">
      <div class="flex-1 space-y-8">
        
        <section>
          <h2 class="text-lg font-bold text-slate-800 mb-4">收货地址</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div 
              v-for="addr in addresses" 
              :key="addr.id"
              @click="selectedAddress = addr.id"
              :class="[
                'p-4 rounded-xl border-2 cursor-pointer transition-all relative',
                selectedAddress === addr.id ? 'border-blue-600 bg-blue-50/30' : 'border-slate-100 hover:border-slate-300'
              ]"
            >
              <div class="flex justify-between mb-2">
                <span class="font-bold text-slate-800">{{ addr.name }}</span>
                <span class="text-slate-500 text-sm">{{ addr.phone }}</span>
              </div>
              <p class="text-sm text-slate-600 leading-relaxed">{{ addr.location }}</p>
              <div v-if="selectedAddress === addr.id" class="absolute top-0 right-0 bg-blue-600 text-white text-xs px-2 py-1 rounded-bl-lg rounded-tr-lg">默认</div>
            </div>
            
            <div class="p-4 rounded-xl border-2 border-dashed border-slate-200 flex items-center justify-center text-slate-400 cursor-pointer hover:border-blue-400 hover:text-blue-500 transition">
              + 新增地址
            </div>
          </div>
        </section>

        <section>
          <h2 class="text-lg font-bold text-slate-800 mb-4">支付方式</h2>
          <div class="flex gap-4">
            <div 
              @click="paymentMethod = 'alipay'"
              :class="['flex-1 p-4 rounded-xl border cursor-pointer flex items-center gap-3 transition', paymentMethod === 'alipay' ? 'border-blue-600 bg-blue-50' : 'border-slate-100']"
            >
              <div class="w-6 h-6 bg-blue-500 rounded text-white flex items-center justify-center font-bold text-xs">支</div>
              <span class="font-medium text-slate-700">支付宝</span>
            </div>
            <div 
              @click="paymentMethod = 'wechat'"
              :class="['flex-1 p-4 rounded-xl border cursor-pointer flex items-center gap-3 transition', paymentMethod === 'wechat' ? 'border-green-600 bg-green-50' : 'border-slate-100']"
            >
              <div class="w-6 h-6 bg-green-500 rounded text-white flex items-center justify-center font-bold text-xs">微</div>
              <span class="font-medium text-slate-700">微信支付</span>
            </div>
          </div>
        </section>

        <section>
          <h2 class="text-lg font-bold text-slate-800 mb-4">商品清单</h2>
          <div class="bg-slate-50 rounded-xl p-4 space-y-3">
            <div v-for="(item, idx) in store.items" :key="idx" class="flex justify-between text-sm items-center">
              <div class="flex items-center gap-2">
                <span class="bg-blue-100 text-blue-800 text-xs font-bold px-2 py-0.5 rounded">x{{ item.quantity }}</span>
                <span class="text-slate-600">{{ item.name }}</span>
              </div>
              <span class="font-medium text-slate-900">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
            </div>
          </div>
        </section>
      </div>

      <div class="w-full lg:w-80">
        <div class="bg-white rounded-2xl shadow-xl border border-slate-100 p-6 sticky top-24">
          <h3 class="font-bold text-slate-800 mb-6 text-lg">费用明细</h3>
          
          <div class="space-y-3 mb-6 border-b border-slate-100 pb-6">
            <div class="flex justify-between text-slate-600">
              <span>商品总额</span>
              <span>¥{{ store.totalPrice }}</span>
            </div>
            <div class="flex justify-between text-slate-600">
              <span>运费</span>
              <span :class="freight === 0 ? 'text-green-600' : ''">{{ freight === 0 ? '- ¥0.00' : `+ ¥${freight}` }}</span>
            </div>
            <div class="flex justify-between text-slate-600">
              <span>活动优惠</span>
              <span class="text-orange-500">- ¥0.00</span>
            </div>
          </div>

          <div class="flex justify-between items-end mb-6">
            <span class="font-bold text-slate-800 text-lg">应付总额</span>
            <span class="text-3xl font-bold text-blue-900 font-serif-sc">¥{{ finalPrice }}</span>
          </div>

          <button 
            @click="submitOrder"
            class="w-full bg-blue-900 hover:bg-blue-800 text-white font-bold py-4 rounded-xl shadow-lg shadow-blue-900/20 transition transform active:scale-95"
          >
            确认付款
          </button>
          
          <p class="text-xs text-center text-slate-400 mt-4">
            下单即代表您同意《用户购买协议》
          </p>
        </div>
      </div>
    </div>
  </div>
</template>