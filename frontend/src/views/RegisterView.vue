<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '../store.js'
import { request } from '@/utils/request'

const API_BASE = import.meta.env.VITE_API_BASE_URL
const router = useRouter()
const form = ref({
  username: '',
  password: '',
  displayName: ''
})

const handleRegister = async () => {
  const regex = /^[a-z0-9]{1,7}$/
  if (!regex.test(form.value.username)) {
    store.showNotification('会员名格式错误：仅限小写字母+数字，最多7位', 'error')
    return
  }

  try {
    const data = await request('/api/users/register', {
      method: 'POST',
      body: JSON.stringify(form.value)
    })

    if (data.success) {
      store.showNotification(data.message, 'success')
      router.push('/login')
    } else {
      store.showNotification(data.message || '注册失败', 'error')
    }
  } catch (error) {
    store.showNotification('注册服务异常', 'error')
  }
}
</script>

<template>
  <div class="min-h-[80vh] flex items-center justify-center bg-[#F8FAFC] py-12 px-4">
    <div class="max-w-md w-full bg-white p-10 rounded-3xl shadow-xl border border-slate-100">
      <div class="text-center mb-10">
        <h2 class="text-3xl font-serif-sc font-bold text-slate-900">加入渔鲜会员</h2>
        <p class="mt-2 text-sm text-slate-500">开启您的鲜活之旅</p>
      </div>

      <form class="space-y-6" @submit.prevent="handleRegister">

        <div>
          <label class="block text-sm font-bold text-slate-700 mb-2">
            会员名 <span class="text-xs font-normal text-slate-400">(唯一ID)</span>
          </label>
          <input v-model="form.username" type="text" required
            class="w-full px-4 py-3 rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 outline-none transition font-mono text-sm"
            placeholder="例: abc123 (小写字母+数字, ≤7位)">
        </div>

        <div>
          <label class="block text-sm font-bold text-slate-700 mb-2">
            用户名 <span class="text-xs font-normal text-slate-400">(昵称，可重复)</span>
          </label>
          <input v-model="form.displayName" type="text"
            class="w-full px-4 py-3 rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 outline-none transition"
            placeholder="例: 测试1">
        </div>

        <div>
          <label class="block text-sm font-bold text-slate-700 mb-2">密码</label>
          <input v-model="form.password" type="password" required
            class="w-full px-4 py-3 rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 outline-none transition"
            placeholder="设置您的登录密码">
        </div>

        <button type="submit"
          class="w-full py-4 bg-slate-900 text-white font-bold rounded-xl hover:bg-blue-900 transition shadow-lg hover:shadow-xl transform hover:-translate-y-0.5">
          立即注册
        </button>

        <div class="text-center text-sm">
          <router-link to="/login" class="text-slate-500 hover:text-blue-600 transition">
            已有账号？<span class="underline">去登录</span>
          </router-link>
        </div>
      </form>
    </div>
  </div>
</template>