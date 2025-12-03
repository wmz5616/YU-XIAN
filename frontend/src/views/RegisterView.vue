<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '../store.js'

const router = useRouter()
const form = ref({
  username: '',
  password: '',
  displayName: '' // 昵称
})

const handleRegister = async () => {
  if (!form.value.username || !form.value.password) {
    store.showNotification('请填写完整信息', 'error')
    return
  }

  try {
    const res = await fetch('http://localhost:8080/api/users/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value)
    })
    
    const msg = await res.text() // 后端返回的是字符串 "注册成功" 或 "失败..."
    
    if (msg === '注册成功') {
      store.showNotification('注册成功！请登录', 'success')
      router.push('/login') // 注册好后去登录页
    } else {
      store.showNotification(msg, 'error')
    }
  } catch (error) {
    store.showNotification('注册服务异常', 'error')
  }
}
</script>

<template>
  <div class="min-h-[80vh] flex items-center justify-center bg-[#F8FAFC] py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8 bg-white p-10 rounded-2xl shadow-xl border border-slate-100">
      <div class="text-center">
        <h2 class="text-3xl font-serif-sc font-bold text-slate-900">加入渔鲜会员</h2>
        <p class="mt-2 text-sm text-slate-600">
          尊享产地直供特权
        </p>
      </div>
      
      <form class="mt-8 space-y-5" @submit.prevent="handleRegister">
        <div class="rounded-md shadow-sm space-y-4">
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">用户名 (登录账号)</label>
            <input v-model="form.username" type="text" required class="appearance-none rounded-lg block w-full px-3 py-3 border border-slate-300 placeholder-slate-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm" placeholder="例如: yuxian001">
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">密码</label>
            <input v-model="form.password" type="password" required class="appearance-none rounded-lg block w-full px-3 py-3 border border-slate-300 placeholder-slate-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm" placeholder="设置您的密码">
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">您的称呼 (昵称)</label>
            <input v-model="form.displayName" type="text" class="appearance-none rounded-lg block w-full px-3 py-3 border border-slate-300 placeholder-slate-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm" placeholder="例如: 张老板">
          </div>
        </div>

        <button type="submit" class="w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-lg text-white bg-blue-900 hover:bg-blue-800 transition-all shadow-lg hover:shadow-blue-900/30">
          立即注册
        </button>
        
        <div class="text-center text-sm">
          <router-link to="/login" class="font-medium text-blue-600 hover:text-blue-500">
            已有账号？去登录
          </router-link>
        </div>
      </form>
    </div>
  </div>
</template>