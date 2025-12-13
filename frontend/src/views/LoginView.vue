<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { store } from '../store';
import request from '../utils/request';

const router = useRouter();
const loading = ref(false);
const errorMessage = ref('');

const form = ref({
  username: '',
  password: ''
});

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    errorMessage.value = "请输入账号和密码";
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const response = await request.post('/users/login', form.value);

    console.log("登录响应数据:", response);

    localStorage.setItem('yuxian_token', response.token);

    const userInfo = {
      username: response.username,
      role: response.role,
      token: response.token
    };

    store.login(userInfo);

    if (response.role === 'ADMIN') {
      router.push('/admin');
    } else {
      router.push('/');
    }
  } catch (error) {
    console.error("登录出错:", error);
    errorMessage.value = error.message || "登录失败，请检查账号密码";
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8 bg-white p-8 rounded-xl shadow-lg">
      <div>
        <img class="mx-auto h-16 w-auto" src="/icons/logo.png" alt="Logo" />
        <h2 class="mt-6 text-center text-3xl font-extrabold text-gray-900">
          账号登录
        </h2>
      </div>

      <form class="mt-8 space-y-6" @submit.prevent="handleLogin">
        <div class="rounded-md shadow-sm -space-y-px">
          <div>
            <label for="username" class="sr-only">用户名</label>
            <input v-model="form.username" id="username" name="username" type="text" required
              class="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm"
              placeholder="请输入用户名" />
          </div>
          <div>
            <label for="password" class="sr-only">密码</label>
            <input v-model="form.password" id="password" name="password" type="password" required
              class="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm"
              placeholder="请输入密码" />
          </div>
        </div>

        <div v-if="errorMessage" class="text-red-500 text-sm text-center">
          {{ errorMessage }}
        </div>

        <div>
          <button type="submit" :disabled="loading"
            class="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50">
            <span v-if="loading">登录中...</span>
            <span v-else>立即登录</span>
          </button>
        </div>

        <div class="text-center mt-4">
          <router-link to="/register" class="text-sm text-blue-600 hover:text-blue-500">
            没有账号？去注册
          </router-link>
        </div>
      </form>
    </div>
  </div>
</template>