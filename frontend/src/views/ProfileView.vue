<script setup>
import { ref, onMounted } from 'vue'
import { store } from '../store.js'
import { useRouter } from 'vue-router'

const router = useRouter()
const orders = ref([])

onMounted(async () => {
  // 如果没登录，踢回登录页
  if (!store.currentUser) {
    router.push('/login')
    return
  }

  // 去后端查订单
  try {
    const res = await fetch(`http://localhost:8080/api/products/orders?username=${store.currentUser.username}`)
    orders.value = await res.json()
  } catch (error) {
    console.error('获取订单失败', error)
  }
})

const getStatusColor = (status) => {
  if (status === '待发货') return 'bg-yellow-100 text-yellow-700'
  if (status === '运输中') return 'bg-blue-100 text-blue-700'
  return 'bg-green-100 text-green-700'
}

const formatDate = (isoString) => {
  if (!isoString) return ''
  return new Date(isoString).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleAvatarUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 1. 限制大小 (比如 2MB)
  if (file.size > 2 * 1024 * 1024) {
    store.showNotification('图片太大了，请小于 2MB', 'error')
    return
  }

  // 2. 将图片转为 Base64
  const reader = new FileReader()
  reader.readAsDataURL(file)
  
  reader.onload = async () => {
    const base64 = reader.result
    
    try {
      // 3. 发给后端保存
      const res = await fetch('http://localhost:8080/api/users/avatar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: store.currentUser.username,
          avatar: base64
        })
      })
      
      if (res.ok) {
        const updatedUser = await res.json()
        // 4. 更新前端 Store 里的用户信息
        store.login(updatedUser) 
        store.showNotification('头像更新成功！')
      }
    } catch (e) {
      console.error(e)
      store.showNotification('上传失败', 'error')
    }
  }
}
</script>

<template>
  <div class="container mx-auto px-4 py-8 max-w-4xl">
    <div class="bg-white rounded-2xl shadow-sm border border-slate-100 p-8 mb-8 flex items-center gap-6">
      <div class="relative group cursor-pointer">
        <input 
          type="file" 
          accept="image/*" 
          class="absolute inset-0 w-full h-full opacity-0 z-20 cursor-pointer"
          @change="handleAvatarUpload"
        />
        
        <div class="w-20 h-20 rounded-full overflow-hidden border-4 border-white shadow-lg bg-blue-900 flex items-center justify-center text-3xl text-white font-serif-sc relative">
          <img 
            v-if="store.currentUser?.avatar" 
            :src="store.currentUser.avatar" 
            class="w-full h-full object-cover"
          />
          <span v-else>{{ store.currentUser?.displayName?.charAt(0) }}</span>
          
          <div class="absolute inset-0 bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition duration-300 z-10">
            <span class="text-xs text-white font-bold">更换</span>
          </div>
        </div>
      </div>
      <div>
        <h1 class="text-2xl font-bold text-slate-800">欢迎，{{ store.currentUser?.displayName }}</h1>
        <p class="text-slate-500 text-sm mt-1">尊贵的初级会员</p>
      </div>
    </div>

    <h2 class="text-xl font-bold text-slate-800 mb-6">我的订单记录</h2>

    <div v-if="orders.length > 0" class="space-y-4">
      <div v-for="order in orders" :key="order.id" class="bg-white p-6 rounded-xl border border-slate-100 shadow-sm hover:shadow-md transition">
        <div class="flex justify-between items-start mb-4">
          <div>
            <span class="text-xs text-slate-400 block mb-1">{{ formatDate(order.createTime) }}</span>
            <span :class="`px-2 py-1 text-xs font-bold rounded ${getStatusColor(order.status)}`">
              {{ order.status }}
            </span>
          </div>
          <span class="text-xl font-bold text-blue-900">¥{{ order.totalPrice }}</span>
        </div>
        <div class="pt-4 border-t border-slate-50">
          <p class="text-slate-700 font-medium">{{ order.productNames }}</p>
        </div>
      </div>
    </div>

    <div v-else class="bg-white rounded-2xl border border-slate-100 p-12 text-center flex flex-col items-center">
      <div class="w-32 h-32 bg-slate-50 rounded-full flex items-center justify-center mb-6 overflow-hidden relative group">
        <div class="absolute top-0 right-0 w-full h-full bg-[radial-gradient(circle_at_50%_120%,rgba(30,58,138,0.1),transparent)]"></div>
        <img src="/icons/empty-order.png" class="w-16 h-16 object-contain opacity-60 group-hover:scale-110 transition duration-500" alt="无订单" />
      </div>
      
      <h3 class="text-xl font-bold text-slate-800 mb-2">您还没有品尝过我们的海鲜</h3>
      <p class="text-slate-400 text-sm mb-8">首单享极速发货，鲜活直达。</p>
      
      <button 
        @click="router.push('/')" 
        class="px-8 py-2.5 border-2 border-slate-900 text-slate-900 font-bold rounded-lg hover:bg-slate-900 hover:text-white transition-colors duration-300"
      >
        去选购今日鲜货
      </button>
    </div>
  </div>
</template>