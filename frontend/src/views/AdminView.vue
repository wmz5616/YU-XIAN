<script setup>
import { ref, onMounted, computed } from 'vue';
import { request } from '@/utils/request';
import { useRouter } from 'vue-router';

const router = useRouter();
const stats = ref({
    totalSales: 0,
    totalOrders: 0,
    totalUsers: 0,
    pendingOrders: 0
});
const orders = ref([]);
const loading = ref(true);

const currentUser = JSON.parse(localStorage.getItem('user') || '{}');

const fetchDashboard = async () => {
    try {
        const res = await request.get(`/admin/dashboard?username=${currentUser.username}`);
        stats.value = res.data;
    } catch (err) {
        console.error("加载数据失败", err);
    }
};

const fetchOrders = async () => {
    try {
        const res = await request.get(`/admin/orders?username=${currentUser.username}`);
        orders.value = res.data;
    } catch (err) {
        console.error("加载订单失败", err);
    } finally {
        loading.value = false;
    }
};

const handleShip = async (orderId) => {
    if (!confirm('确定要立即发货该订单吗？物流信息将同步上链。')) return;

    try {
        await request.post(`/admin/orders/${orderId}/ship`, {
            username: currentUser.username
        });
        alert('发货成功！');
        fetchOrders();
        fetchDashboard();
    } catch (err) {
        alert(err.response?.data || '操作失败');
    }
};

onMounted(() => {
    const role = localStorage.getItem('role');
    if (role !== 'ADMIN') {
        alert('您没有管理员权限');
        router.push('/');
        return;
    }
    fetchDashboard();
    fetchOrders();
});

const getStatusColor = (status) => {
    switch (status) {
        case '待发货': return 'text-orange-600 bg-orange-100';
        case '运输中': return 'text-blue-600 bg-blue-100';
        case '已送达': return 'text-green-600 bg-green-100';
        default: return 'text-gray-600 bg-gray-100';
    }
};
</script>

<template>
    <div class="min-h-screen bg-gray-50 flex">

        <aside class="w-64 bg-slate-800 text-white min-h-screen fixed left-0 top-0">
            <div class="p-6 text-2xl font-bold tracking-wider border-b border-slate-700">
                御鲜·后台
            </div>
            <nav class="mt-6">
                <a class="block py-3 px-6 bg-blue-600 text-white font-medium cursor-pointer">
                    📊 数据总览
                </a>
                <a class="block py-3 px-6 text-slate-400 hover:text-white hover:bg-slate-700 cursor-pointer transition">
                    📦 商品管理 (演示版)
                </a>
                <a class="block py-3 px-6 text-slate-400 hover:text-white hover:bg-slate-700 cursor-pointer transition">
                    👥 用户管理
                </a>
                <div class="mt-10 px-6">
                    <button @click="router.push('/')"
                        class="w-full py-2 border border-slate-600 rounded text-sm hover:bg-slate-700">
                        返回前台商城
                    </button>
                </div>
            </nav>
        </aside>

        <main class="ml-64 flex-1 p-8">

            <div class="flex justify-between items-center mb-8">
                <h1 class="text-2xl font-bold text-gray-800">运营控制台</h1>
                <div class="flex items-center space-x-4">
                    <span class="text-sm text-gray-500">管理员: {{ currentUser.displayName }}</span>
                    <img :src="currentUser.avatar || '/images/default-avatar.png'"
                        class="w-10 h-10 rounded-full border">
                </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <div class="text-gray-500 text-sm mb-1">平台总交易额 (GMV)</div>
                    <div class="text-3xl font-bold text-blue-600">¥ {{ stats.totalSales }}</div>
                    <div class="text-xs text-green-500 mt-2">↑ 较昨日 +12.5%</div>
                </div>

                <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <div class="text-gray-500 text-sm mb-1">待发货订单</div>
                    <div class="text-3xl font-bold text-orange-500">{{ stats.pendingOrders }}</div>
                    <div class="text-xs text-gray-400 mt-2">需要尽快处理</div>
                </div>

                <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <div class="text-gray-500 text-sm mb-1">注册会员数</div>
                    <div class="text-3xl font-bold text-gray-800">{{ stats.totalUsers }}</div>
                    <div class="text-xs text-blue-500 mt-2">本周新增 +3</div>
                </div>

                <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <div class="text-gray-500 text-sm mb-1">上架SKU总数</div>
                    <div class="text-3xl font-bold text-gray-800">{{ stats.totalProducts }}</div>
                    <div class="text-xs text-gray-400 mt-2">库存状态良好</div>
                </div>
            </div>

            <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                <div class="p-6 border-b border-gray-100 flex justify-between items-center">
                    <h2 class="text-lg font-bold text-gray-800">实时订单监控</h2>
                    <button @click="fetchOrders" class="text-blue-600 text-sm hover:underline">刷新列表</button>
                </div>

                <div class="overflow-x-auto">
                    <table class="w-full text-left text-sm">
                        <thead class="bg-gray-50 text-gray-500">
                            <tr>
                                <th class="p-4">订单号</th>
                                <th class="p-4">商品内容</th>
                                <th class="p-4">买家</th>
                                <th class="p-4">金额</th>
                                <th class="p-4">下单时间</th>
                                <th class="p-4">状态</th>
                                <th class="p-4 text-center">操作</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-gray-100">
                            <tr v-for="order in orders" :key="order.id" class="hover:bg-gray-50 transition">
                                <td class="p-4 font-mono text-gray-500">#{{ order.id }}</td>
                                <td class="p-4 max-w-xs truncate" :title="order.productNames">
                                    {{ order.productNames }}
                                </td>
                                <td class="p-4">{{ order.username }}</td>
                                <td class="p-4 font-medium">¥{{ order.totalPrice }}</td>
                                <td class="p-4 text-gray-500">{{ new Date(order.createTime).toLocaleString() }}</td>
                                <td class="p-4">
                                    <span
                                        :class="['px-2 py-1 rounded-full text-xs font-medium', getStatusColor(order.status)]">
                                        {{ order.status }}
                                    </span>
                                </td>
                                <td class="p-4 text-center">
                                    <button v-if="order.status === '待发货'" @click="handleShip(order.id)"
                                        class="bg-blue-600 text-white px-3 py-1 rounded text-xs hover:bg-blue-700 transition shadow-sm">
                                        立即发货
                                    </button>
                                    <span v-else class="text-gray-400 text-xs">--</span>
                                </td>
                            </tr>
                        </tbody>
                    </table>

                    <div v-if="orders.length === 0 && !loading" class="p-10 text-center text-gray-400">
                        暂无订单数据
                    </div>
                </div>
            </div>

        </main>
    </div>
</template>