<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '@/store'
import { request } from '@/utils/request'

const router = useRouter()

const form = ref({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    store.showNotification('请输入用户名和密码', 'error')
    return
  }

  try {
    const data = await request('/api/users/login', {
      method: 'POST',
      body: JSON.stringify(form.value)
    })

    store.login(data.user)
    localStorage.setItem('yuxian_token', data.token)

    store.showNotification('登录成功！', 'success')
    router.push('/')

  } catch (error) {
    console.error(error)
    if (error.message && error.message.includes('401')) {
       store.showNotification('用户名或密码错误', 'error')
    } else {
       store.showNotification('登录服务异常，请检查后端', 'error')
    }
  }
}
</script>

<template>
  <div class="min-h-[80vh] flex items-center justify-center bg-[#F8FAFC] py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8 bg-white p-10 rounded-2xl shadow-xl border border-slate-100">
      <div class="text-center">
        <h2 class="mt-6 text-3xl font-bold text-slate-900 font-serif-sc">
          欢迎回来
        </h2>
        <p class="mt-2 text-sm text-slate-600">
          登录您的渔鲜账号
        </p>
      </div>

      <form class="mt-8 space-y-6" @submit.prevent="handleLogin">
        <div class="rounded-md shadow-sm space-y-4">
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">用户名</label>
            <input v-model="form.username" type="text" required
              class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-slate-300 placeholder-slate-400 text-slate-900 focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm transition bg-slate-50"
              placeholder="请输入用户名">
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">密码</label>
            <input v-model="form.password" type="password" required
              class="appearance-none rounded-xl relative block w-full px-4 py-3 border border-slate-300 placeholder-slate-400 text-slate-900 focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm transition bg-slate-50"
              placeholder="请输入密码">
          </div>
        </div>

        <div>
          <button type="submit"
            class="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-bold rounded-xl text-white bg-slate-900 hover:bg-blue-900 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-all shadow-lg hover:shadow-blue-900/30 transform active:scale-95">
            登录
          </button>
        </div>

        <div class="text-center">
          <p class="text-sm text-slate-600">
            还没有账号？
            <router-link to="/register" class="font-medium text-blue-600 hover:text-blue-500">
              立即注册
            </router-link>
          </p>
        </div>
      </form>
    </div>
  </div>
</template>