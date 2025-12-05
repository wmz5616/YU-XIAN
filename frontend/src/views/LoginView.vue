<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '../store.js'

const router = useRouter()
const form = ref({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    store.showNotification('请输入账号和密码', 'error')
    return
  }

  try {
    const res = await fetch('http://localhost:8080/api/users/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form.value)
    })
    
    if (res.ok) {
      const user = await res.json()
      if (user) {
        store.login(user) 
        router.push('/') 
      } else {
        store.showNotification('账号或密码错误', 'error')
      }
    } else {
      store.showNotification('登录服务异常', 'error')
    }
  } catch (error) {
    store.showNotification('账号或密码错误', 'error')
  }
}
</script>

<template>
  <div class="min-h-[80vh] flex items-center justify-center bg-[#F8FAFC] py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8 bg-white p-10 rounded-2xl shadow-xl border border-slate-100">
      <div class="text-center">
        <h2 class="text-3xl font-serif-sc font-bold text-slate-900">欢迎回家</h2>
        <p class="mt-2 text-sm text-slate-600">
          请登录您的渔鲜直供账号
        </p>
      </div>
      
      <form class="mt-8 space-y-6" @submit.prevent="handleLogin">
        <div class="rounded-md shadow-sm -space-y-px">
          <div class="mb-4">
            <label class="block text-sm font-medium text-slate-700 mb-1">账号</label>
            <input 
              v-model="form.username" 
              type="text" 
              required 
              class="appearance-none rounded-lg relative block w-full px-3 py-3 border border-slate-300 placeholder-slate-500 text-slate-900 focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm transition" 
              placeholder="请输入用户名"
            >
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">密码</label>
            <input 
              v-model="form.password" 
              type="password" 
              required 
              class="appearance-none rounded-lg relative block w-full px-3 py-3 border border-slate-300 placeholder-slate-500 text-slate-900 focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm transition" 
              placeholder="请输入密码"
            >
          </div>
        </div>

        <div>
          <button 
            type="submit" 
            class="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-lg text-white bg-blue-900 hover:bg-blue-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-all shadow-lg hover:shadow-blue-900/30"
          >
            登录
          </button>
        </div>
        
        <div class="text-center text-sm">
          <span class="text-slate-500">还没有账号？</span>
          <router-link to="/register" class="font-medium text-blue-600 hover:text-blue-500">
            立即免费注册
          </router-link>
        </div>
      </form>
    </div>
  </div>
</template>