<script setup>
import { ref, onMounted, nextTick, watch, onUnmounted, computed } from 'vue';
import { request } from '@/utils/request';
import { useRouter, useRoute } from 'vue-router';
import * as echarts from 'echarts';
import Swal from 'sweetalert2';

const router = useRouter();
const route = useRoute();

const currentTab = ref('dashboard');
const currentUser = ref(JSON.parse(localStorage.getItem('yuxian_user') || '{}'));
const loading = ref(false);
const isSidebarOpen = ref(false);
const isDark = ref(localStorage.getItem('theme') === 'dark');

const refundList = ref([]);

const stats = ref({
    totalSales: 0, totalOrders: 0, totalUsers: 0, pendingOrders: 0, totalProducts: 0,
    chartData: { dates: [], values: [] }
});

const displayedOrders = ref([]);
const chartRef = ref(null);
let myChart = null;

const orderQuery = ref({ page: 1, size: 10, keyword: '', status: 'ALL', total: 0 });

const statusTabs = [
    { key: 'ALL', label: '全部' },
    { key: 'PAID', label: '待发货' },
    { key: 'SHIPPED', label: '运输中' },
    { key: 'DELIVERED', label: '已送达' }
];

const showDetailModal = ref(false);
const currentOrderDetails = ref({});

const products = ref([]);
const showProductModal = ref(false);
const editingProduct = ref({});

const users = ref([]);
const showPointModal = ref(false);
const editingUser = ref({});

const Toast = Swal.mixin({
    toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true,
    didOpen: (toast) => { toast.addEventListener('mouseenter', Swal.stopTimer); toast.addEventListener('mouseleave', Swal.resumeTimer); }
});

const currentPageTitle = computed(() => {
    const map = {
        'dashboard': '运营概况',
        'orders': '订单管理',
        'products': '商品库管理',
        'users': '会员管理',
        'refund': '售后处理中心'
    }
    return map[currentTab.value] || '控制台'
})

watch(() => route.path, () => { isSidebarOpen.value = false; });

let socket = null;
const initWebSocket = () => {
    if (typeof (WebSocket) === "undefined") return;
    const token = localStorage.getItem('token') || '';
    const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
    const wsUrl = `${protocol}localhost:8080/ws/orders?token=${token}`;

    socket = new WebSocket(wsUrl);

    socket.onopen = () => {
        console.log("WebSocket已连接 (身份鉴权中...)");
    };

    socket.onmessage = (msg) => {
        if (msg.data === 'NEW_ORDER') {
            Toast.fire({ icon: 'info', title: '🔔 收到新订单！', text: '列表已自动刷新' });
            if (currentTab.value === 'dashboard' || currentTab.value === 'orders') {
                fetchStats();
                fetchOrders(false);
            }
        }
    };

    socket.onerror = () => {
        console.log("WebSocket连接失败 (可能是Token失效或网络问题)");
    };
};

const toggleDark = () => {
    isDark.value = !isDark.value;
    updateTheme();
};

const updateTheme = () => {
    const html = document.documentElement;
    if (isDark.value) {
        html.classList.add('dark');
        localStorage.setItem('theme', 'dark');
    } else {
        html.classList.remove('dark');
        localStorage.setItem('theme', 'light');
    }
    if (myChart) {
        myChart.dispose();
        initChart();
    }
};

const initChart = () => {
    if (!chartRef.value) return;
    if (myChart) myChart.dispose();

    myChart = echarts.init(chartRef.value, isDark.value ? 'dark' : undefined, { renderer: 'svg' });

    const textColor = isDark.value ? '#94a3b8' : '#334155';
    const splitLineColor = isDark.value ? '#334155' : '#e2e8f0';

    const option = {
        backgroundColor: 'transparent',
        title: { text: '近7日销售趋势', left: 'left', textStyle: { fontSize: 16, color: textColor, fontWeight: 'bold' } },
        tooltip: { trigger: 'axis', backgroundColor: isDark.value ? '#1e293b' : '#fff', borderColor: splitLineColor, textStyle: { color: textColor } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', boundaryGap: false, data: stats.value.chartData.dates, axisLine: { lineStyle: { color: splitLineColor } }, axisLabel: { color: textColor } },
        yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: splitLineColor } }, axisLabel: { color: textColor } },
        series: [{
            name: '销售额', type: 'line', smooth: true, showSymbol: false,
            data: stats.value.chartData.values,
            itemStyle: { color: '#3b82f6' },
            lineStyle: { width: 3 },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(59,130,246,0.5)' }, { offset: 1, color: 'rgba(59,130,246,0)' }]) }
        }]
    };
    myChart.setOption(option);
    window.addEventListener('resize', () => myChart && myChart.resize());
};

const fetchStats = async () => {
    try {
        const res = await request.get('/api/admin/stats');
        stats.value = { ...stats.value, ...res };
        nextTick(() => initChart());
    } catch (err) { console.error(err); }
};

const fetchOrders = async (showLoading = true) => {
    if (showLoading) {
        loading.value = true;
        displayedOrders.value = [];
    }
    try {
        const params = new URLSearchParams({
            page: orderQuery.value.page,
            size: orderQuery.value.size,
            keyword: orderQuery.value.keyword,
            status: orderQuery.value.status
        });

        if (showLoading) await new Promise(r => setTimeout(r, 400));

        const res = await request.get(`/api/admin/orders?${params.toString()}`);

        let rawList = Array.isArray(res) ? res : (res.content || []);

        displayedOrders.value = rawList;
        
        orderQuery.value.total = res.totalElements || 0;

    } catch (err) {
        console.error(err);
        Toast.fire('获取订单失败', '', 'error');
    } finally {
        loading.value = false;
    }
};

const switchStatusTab = (k) => { 
    orderQuery.value.status = k; 
    orderQuery.value.page = 1;
    fetchOrders();
};

const handleSearch = () => { orderQuery.value.page = 1; fetchOrders(); };

const changePage = (p) => { if (p < 1) return; orderQuery.value.page = p; fetchOrders(); };

const handleShip = async (id) => {
    const res = await Swal.fire({ title: '确认发货?', text: '将更新订单状态为运输中', icon: 'info', showCancelButton: true, confirmButtonColor: '#3b82f6', cancelButtonColor: '#64748b', confirmButtonText: '发货' });
    if (res.isConfirmed) {
        await request.put(`/api/admin/orders/${id}/status`, { status: 'SHIPPED' });
        fetchOrders(false);
        Toast.fire('已发货', '', 'success');
    }
};

const openDetailModal = (o) => { currentOrderDetails.value = o; showDetailModal.value = true; };
const formatStatus = (s) => ({ 'PAID': '待发货', 'SHIPPED': '运输中', 'DELIVERED': '已送达', '已送达': '已送达', '售后处理中': '售后中', '退款成功': '已退款' }[s] || s);
const getStatusClass = (s) => {
    const base = "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ";
    if (['PAID', '待发货'].includes(s)) return base + "bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300";
    if (['SHIPPED', '运输中'].includes(s)) return base + "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300";
    if (['DELIVERED', '已送达'].includes(s)) return base + "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300";
    if (['售后处理中', '退款成功'].includes(s)) return base + "bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300";
    return base + "bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300";
};

const fetchProducts = async () => {
    loading.value = true; products.value = [];
    try { await new Promise(r => setTimeout(r, 300)); const res = await request.get('/api/products'); products.value = res || []; }
    finally { loading.value = false; }
};
const openProductModal = (p) => { editingProduct.value = p ? { ...p } : { name: '', price: 0, stock: 100 }; showProductModal.value = true; };
const saveProduct = async () => {
    try {
        if (editingProduct.value.id) await request.put(`/api/products/${editingProduct.value.id}`, editingProduct.value);
        else await request.post('/api/products', editingProduct.value);
        showProductModal.value = false; fetchProducts(); Toast.fire('保存成功', '', 'success');
    } catch (e) { Swal.fire('Error', e.message, 'error'); }
};
const handleDeleteProduct = async (id) => { if ((await Swal.fire({ title: '删除?', icon: 'warning', showCancelButton: true })).isConfirmed) { await request.delete(`/api/products/${id}`); fetchProducts(); Toast.fire('已删除', '', 'success'); } };

const fetchUsers = async () => {
    loading.value = true; users.value = [];
    try { await new Promise(r => setTimeout(r, 300)); users.value = await request.get('/api/admin/users') || []; }
    finally { loading.value = false; }
};
const openPointModal = (u) => { editingUser.value = { ...u }; showPointModal.value = true; };
const saveUserPoints = async () => { await request.put(`/api/admin/users/${editingUser.value.id}/points`, { points: parseInt(editingUser.value.points) }); showPointModal.value = false; fetchUsers(); Toast.fire('修改成功', '', 'success'); };
const handleDeleteUser = async (id) => { if ((await Swal.fire({ title: '删除用户?', icon: 'error', showCancelButton: true })).isConfirmed) { await request.delete(`/api/admin/users/${id}`); fetchUsers(); } };

const fetchRefunds = async () => {
    loading.value = true;
    try {
        const res = await request.get('/api/orders/admin/refunds');
        refundList.value = res || [];
    } catch (e) {
        console.error("加载售后失败", e);
        Toast.fire('加载失败', '无法获取售后列表', 'error');
    } finally {
        loading.value = false;
    }
};

const handleRefundAction = async (orderId, action) => {
    const isApprove = action === 'approve';

    let rejectReason = '';
    if (!isApprove) {
        const { value: text } = await Swal.fire({
            title: '请输入拒绝理由',
            input: 'textarea',
            inputLabel: '理由',
            inputPlaceholder: '请输入...',
            showCancelButton: true
        });
        if (!text) return;
        rejectReason = text;
    } else {
        const confirm = await Swal.fire({
            title: '确认同意退款?',
            text: '订单金额将原路退回',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#10b981',
            confirmButtonText: '确认同意'
        });
        if (!confirm.isConfirmed) return;
    }

    try {
        await request.post(`/api/orders/admin/refunds/${orderId}/audit`, {
            pass: isApprove,
            reason: isApprove ? '审核通过' : rejectReason,
            adminUsername: currentUser.value.username
        });

        Toast.fire(isApprove ? '已同意退款' : '已拒绝申请', '', 'success');
        fetchRefunds();
    } catch (e) {
        Swal.fire('操作失败', e.message || '系统繁忙', 'error');
    }
}

const switchTab = (tab) => {
    currentTab.value = tab;
    if (window.innerWidth < 1024) isSidebarOpen.value = false;

    if (tab === 'dashboard') {
        fetchStats();
        orderQuery.value.page = 1;
        fetchOrders();
    }
    else if (tab === 'orders') {
        orderQuery.value.page = 1;
        orderQuery.value.status = 'ALL';
        fetchOrders();
    }
    else if (tab === 'products') fetchProducts();
    else if (tab === 'users') fetchUsers();
    else if (tab === 'refund') fetchRefunds();
};

onMounted(() => {
    updateTheme();
    if (currentUser.value.role !== 'ADMIN') { router.push('/'); return; }
    fetchStats();
    fetchOrders();
    initWebSocket();
});

onUnmounted(() => { if (socket) socket.close(); });
</script>

<template>
    <div class="min-h-screen bg-[#F8FAFC] dark:bg-[#0f172a] flex font-sans transition-colors duration-300">

        <div v-if="isSidebarOpen" @click="isSidebarOpen = false"
            class="fixed inset-0 bg-black/50 z-30 lg:hidden backdrop-blur-sm transition-opacity"></div>

        <aside :class="['fixed inset-y-0 left-0 z-40 w-64 shadow-xl flex flex-col transition-all duration-300 overflow-hidden border-r border-slate-200 dark:border-slate-800',
            isSidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0',
            'bg-white dark:bg-[#0B1120]']">

            <div class="p-6 text-center border-b border-slate-100 dark:border-slate-800/50 relative z-10">
                <div class="flex items-center justify-center gap-3 mb-1">
                    <img src="/icons/logo.png" class="w-8 h-8 object-contain" />
                    <span class="text-xl font-bold text-slate-800 dark:text-white tracking-wider">渔鲜·后台</span>
                </div>
                <div class="text-[10px] text-slate-400 uppercase tracking-widest mt-1">管理系统</div>
            </div>

            <nav class="flex-1 mt-6 px-4 space-y-2 overflow-y-auto relative z-10">
                <a @click="switchTab('dashboard')"
                    :class="['flex items-center space-x-3 px-4 py-3 rounded-xl cursor-pointer transition-all duration-300 font-medium',
                        currentTab === 'dashboard'
                            ? 'bg-blue-50 text-blue-600 dark:bg-blue-600 dark:text-white shadow-sm'
                            : 'text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-slate-200']">
                    <span></span><span>数据总览</span>
                </a>

                <a @click="switchTab('orders')"
                    :class="['flex items-center space-x-3 px-4 py-3 rounded-xl cursor-pointer transition-all duration-300 font-medium',
                        currentTab === 'orders'
                            ? 'bg-blue-50 text-blue-600 dark:bg-blue-600 dark:text-white shadow-sm'
                            : 'text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-slate-200']">
                    <span></span><span>订单管理</span>
                </a>

                <a @click="switchTab('products')"
                    :class="['flex items-center space-x-3 px-4 py-3 rounded-xl cursor-pointer transition-all duration-300 font-medium',
                        currentTab === 'products'
                            ? 'bg-blue-50 text-blue-600 dark:bg-blue-600 dark:text-white shadow-sm'
                            : 'text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-slate-200']">
                    <span></span><span>商品管理</span>
                </a>

                <a @click="switchTab('refund')"
                    :class="['flex items-center space-x-3 px-4 py-3 rounded-xl cursor-pointer transition-all duration-300 font-medium',
                        currentTab === 'refund'
                            ? 'bg-blue-50 text-blue-600 dark:bg-blue-600 dark:text-white shadow-sm'
                            : 'text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-slate-200']">
                    <span></span><span>售后处理</span>
                </a>

                <a @click="switchTab('users')"
                    :class="['flex items-center space-x-3 px-4 py-3 rounded-xl cursor-pointer transition-all duration-300 font-medium',
                        currentTab === 'users'
                            ? 'bg-blue-50 text-blue-600 dark:bg-blue-600 dark:text-white shadow-sm'
                            : 'text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-slate-200']">
                    <span></span><span>用户管理</span>
                </a>
            </nav>

            <div class="p-4 border-t border-slate-100 dark:border-slate-800 relative z-10">
                <button @click="router.push('/')"
                    class="w-full flex items-center justify-center space-x-2 py-2.5 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-slate-500 hover:bg-slate-50 dark:hover:bg-slate-800 transition font-bold">
                    <span>⬅</span><span>返回商城首页</span>
                </button>
            </div>
        </aside>

        <main class="flex-1 p-4 lg:p-8 overflow-x-hidden relative transition-all duration-300 lg:ml-64">

            <header
                class="flex justify-between items-center mb-6 lg:mb-8 bg-white dark:bg-slate-900 p-4 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 sticky top-4 z-20 transition-colors duration-300">
                <div class="flex items-center gap-4">
                    <button @click="isSidebarOpen = true" class="lg:hidden p-2 text-slate-600 dark:text-slate-300">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                d="M4 6h16M4 12h16M4 18h16"></path>
                        </svg>
                    </button>
                    <div>
                        <div class="flex items-center text-xs text-slate-400 mb-1 font-medium">
                            <span>管理系统</span>
                            <span class="mx-1">/</span>
                            <span class="text-blue-500">{{ currentPageTitle }}</span>
                        </div>
                        <h1 class="text-xl font-bold text-slate-800 dark:text-white tracking-tight">{{ currentPageTitle
                            }}</h1>
                    </div>
                </div>
                <div class="flex items-center gap-3">
                    <button @click="toggleDark"
                        class="w-10 h-10 flex items-center justify-center rounded-full bg-slate-50 dark:bg-slate-800 text-slate-600 dark:text-yellow-400 hover:scale-105 transition shadow-sm border border-slate-100 dark:border-slate-700">
                        <img v-if="isDark" src="/icons/moon.png" class="w-5 h-5 object-contain" alt="Dark Mode" />
                        <img v-else src="/icons/sun.png" class="w-5 h-5 object-contain" alt="Light Mode" />
                    </button>
                    <div class="flex items-center gap-3 pl-3 border-l border-slate-200 dark:border-slate-700">
                        <div class="text-right hidden sm:block">
                            <div class="text-sm font-bold text-slate-700 dark:text-slate-200">{{ currentUser.displayName
                            }}</div>
                            <div class="text-[10px] text-slate-400 uppercase">Administrator</div>
                        </div>
                        <div
                            class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-bold text-sm border-2 border-white dark:border-slate-700 shadow-sm">
                            {{ currentUser.displayName?.charAt(0) || 'A' }}
                        </div>
                    </div>
                </div>
            </header>

            <div v-if="currentTab === 'dashboard'" class="animate-fade-in-up space-y-6">
                <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
                    <div v-for="(item, idx) in [{ label: '总销售额', val: '¥' + stats.totalSales.toLocaleString(), icon: '💰', color: 'text-emerald-500 bg-emerald-50 dark:bg-emerald-900/20' }, { label: '待发货订单', val: stats.pendingOrders, icon: '🔔', color: 'text-amber-500 bg-amber-50 dark:bg-amber-900/20' }, { label: '注册用户', val: stats.totalUsers, icon: '👥', color: 'text-blue-500 bg-blue-50 dark:bg-blue-900/20' }, { label: '库存商品', val: stats.totalProducts, icon: '📦', color: 'text-purple-500 bg-purple-50 dark:bg-purple-900/20' }]"
                        :key="idx"
                        class="bg-white dark:bg-slate-900 p-6 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 hover:shadow-md transition-shadow">
                        <div class="flex justify-between items-start">
                            <div>
                                <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">{{ item.label }}
                                </p>
                                <h3 class="text-2xl font-black text-slate-800 dark:text-white mt-2">{{ item.val }}</h3>
                            </div>
                            <span :class="['p-3 rounded-xl text-xl', item.color]">{{ item.icon }}</span>
                        </div>
                    </div>
                </div>

                <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    <div
                        class="lg:col-span-2 bg-white dark:bg-slate-900 p-6 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800">
                        <div ref="chartRef" class="w-full h-[350px]"></div>
                    </div>
                    
                </div>

                <div
                    class="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 overflow-hidden">
                    <div class="p-5 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center">
                        <h2 class="font-bold text-slate-800 dark:text-white">最新订单</h2>
                        <button @click="switchTab('orders')" class="text-sm text-blue-600 hover:underline">查看全部
                            ></button>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="w-full text-left text-sm text-slate-600 dark:text-slate-300">
                            <thead class="bg-slate-50 dark:bg-slate-800/50 text-xs uppercase font-bold text-slate-400">
                                <tr>
                                    <th class="p-4">订单号</th>
                                    <th class="p-4 w-1/4">内容</th>
                                    <th class="p-4">用户</th>
                                    <th class="p-4">金额</th>
                                    <th class="p-4">状态</th>
                                    <th class="p-4 text-center">操作</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                                <tr v-for="order in displayedOrders.slice(0, 5)" :key="order.id"
                                    class="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                                    <td class="p-4 font-mono">#{{ String(order.id).padStart(6, '0') }}</td>
                                    <td class="p-4 max-w-[200px] truncate" :title="order.productNames">{{
                                        order.productNames }}</td>
                                    <td class="p-4 font-bold">{{ order.username }}</td>
                                    <td class="p-4">¥{{ order.totalPrice.toFixed(2) }}</td>
                                    <td class="p-4"><span :class="getStatusClass(order.status)">{{
                                        formatStatus(order.status) }}</span></td>
                                    <td class="p-4 text-center">
                                        <button @click="openDetailModal(order)"
                                            class="text-blue-600 hover:underline mr-3">详情</button>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <div v-else-if="currentTab === 'orders'" class="animate-fade-in-up">
                <div
                    class="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 overflow-hidden">
                    <div
                        class="p-5 border-b border-slate-100 dark:border-slate-800 flex flex-col md:flex-row justify-between items-center gap-4">
                        <h2 class="font-bold text-slate-800 dark:text-white">订单管理</h2>

                        <div class="flex flex-col sm:flex-row gap-3 w-full md:w-auto">
                            <div class="flex gap-2 bg-slate-100 dark:bg-slate-800 p-1 rounded-lg">
                                <button v-for="tab in statusTabs" :key="tab.key" @click="switchStatusTab(tab.key)"
                                    :class="['px-3 py-1.5 text-xs rounded-md font-bold transition-colors', orderQuery.status === tab.key ? 'bg-white shadow text-blue-600 dark:bg-slate-700 dark:text-white' : 'text-slate-500 hover:text-slate-700 dark:text-slate-400']">
                                    {{ tab.label }}
                                </button>
                            </div>
                            <div class="flex gap-2">
                                <input v-model="orderQuery.keyword" @keyup.enter="handleSearch" placeholder="搜索订单/用户..."
                                    class="px-3 py-1.5 border border-slate-200 rounded-lg text-sm outline-none focus:border-blue-500 dark:bg-slate-800 dark:border-slate-700 dark:text-white">
                                <button @click="handleSearch"
                                    class="px-4 py-1.5 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700 font-bold">搜索</button>
                            </div>
                        </div>
                    </div>

                    <div class="overflow-x-auto min-h-[400px]">
                        <table class="w-full text-left text-sm text-slate-600 dark:text-slate-300">
                            <thead class="bg-slate-50 dark:bg-slate-800/50 text-xs uppercase font-bold text-slate-400">
                                <tr>
                                    <th class="p-4">订单号</th>
                                    <th class="p-4 w-1/4">商品信息</th>
                                    <th class="p-4">收货人</th>
                                    <th class="p-4">用户账号</th>
                                    <th class="p-4">实付金额</th>
                                    <th class="p-4">当前状态</th>
                                    <th class="p-4 text-center">操作</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                                <tr v-for="order in displayedOrders" :key="order.id"
                                    class="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                                    <td class="p-4 font-mono">#{{ 20250000 + order.id }}</td>
                                    <td class="p-4 max-w-[200px]" :title="order.productNames">
                                        <div class="truncate font-medium">{{ order.productNames }}</div>
                                        <div class="text-xs text-slate-400 mt-0.5">{{ new
                                            Date(order.createTime).toLocaleString() }}</div>
                                    </td>
                                    <td class="p-4">
                                        <div v-if="order.receiverName">
                                            <div class="font-bold text-slate-700 dark:text-slate-200">{{
                                                order.receiverName }}</div>
                                            <div class="text-xs text-slate-400 mt-0.5">{{ order.receiverPhone }}</div>
                                        </div>
                                    </td>
                                    <td class="p-4 font-bold">{{ order.username }}</td>
                                    <td class="p-4 font-mono">¥{{ order.totalPrice.toFixed(2) }}</td>
                                    <td class="p-4"><span :class="getStatusClass(order.status)">{{
                                        formatStatus(order.status) }}</span></td>
                                    <td class="p-4 text-center">
                                        <button @click="openDetailModal(order)"
                                            class="text-blue-600 hover:underline mr-3 font-medium">详情</button>
                                        <button v-if="['PAID', '待发货'].includes(order.status)"
                                            @click="handleShip(order.id)"
                                            class="text-white bg-blue-600 hover:bg-blue-700 px-3 py-1 rounded-lg text-xs shadow-sm shadow-blue-200 dark:shadow-none">发货</button>
                                    </td>
                                </tr>
                                <tr v-if="displayedOrders.length === 0">
                                    <td colspan="7" class="p-12 text-center text-slate-400">
                                        暂无符合条件的订单
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                    <div
                        class="p-4 border-t border-slate-100 dark:border-slate-800 flex justify-center items-center gap-4">
                        <button @click="changePage(orderQuery.page - 1)" :disabled="orderQuery.page <= 1"
                            class="px-4 py-2 border rounded-lg hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed dark:border-slate-700 dark:hover:bg-slate-800 text-sm">上一页</button>
                        <span class="text-sm font-bold text-slate-600 dark:text-slate-400">第 {{ orderQuery.page }}
                            页</span>
                        <button @click="changePage(orderQuery.page + 1)"
                            :disabled="displayedOrders.length < orderQuery.size"
                            class="px-4 py-2 border rounded-lg hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed dark:border-slate-700 dark:hover:bg-slate-800 text-sm">下一页</button>
                    </div>
                </div>
            </div>

            <div v-else-if="currentTab === 'products'" class="animate-fade-in-up">
                <div
                    class="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 overflow-hidden">
                    <div class="p-5 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center">
                        <h2 class="font-bold text-slate-800 dark:text-white">商品列表</h2>
                        <button @click="openProductModal(null)"
                            class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-bold hover:bg-blue-700 shadow-lg shadow-blue-200 dark:shadow-none transition">+
                            新增商品</button>
                    </div>
                    <table class="w-full text-left text-sm text-slate-600 dark:text-slate-300">
                        <thead class="bg-slate-50 dark:bg-slate-800/50 font-bold">
                            <tr>
                                <th class="p-4">图片</th>
                                <th class="p-4">名称</th>
                                <th class="p-4">价格</th>
                                <th class="p-4">库存</th>
                                <th class="p-4">描述</th>
                                <th class="p-4 text-center">操作</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                            <tr v-for="p in products" :key="p.id" class="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                                <td class="p-4"><img :src="p.imageUrl"
                                        class="w-10 h-10 rounded border dark:border-slate-700 object-cover"></td>
                                <td class="p-4 font-bold">{{ p.name }}</td>
                                <td class="p-4 text-orange-500 font-mono">¥{{ p.price }}</td>
                                <td class="p-4"><span
                                        :class="p.stock < 10 ? 'text-red-500 font-bold' : 'text-slate-500'">{{ p.stock
                                        }}</span></td>
                                <td class="p-4 max-w-xs truncate text-slate-400">{{ p.description }}</td>
                                <td class="p-4 text-center space-x-3">
                                    <button @click="openProductModal(p)" class="text-blue-600">编辑</button>
                                    <button @click="handleDeleteProduct(p.id)" class="text-red-500">下架</button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div v-else-if="currentTab === 'refund'" class="animate-fade-in-up">
                <div
                    class="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 overflow-hidden">
                    <div class="p-5 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center">
                        <h2 class="font-bold text-slate-800 dark:text-white">售后申请列表</h2>
                        <span class="bg-orange-100 text-orange-700 px-3 py-1 rounded-full text-xs font-bold">待处理: {{
                            refundList.filter(r => r.status === '售后处理中' || r.status === 'PENDING').length}}</span>
                    </div>
                    <table class="w-full text-left text-sm text-slate-600 dark:text-slate-300">
                        <thead class="bg-slate-50 dark:bg-slate-800/50 font-bold text-slate-500">
                            <tr>
                                <th class="p-5">关联订单</th>
                                <th class="p-5">申请用户</th>
                                <th class="p-5">退款金额</th>
                                <th class="p-5">申请原因</th>
                                <th class="p-5">状态</th>
                                <th class="p-5 text-center">操作</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                            <tr v-for="rf in refundList" :key="rf.id || rf.orderId"
                                class="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                                <td class="p-5 font-mono text-xs text-blue-500 hover:underline cursor-pointer">
                                    #{{ 20250000 + (rf.orderId || rf.id) }}
                                    <div class="text-[10px] text-slate-400 mt-1">{{ new Date(rf.applyTime ||
                                        rf.createTime).toLocaleString() }}</div>
                                </td>
                                <td class="p-5 font-bold">{{ rf.username || rf.user }}</td>
                                <td class="p-5 font-mono font-bold text-slate-800 dark:text-white">¥{{ (rf.amount ||
                                    rf.totalPrice || 0).toFixed(2) }}</td>
                                <td class="p-5 text-slate-500 max-w-[200px] truncate" :title="rf.reason">{{ rf.reason ||
                                    '无详细原因' }}</td>
                                <td class="p-5">
                                    <span v-if="rf.status === '售后处理中' || rf.status === 'PENDING'"
                                        class="bg-orange-100 text-orange-600 px-2 py-1 rounded text-xs font-bold">待处理</span>
                                    <span v-else-if="rf.status === '退款成功' || rf.status === 'APPROVED'"
                                        class="bg-green-100 text-green-600 px-2 py-1 rounded text-xs font-bold">已同意</span>
                                    <span v-else
                                        class="bg-slate-100 text-slate-600 px-2 py-1 rounded text-xs font-bold">{{
                                            rf.status }}</span>
                                </td>
                                <td class="p-5 text-center">
                                    <div v-if="rf.status === '售后处理中' || rf.status === 'PENDING'"
                                        class="flex justify-center gap-2">
                                        <button @click="handleRefundAction(rf.orderId || rf.id, 'approve')"
                                            class="text-green-600 hover:bg-green-50 px-3 py-1 rounded text-xs font-bold border border-green-200 transition">同意</button>
                                        <button @click="handleRefundAction(rf.orderId || rf.id, 'reject')"
                                            class="text-red-600 hover:bg-red-50 px-3 py-1 rounded text-xs font-bold border border-red-200 transition">拒绝</button>
                                    </div>
                                    <span v-else class="text-xs text-slate-400">已归档</span>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                    <div v-if="refundList.length === 0" class="p-12 text-center text-slate-400">
                        暂无售后申请
                    </div>
                </div>
            </div>

            <div v-else-if="currentTab === 'users'" class="animate-fade-in-up">
                <div
                    class="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 overflow-hidden">
                    <div class="p-5 border-b border-slate-100 dark:border-slate-800">
                        <h2 class="font-bold text-slate-800 dark:text-white">会员列表</h2>
                    </div>
                    <table class="w-full text-left text-sm text-slate-600 dark:text-slate-300">
                        <thead class="bg-slate-50 dark:bg-slate-800/50 font-bold">
                            <tr>
                                <th class="p-4">用户</th>
                                <th class="p-4">角色</th>
                                <th class="p-4">积分</th>
                                <th class="p-4 text-center">操作</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                            <tr v-for="u in users" :key="u.id" class="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                                <td class="p-4 font-bold">{{ u.username }}</td>
                                <td class="p-4"><span
                                        class="bg-slate-100 dark:bg-slate-800 px-2 py-1 rounded text-xs">{{ u.role
                                        }}</span></td>
                                <td class="p-4 font-mono text-amber-500 font-bold">{{ u.points }}</td>
                                <td class="p-4 text-center"><button @click="openPointModal(u)"
                                        class="text-blue-600 hover:underline">修改积分</button></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div v-if="showProductModal"
                class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
                <div class="bg-white dark:bg-slate-900 p-6 rounded-2xl w-full max-w-lg shadow-2xl animate-scale-up">
                    <h3 class="font-bold mb-4 text-lg dark:text-white">{{ editingProduct.id ? '编辑' : '新增' }}商品</h3>
                    <div class="space-y-4">
                        <input v-model="editingProduct.name"
                            class="w-full p-3 border rounded-xl dark:bg-slate-800 dark:border-slate-700 dark:text-white"
                            placeholder="商品名称">
                        <div class="grid grid-cols-2 gap-4">
                            <input v-model="editingProduct.price"
                                class="p-3 border rounded-xl dark:bg-slate-800 dark:border-slate-700 dark:text-white"
                                placeholder="价格">
                            <input v-model="editingProduct.stock"
                                class="p-3 border rounded-xl dark:bg-slate-800 dark:border-slate-700 dark:text-white"
                                placeholder="库存">
                        </div>
                        <input v-model="editingProduct.imageUrl"
                            class="w-full p-3 border rounded-xl dark:bg-slate-800 dark:border-slate-700 dark:text-white"
                            placeholder="图片URL">
                        <textarea v-model="editingProduct.description"
                            class="w-full p-3 border rounded-xl dark:bg-slate-800 dark:border-slate-700 dark:text-white"
                            placeholder="描述"></textarea>
                    </div>
                    <div class="flex justify-end gap-3 mt-6">
                        <button @click="showProductModal = false"
                            class="px-4 py-2 text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg">取消</button>
                        <button @click="saveProduct"
                            class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">保存</button>
                    </div>
                </div>
            </div>

            <div v-if="showPointModal"
                class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
                <div class="bg-white dark:bg-slate-900 p-6 rounded-2xl w-full max-w-sm shadow-2xl animate-scale-up">
                    <h3 class="font-bold mb-4 dark:text-white">修改积分</h3>
                    <input v-model="editingUser.points" type="number"
                        class="w-full p-3 border rounded-xl mb-4 dark:bg-slate-800 dark:border-slate-700 dark:text-white">
                    <div class="flex justify-end gap-3">
                        <button @click="showPointModal = false" class="px-4 py-2 text-slate-500">取消</button>
                        <button @click="saveUserPoints" class="px-4 py-2 bg-blue-600 text-white rounded-lg">保存</button>
                    </div>
                </div>
            </div>

            <div v-if="showDetailModal"
                class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
                <div class="bg-white dark:bg-slate-900 p-6 rounded-2xl w-full max-w-2xl shadow-2xl animate-scale-up">
                    <div class="flex justify-between items-center mb-6 border-b pb-4 dark:border-slate-800">
                        <h3 class="font-bold text-lg dark:text-white">订单详情 <span
                                class="text-sm font-mono text-slate-400">#{{ currentOrderDetails.id }}</span></h3>
                        <button @click="showDetailModal = false"
                            class="text-2xl text-slate-400 hover:text-slate-600">&times;</button>
                    </div>
                    <div class="space-y-4">
                        <div class="p-4 bg-slate-50 dark:bg-slate-800 rounded-xl">
                            <p class="text-sm text-slate-500 dark:text-slate-400 mb-1">收货信息</p>
                            <p class="font-bold text-slate-800 dark:text-white">{{ currentOrderDetails.receiverName }}
                                {{ currentOrderDetails.receiverPhone }}</p>
                            <p class="text-sm text-slate-600 dark:text-slate-300 mt-1">{{
                                currentOrderDetails.receiverAddress }}</p>
                        </div>
                        <div>
                            <p class="text-sm text-slate-500 mb-2">商品清单</p>
                            <div class="border rounded-xl dark:border-slate-700 divide-y dark:divide-slate-700">
                                <div v-for="item in currentOrderDetails.items" :key="item.id"
                                    class="p-3 flex justify-between items-center">
                                    <span class="font-medium dark:text-white">{{ item.productName }}</span>
                                    <div class="text-right">
                                        <div class="text-sm text-slate-500">x{{ item.quantity }}</div>
                                        <div class="font-mono dark:text-white">¥{{ item.price }}</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

        </main>
    </div>
</template>

<style scoped>
.animate-fade-in-up {
    animation: fadeInUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.animate-scale-up {
    animation: scaleUp 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes fadeInUp {
    from {
        opacity: 0;
        transform: translateY(20px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

@keyframes scaleUp {
    from {
        opacity: 0;
        transform: scale(0.95);
    }

    to {
        opacity: 1;
        transform: scale(1);
    }
}
</style>