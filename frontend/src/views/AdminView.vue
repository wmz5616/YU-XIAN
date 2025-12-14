<script setup>
import { ref, onMounted, computed } from 'vue';
import { request } from '@/utils/request';
import { useRouter } from 'vue-router';

const router = useRouter();

// 统计数据初始值
const stats = ref({
    totalSales: 0,
    totalOrders: 0,
    totalUsers: 0,
    pendingOrders: 0, // 这个由前端计算
    totalProducts: 0
});

const orders = ref([]);
const loading = ref(true);

// 获取当前用户信息（防止报错，加个默认空对象）
const currentUser = ref(JSON.parse(localStorage.getItem('yuxian_user') || '{}'));

// 1. 获取仪表盘统计数据
const fetchDashboard = async () => {
    try {
        // 修正：后端接口是 /api/admin/stats
        const res = await request.get('/api/admin/stats');
        // 合并数据（保留 pendingOrders）
        stats.value = { ...stats.value, ...res };
    } catch (err) {
        console.error("加载统计数据失败", err);
    }
};

// 2. 获取订单列表
const fetchOrders = async () => {
    loading.value = true;
    try {
        // 修正：后端接口是 /api/admin/orders
        const res = await request.get('/api/admin/orders');
        orders.value = res || [];
        
        // 【关键修复】手动计算“待发货”订单数量
        // 假设状态不是 "SHIPPED"(已发货) 且不是 "DELIVERED"(已送达) 就是待处理
        const pendingCount = orders.value.filter(o => 
            o.status !== 'SHIPPED' && o.status !== 'DELIVERED' && o.status !== '已发货'
        ).length;
        stats.value.pendingOrders = pendingCount;

    } catch (err) {
        console.error("加载订单失败", err);
        orders.value = [];
    } finally {
        loading.value = false;
    }
};

// 3. 处理发货逻辑
const handleShip = async (orderId) => {
    if (!confirm('确定要立即发货该订单吗？')) return;

    try {
        // 修正：使用 PUT 方法调用后端 updateOrderStatus 接口
        await request.put(`/api/admin/orders/${orderId}/status`, {
            status: 'SHIPPED'
        });
        
        alert('发货成功！');
        // 刷新列表和数据
        await fetchOrders(); 
        await fetchDashboard();
    } catch (err) {
        console.error(err);
        alert(err.message || '操作失败');
    }
};

// 状态文本转换（后端存的是英文，前端显示中文）
const formatStatus = (status) => {
    const map = {
        'PAID': '待发货',
        'PENDING': '待付款',
        'SHIPPED': '运输中',
        'DELIVERED': '已送达'
    };
    return map[status] || status; // 如果匹配不到，就直接显示原文本
};

// 状态颜色映射
const getStatusColor = (status) => {
    if (status === 'SHIPPED' || status === '运输中') return 'text-blue-600 bg-blue-100';
    if (status === 'DELIVERED' || status === '已送达') return 'text-green-600 bg-green-100';
    if (status === 'PAID' || status === '待发货') return 'text-orange-600 bg-orange-100';
    return 'text-gray-600 bg-gray-100';
};

onMounted(() => {
    // 简单的权限检查
    // 注意：localStorage 里的 user 结构要看你 LoginView 怎么存的
    const user = JSON.parse(localStorage.getItem('yuxian_user') || '{}');
    if (user.role !== 'ADMIN') {
        alert('您没有管理员权限');
        router.push('/');
        return;
    }
    
    fetchDashboard();
    fetchOrders();
});
</script>

<template>
    <div class="min-h-screen bg-gray-50 flex">

        <aside class="w-64 bg-slate-800 text-white min-h-screen fixed left-0 top-0 z-10">
            <div class="p-6 text-2xl font-bold tracking-wider border-b border-slate-700">
                御鲜·后台
            </div>
            <nav class="mt-6">
                <a class="block py-3 px-6 bg-blue-600 text-white font-medium cursor-pointer">
                    📊 数据总览
                </a>
                <a class="block py-3 px-6 text-slate-400 hover:text-white hover:bg-slate-700 cursor-pointer transition">
                    📦 商品管理 (开发中)
                </a>
                <a class="block py-3 px-6 text-slate-400 hover:text-white hover:bg-slate-700 cursor-pointer transition">
                    👥 用户管理
                </a>
                <div class="mt-10 px-6">
                    <button @click="router.push('/')"
                        class="w-full py-2 border border-slate-600 rounded text-sm hover:bg-slate-700 transition">
                        返回前台商城
                    </button>
                </div>
            </nav>
        </aside>

        <main class="ml-64 flex-1 p-8">

            <div class="flex justify-between items-center mb-8">
                <h1 class="text-2xl font-bold text-gray-800">运营控制台</h1>
                <div class="flex items-center space-x-4">
                    <div class="text-right">
                        <div class="text-sm font-medium text-gray-900">{{ currentUser.username || 'Admin' }}</div>
                        <div class="text-xs text-gray-500">超级管理员</div>
                    </div>
                    <img :src="currentUser.avatar || '/icons/logo.png'"
                        class="w-10 h-10 rounded-full border bg-white object-contain">
                </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <div class="text-gray-500 text-sm mb-1">平台总交易额 (GMV)</div>
                    <div class="text-3xl font-bold text-blue-600">¥ {{ stats.totalSales.toFixed(2) }}</div>
                    <div class="text-xs text-green-500 mt-2">↑ 实时数据</div>
                </div>

                <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <div class="text-gray-500 text-sm mb-1">待发货订单</div>
                    <div class="text-3xl font-bold text-orange-500">{{ stats.pendingOrders }}</div>
                    <div class="text-xs text-gray-400 mt-2">需要尽快处理</div>
                </div>

                <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <div class="text-gray-500 text-sm mb-1">注册会员数</div>
                    <div class="text-3xl font-bold text-gray-800">{{ stats.totalUsers }}</div>
                    <div class="text-xs text-blue-500 mt-2">活跃用户</div>
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
                                    {{ order.productNames || '未知商品' }}
                                </td>
                                <td class="p-4">{{ order.username }}</td>
                                <td class="p-4 font-medium">¥{{ order.totalPrice }}</td>
                                <td class="p-4 text-gray-500">
                                    {{ order.createTime ? new Date(order.createTime).toLocaleString() : '-' }}
                                </td>
                                <td class="p-4">
                                    <span
                                        :class="['px-2 py-1 rounded-full text-xs font-medium', getStatusColor(order.status)]">
                                        {{ formatStatus(order.status) }}
                                    </span>
                                </td>
                                <td class="p-4 text-center">
                                    <button 
                                        v-if="order.status !== 'SHIPPED' && order.status !== '运输中'" 
                                        @click="handleShip(order.id)"
                                        class="bg-blue-600 text-white px-3 py-1 rounded text-xs hover:bg-blue-700 transition shadow-sm">
                                        立即发货
                                    </button>
                                    <span v-else class="text-gray-400 text-xs">已处理</span>
                                </td>
                            </tr>
                        </tbody>
                    </table>

                    <div v-if="orders.length === 0 && !loading" class="p-10 text-center text-gray-400">
                        暂无订单数据
                    </div>
                    <div v-if="loading" class="p-10 text-center text-gray-400">
                        加载中...
                    </div>
                </div>
            </div>

        </main>
    </div>
</template>