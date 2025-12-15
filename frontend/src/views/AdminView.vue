<script setup>
import { ref, onMounted, nextTick, watch, onUnmounted } from 'vue';
import { request } from '@/utils/request';
import { useRouter, useRoute } from 'vue-router';
import * as echarts from 'echarts';
import Swal from 'sweetalert2';

const router = useRouter();
const route = useRoute();

// === 全局状态 ===
const currentTab = ref('dashboard');
const currentUser = ref(JSON.parse(localStorage.getItem('yuxian_user') || '{}'));
const loading = ref(false);
const isSidebarOpen = ref(false);
const isDark = ref(localStorage.getItem('theme') === 'dark');

// === 仪表盘 & 订单状态 ===
const stats = ref({
    totalSales: 0, totalOrders: 0, totalUsers: 0, pendingOrders: 0, totalProducts: 0,
    chartData: { dates: [], values: [] }
});
const allOrders = ref([]);
const displayedOrders = ref([]);
const chartRef = ref(null);
let myChart = null;

const orderQuery = ref({ page: 1, size: 10, keyword: '', status: 'ALL', total: 0 });
const statusTabs = [
    { key: 'ALL', label: '全部' }, { key: 'PAID', label: '待发货' },
    { key: 'SHIPPED', label: '运输中' }, { key: '已送达', label: '已完成' }
];

const showDetailModal = ref(false);
const currentOrderDetails = ref({});

// === 商品 & 用户状态 ===
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

// 监听路由变化自动关闭侧边栏
watch(() => route.path, () => { isSidebarOpen.value = false; });

// ===========================
// 📡 WebSocket 实时通信逻辑
// ===========================
let socket = null;

const initWebSocket = () => {
    if (typeof (WebSocket) === "undefined") {
        console.error("您的浏览器不支持WebSocket");
        return;
    }
    const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
    const wsUrl = `${protocol}localhost:8080/ws/orders`;

    socket = new WebSocket(wsUrl);

    socket.onopen = () => { console.log("WebSocket已连接"); };

    socket.onmessage = (msg) => {
        if (msg.data === 'NEW_ORDER') {
            Toast.fire({ icon: 'info', title: '🔔 收到新订单！', text: '列表已自动刷新', timer: 5000 });
            if (currentTab.value === 'dashboard') {
                fetchStats();
                fetchOrders(false);
            }
        }
    };

    socket.onclose = () => { console.log("WebSocket已关闭"); };
    socket.onerror = (err) => { console.error("WebSocket错误", err); };
};

// ===========================
// 🎨 暗黑模式逻辑
// ===========================
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

// ===========================
// 📊 图表逻辑
// ===========================
const initChart = () => {
    if (!chartRef.value) return;
    if (myChart) myChart.dispose();

    myChart = echarts.init(chartRef.value, isDark.value ? 'dark' : undefined, { renderer: 'svg' });

    const textColor = isDark.value ? '#94a3b8' : '#334155';
    const splitLineColor = isDark.value ? '#334155' : '#e2e8f0';
    const areaColorStart = isDark.value ? 'rgba(59, 130, 246, 0.5)' : 'rgba(37, 99, 235, 0.2)';
    const areaColorEnd = isDark.value ? 'rgba(59, 130, 246, 0)' : 'rgba(37, 99, 235, 0)';

    const option = {
        backgroundColor: 'transparent',
        title: { text: '近7日销售趋势', left: 'left', textStyle: { fontSize: 16, color: textColor } },
        tooltip: { trigger: 'axis', formatter: '{b} <br/> 销售额: ¥{c}' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
            type: 'category', boundaryGap: false, data: stats.value.chartData.dates,
            axisLine: { lineStyle: { color: splitLineColor } }, axisLabel: { color: textColor }
        },
        yAxis: {
            type: 'value', splitLine: { lineStyle: { type: 'dashed', color: splitLineColor } }, axisLabel: { color: textColor }
        },
        series: [{
            name: '销售额', type: 'line', smooth: true, data: stats.value.chartData.values,
            itemStyle: { color: '#3b82f6' },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: areaColorStart }, { offset: 1, color: areaColorEnd }]) }
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

// ===========================
// 📦 订单管理逻辑
// ===========================
const fetchOrders = async (showLoading = true) => {
    if (showLoading) {
        loading.value = true;
        displayedOrders.value = [];
    }
    try {
        const params = new URLSearchParams({ page: orderQuery.value.page, size: orderQuery.value.size, keyword: orderQuery.value.keyword });
        if (showLoading) await new Promise(r => setTimeout(r, 400));

        const res = await request.get(`/api/admin/orders?${params.toString()}`);
        let rawList = Array.isArray(res) ? res : (res.content || []);
        orderQuery.value.total = Array.isArray(res) ? res.length : (res.totalElements || 0);
        allOrders.value = rawList;
        applyClientSideFilter();
    } catch (err) { console.error(err); } finally { loading.value = false; }
};

const applyClientSideFilter = () => {
    const status = orderQuery.value.status;
    if (status === 'ALL') displayedOrders.value = allOrders.value;
    else displayedOrders.value = allOrders.value.filter(o => {
        if (status === 'PAID') return o.status === 'PAID' || o.status === '待发货';
        if (status === 'SHIPPED') return o.status === 'SHIPPED' || o.status === '运输中';
        if (status === '已送达') return o.status === 'DELIVERED' || o.status === '已送达';
        return o.status === status;
    });
};

const switchStatusTab = (k) => { orderQuery.value.status = k; applyClientSideFilter(); };
const handleSearch = () => { orderQuery.value.page = 1; fetchOrders(); };
const changePage = (p) => { if (p < 1) return; orderQuery.value.page = p; fetchOrders(); };

const handleShip = async (id) => {
    const res = await Swal.fire({ title: '发货?', icon: 'info', showCancelButton: true, confirmButtonText: '发货' });
    if (res.isConfirmed) {
        await request.put(`/api/admin/orders/${id}/status`, { status: 'SHIPPED' });
        fetchOrders(false);
        Toast.fire('已发货', '', 'success');
    }
};

const openDetailModal = (o) => { currentOrderDetails.value = o; showDetailModal.value = true; };
const formatStatus = (s) => ({ 'PAID': '待发货', 'SHIPPED': '运输中', 'DELIVERED': '已送达', '已送达': '已送达' }[s] || s);

// ✅ UI优化2: 状态徽章现代化 (rounded-md, 更通透的颜色)
const getStatusClass = (s) => {
    const base = "inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium transition-colors ";
    if (['SHIPPED', '运输中'].includes(s)) return base + "bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300";
    if (['DELIVERED', '已送达'].includes(s)) return base + "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300";
    // 待发货/未支付
    return base + "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300";
};

// ===========================
// 🛍️ 商品管理逻辑
// ===========================
const fetchProducts = async () => {
    loading.value = true; products.value = [];
    try { await new Promise(r => setTimeout(r, 400)); const res = await request.get('/api/products'); products.value = res || []; }
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

// ===========================
// 👥 用户管理逻辑
// ===========================
const fetchUsers = async () => {
    loading.value = true; users.value = [];
    try { await new Promise(r => setTimeout(r, 400)); users.value = await request.get('/api/admin/users') || []; }
    finally { loading.value = false; }
};
const openPointModal = (u) => { editingUser.value = { ...u }; showPointModal.value = true; };
const saveUserPoints = async () => { await request.put(`/api/admin/users/${editingUser.value.id}/points`, { points: parseInt(editingUser.value.points) }); showPointModal.value = false; fetchUsers(); Toast.fire('修改成功', '', 'success'); };
const handleDeleteUser = async (id) => { if ((await Swal.fire({ title: '删除用户?', icon: 'error', showCancelButton: true })).isConfirmed) { await request.delete(`/api/admin/users/${id}`); fetchUsers(); } };

const switchTab = (tab) => {
    currentTab.value = tab;
    if (window.innerWidth < 1024) isSidebarOpen.value = false;
    if (tab === 'dashboard') { fetchStats(); fetchOrders(); }
    else if (tab === 'products') fetchProducts();
    else if (tab === 'users') fetchUsers();
};

onMounted(() => {
    updateTheme();
    if (currentUser.value.role !== 'ADMIN') { router.push('/'); return; }
    fetchStats();
    fetchOrders();
    initWebSocket();
});

onUnmounted(() => {
    if (socket) socket.close();
});
</script>

<template>
    <div class="min-h-screen bg-[#F1F5F9] dark:bg-slate-950 flex font-sans transition-colors duration-300">

        <div v-if="isSidebarOpen" @click="isSidebarOpen = false"
            class="fixed inset-0 bg-black/50 z-30 lg:hidden backdrop-blur-sm transition-opacity"></div>

        <aside
            :class="['fixed inset-y-0 left-0 z-40 w-64 bg-slate-900/95 dark:bg-black/90 backdrop-blur-xl text-white shadow-2xl flex flex-col transition-transform duration-300', isSidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0']">
            <div class="p-8 text-2xl font-black tracking-widest text-center border-b border-white/10">
                YU·XIAN
                <span class="block text-xs font-normal text-slate-400 mt-1 tracking-normal">管理控制台</span>
            </div>

            <nav class="flex-1 mt-6 space-y-1 overflow-y-auto">
                <a @click="switchTab('dashboard')"
                    :class="['flex items-center space-x-3 px-6 py-4 cursor-pointer transition-all border-l-4 hover:bg-white/5',
                        currentTab === 'dashboard' ? 'border-blue-500 bg-white/10 text-white' : 'border-transparent text-slate-400 hover:text-white']">
                    <span>📊</span><span class="font-medium tracking-wide">数据总览</span>
                </a>

                <a @click="switchTab('products')"
                    :class="['flex items-center space-x-3 px-6 py-4 cursor-pointer transition-all border-l-4 hover:bg-white/5',
                        currentTab === 'products' ? 'border-blue-500 bg-white/10 text-white' : 'border-transparent text-slate-400 hover:text-white']">
                    <span>📦</span><span class="font-medium tracking-wide">商品管理</span>
                </a>

                <router-link to="/admin/refund" active-class="border-blue-500 bg-white/10 text-white !text-white"
                    class="flex items-center space-x-3 px-6 py-4 cursor-pointer transition-all border-l-4 hover:bg-white/5 border-transparent text-slate-400 hover:text-white">
                    <span>🛡️</span><span class="font-medium tracking-wide">售后处理</span>
                </router-link>
                <a @click="switchTab('users')"
                    :class="['flex items-center space-x-3 px-6 py-4 cursor-pointer transition-all border-l-4 hover:bg-white/5',
                        currentTab === 'users' ? 'border-blue-500 bg-white/10 text-white' : 'border-transparent text-slate-400 hover:text-white']">
                    <span>👥</span><span class="font-medium tracking-wide">用户管理</span>
                </a>
            </nav>
            <div class="p-6 border-t border-white/10">
                <button @click="router.push('/')"
                    class="w-full flex items-center justify-center space-x-2 py-3 border border-slate-600 rounded-xl text-sm text-slate-300 hover:bg-white/10 hover:text-white transition"><span>⬅</span><span>返回商城</span></button>
            </div>
        </aside>

        <main class="flex-1 p-4 lg:p-8 overflow-x-hidden relative transition-all duration-300 lg:ml-64">

            <header
                class="flex justify-between items-center mb-6 lg:mb-10 bg-white dark:bg-slate-900 p-4 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 sticky top-0 z-20 transition-colors duration-300">
                <div class="flex items-center gap-4">
                    <button @click="isSidebarOpen = true"
                        class="lg:hidden p-2 -ml-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                d="M4 6h16M4 12h16M4 18h16"></path>
                        </svg>
                    </button>
                    <div>
                        <h1 class="text-xl lg:text-2xl font-bold text-slate-800 dark:text-white">
                            {{ currentTab === 'dashboard' ? '运营概况' : (currentTab === 'products' ? '商品库管理' : '会员管理') }}
                        </h1>
                        <p class="text-xs lg:text-sm text-slate-500 dark:text-slate-400 mt-1 hidden sm:block">欢迎回来, {{
                            currentUser.displayName || '管理员' }} 👋</p>
                    </div>
                </div>
                <div class="flex items-center gap-4">
                    <button @click="toggleDark"
                        class="p-2 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-yellow-400 hover:scale-110 transition shadow-sm">
                        <span v-if="isDark">🌙</span><span v-else>☀️</span>
                    </button>
                    <div
                        class="h-9 w-9 lg:h-10 lg:w-10 rounded-full bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-300 flex items-center justify-center font-bold shadow-sm">
                        A</div>
                </div>
            </header>

            <div v-if="currentTab === 'dashboard'" class="animate-fade-in-up">
                <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 lg:gap-6 mb-8">
                    <div v-for="(item, idx) in [
                        { label: '总销售额', val: '¥' + stats.totalSales.toLocaleString(), icon: '💰' },
                        { label: '待发货', val: stats.pendingOrders, icon: '🔔' },
                        { label: '用户数', val: stats.totalUsers, icon: '👥' },
                        { label: '商品数', val: stats.totalProducts, icon: '📦' }
                    ]" :key="idx"
                        class="bg-white dark:bg-slate-900 p-6 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 hover:shadow-xl hover:-translate-y-1 transition-all duration-300">
                        <div class="flex justify-between items-start">
                            <div>
                                <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">{{ item.label }}
                                </p>
                                <h3 class="text-3xl font-black text-slate-800 dark:text-white mt-2">{{ item.val }}</h3>
                            </div>
                            <span class="p-3 rounded-xl text-2xl bg-slate-50 dark:bg-slate-800">{{ item.icon }}</span>
                        </div>
                    </div>
                </div>

                <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">
                    <div
                        class="lg:col-span-2 bg-white dark:bg-slate-900 p-6 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 transition-colors">
                        <div ref="chartRef" class="w-full h-[350px]"></div>
                    </div>
                    <div
                        class="bg-white dark:bg-slate-900 p-6 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 transition-colors">
                        <h3 class="font-bold text-slate-800 dark:text-white mb-4">系统公告</h3>
                        <div class="space-y-4">
                            <div
                                class="p-4 bg-blue-50 dark:bg-blue-900/20 rounded-xl border border-blue-100 dark:border-blue-800 text-sm text-blue-800 dark:text-blue-300">
                                <strong>🚀 升级：</strong> UI 全面优化！表格更清晰，操作更顺滑。
                            </div>
                        </div>
                    </div>
                </div>

                <div
                    class="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 overflow-hidden transition-colors">
                    <div class="p-6 border-b border-slate-100 dark:border-slate-800">
                        <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-4">
                            <h2 class="text-lg font-bold text-slate-800 dark:text-white">订单管理</h2>
                            <div class="flex w-full sm:w-auto gap-3">
                                <div
                                    class="relative group w-full sm:w-64 transition-all duration-300 focus-within:w-full sm:focus-within:w-80">
                                    <input v-model="orderQuery.keyword" @keyup.enter="handleSearch" type="text"
                                        placeholder="搜索订单号或用户名..."
                                        class="w-full pl-10 pr-12 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none transition-all dark:text-white placeholder-slate-400">
                                    <span
                                        class="absolute left-3.5 top-2.5 text-slate-400 group-focus-within:text-blue-500 transition-colors">
                                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                                        </svg>
                                    </span>
                                    <button @click="handleSearch"
                                        class="absolute right-1.5 top-1.5 p-1 bg-white dark:bg-slate-700 hover:bg-blue-50 dark:hover:bg-slate-600 text-slate-400 hover:text-blue-600 rounded-lg transition-colors shadow-sm border border-slate-100 dark:border-slate-600"
                                        title="点击搜索">
                                        <svg class="w-4 h-4 transform rotate-90" fill="none" stroke="currentColor"
                                            viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                                d="M5 12h14M12 5l7 7-7 7"></path>
                                        </svg>
                                    </button>
                                </div>
                                <button @click="fetchOrders"
                                    class="flex items-center gap-1 px-3 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-slate-500 hover:text-blue-600 hover:border-blue-200 hover:bg-blue-50 transition-all text-sm group"
                                    title="刷新列表">
                                    <span class="group-hover:rotate-180 transition-transform duration-500">↻</span>
                                </button>
                            </div>
                        </div>
                        <div
                            class="flex space-x-1 bg-slate-100 dark:bg-slate-800 p-1 rounded-lg w-full overflow-x-auto">
                            <button v-for="tab in statusTabs" :key="tab.key" @click="switchStatusTab(tab.key)"
                                :class="['flex-shrink-0 px-4 py-1.5 rounded-md text-xs font-bold transition-all', orderQuery.status === tab.key ? 'bg-white dark:bg-slate-700 text-blue-600 dark:text-blue-400 shadow-sm' : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-white']">{{
                                    tab.label }}</button>
                        </div>
                    </div>

                    <div class="overflow-x-auto">
                        <table class="w-full text-left text-sm min-w-[900px] text-slate-600 dark:text-slate-300">
                            <thead
                                class="bg-slate-50 dark:bg-slate-800/50 border-b border-slate-100 dark:border-slate-700 text-slate-500 dark:text-slate-400 uppercase text-xs font-bold tracking-wider">
                                <tr>
                                    <th class="p-5 pl-6">订单号</th>
                                    <th class="p-5 w-1/4">内容</th>
                                    <th class="p-5">收货信息</th>
                                    <th class="p-5">用户</th>
                                    <th class="p-5 text-right">金额</th>
                                    <th class="p-5 text-center">状态</th>
                                    <th class="p-5 text-center">操作</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                                <tr v-if="loading" v-for="i in 5" :key="'skel-' + i" class="animate-pulse">
                                    <td class="p-5">
                                        <div class="h-4 bg-slate-200 dark:bg-slate-700 rounded w-12"></div>
                                    </td>
                                    <td class="p-5">
                                        <div class="h-4 bg-slate-200 dark:bg-slate-700 rounded w-full"></div>
                                    </td>
                                    <td class="p-5">
                                        <div class="h-4 bg-slate-200 dark:bg-slate-700 rounded w-24"></div>
                                    </td>
                                    <td class="p-5">
                                        <div class="h-8 w-8 bg-slate-200 dark:bg-slate-700 rounded-full"></div>
                                    </td>
                                    <td class="p-5">
                                        <div class="h-4 bg-slate-200 dark:bg-slate-700 rounded w-16 ml-auto"></div>
                                    </td>
                                    <td class="p-5">
                                        <div class="h-6 bg-slate-200 dark:bg-slate-700 rounded w-16 mx-auto"></div>
                                    </td>
                                    <td class="p-5">
                                        <div class="h-8 bg-slate-200 dark:bg-slate-700 rounded w-12 mx-auto"></div>
                                    </td>
                                </tr>
                                <template v-else>
                                    <tr v-for="order in displayedOrders" :key="order.id"
                                        class="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors duration-200">
                                        <td class="p-5 pl-6 font-mono font-medium text-slate-500">
                                            <span class="bg-slate-100 dark:bg-slate-800 px-2 py-1 rounded text-xs">#{{
                                                String(order.id).padStart(6, '0') }}</span>
                                        </td>
                                        <td class="p-5 max-w-[200px] truncate text-slate-700 dark:text-slate-200 font-medium"
                                            :title="order.productNames">{{ order.productNames }}</td>
                                        <td class="p-5 max-w-[200px]">
                                            <div v-if="order.receiverName">
                                                <div class="font-bold text-slate-700 dark:text-slate-200">{{
                                                    order.receiverName }} <span
                                                        class="text-slate-400 font-normal ml-1">{{ order.receiverPhone
                                                        }}</span></div>
                                                <div class="text-xs text-slate-500 dark:text-slate-500 mt-1 truncate">{{
                                                    order.receiverAddress }}</div>
                                            </div>
                                            <div v-else class="text-xs text-slate-300 italic">(旧订单)</div>
                                        </td>
                                        <td class="p-5">
                                            <div class="flex items-center gap-3">
                                                <div
                                                    class="w-8 h-8 rounded-md bg-gradient-to-br from-blue-100 to-blue-50 dark:from-blue-900 dark:to-slate-800 flex items-center justify-center font-bold text-blue-600 dark:text-blue-300 text-xs shadow-sm">
                                                    {{ order.username.charAt(0).toUpperCase() }}</div>
                                                <span class="font-bold text-slate-700 dark:text-slate-200">{{
                                                    order.username }}</span>
                                            </div>
                                        </td>
                                        <td class="p-5 text-right font-mono font-bold text-slate-800 dark:text-white">
                                            ¥{{ order.totalPrice.toFixed(2) }}</td>
                                        <td class="p-5 text-center"><span :class="getStatusClass(order.status)">{{
                                            formatStatus(order.status) }}</span></td>
                                        <td class="p-5 text-center space-x-2">
                                            <button @click="openDetailModal(order)"
                                                class="text-xs bg-white border border-slate-200 dark:bg-slate-800 dark:border-slate-700 hover:border-blue-500 hover:text-blue-600 dark:hover:text-blue-400 px-3 py-1.5 rounded transition shadow-sm">详情</button>
                                            <button v-if="['PAID', '待发货'].includes(order.status)"
                                                @click="handleShip(order.id)"
                                                class="text-xs bg-blue-600 text-white px-3 py-1.5 rounded hover:bg-blue-700 shadow-sm shadow-blue-200">发货</button>
                                        </td>
                                    </tr>
                                </template>
                            </tbody>
                        </table>
                    </div>
                    <div
                        class="p-5 border-t border-slate-100 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/50 flex justify-between items-center">
                        <span class="text-xs text-slate-500 dark:text-slate-400">共 {{ orderQuery.total }} 条数据</span>
                        <div class="flex gap-2">
                            <button @click="changePage(orderQuery.page - 1)" :disabled="orderQuery.page === 1"
                                class="px-3 py-1 border border-slate-300 dark:border-slate-700 rounded bg-white dark:bg-slate-800 disabled:opacity-50 text-sm hover:border-blue-500 transition-colors">上一页</button>
                            <button @click="changePage(orderQuery.page + 1)"
                                :disabled="orderQuery.page >= Math.ceil(orderQuery.total / orderQuery.size)"
                                class="px-3 py-1 border border-slate-300 dark:border-slate-700 rounded bg-white dark:bg-slate-800 disabled:opacity-50 text-sm hover:border-blue-500 transition-colors">下一页</button>
                        </div>
                    </div>
                </div>
            </div>

            <div v-else-if="currentTab === 'products'" class="animate-fade-in-up">
                <div class="flex justify-between items-center mb-6">
                    <p class="text-slate-500 dark:text-slate-400 text-sm">管理商品库存</p>
                    <button @click="openProductModal(null)"
                        class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 shadow-lg shadow-blue-200 dark:shadow-none hover:-translate-y-0.5 transition-all"><span>+</span>
                        新增商品</button>
                </div>
                <div
                    class="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 overflow-hidden">
                    <table class="w-full text-left text-sm text-slate-600 dark:text-slate-300">
                        <thead
                            class="bg-slate-50 dark:bg-slate-800/50 text-slate-500 dark:text-slate-400 font-bold border-b border-slate-100 dark:border-slate-700">
                            <tr>
                                <th class="p-5">图片</th>
                                <th class="p-5">名称</th>
                                <th class="p-5">价格</th>
                                <th class="p-5">库存</th>
                                <th class="p-5">描述</th>
                                <th class="p-5 text-center">操作</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                            <tr v-if="loading" v-for="i in 5" :key="'pskel-' + i" class="animate-pulse">
                                <td class="p-5">
                                    <div class="h-10 w-10 bg-slate-200 dark:bg-slate-700 rounded"></div>
                                </td>
                                <td class="p-5" colspan="5">
                                    <div class="h-4 bg-slate-200 dark:bg-slate-700 rounded w-full"></div>
                                </td>
                            </tr>
                            <template v-else>
                                <tr v-for="p in products" :key="p.id"
                                    class="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                                    <td class="p-5"><img :src="p.imageUrl"
                                            class="w-12 h-12 rounded-lg border dark:border-slate-700 bg-gray-100 object-cover shadow-sm">
                                    </td>
                                    <td class="p-5 font-bold text-slate-700 dark:text-white">{{ p.name }}</td>
                                    <td class="p-5 text-orange-600 font-bold font-mono">¥{{ p.price }}</td>
                                    <td class="p-5">
                                        <div v-if="p.stock < 10"
                                            class="inline-flex items-center gap-1 bg-red-50 dark:bg-red-900/30 text-red-600 dark:text-red-400 px-2.5 py-1 rounded-md text-xs font-bold ring-1 ring-inset ring-red-600/10 animate-pulse">
                                            <span>⚠️ 缺货</span> {{ p.stock }}
                                        </div>
                                        <span v-else
                                            class="text-green-600 dark:text-green-400 font-bold bg-green-50 dark:bg-green-900/30 px-2.5 py-1 rounded-md text-xs ring-1 ring-inset ring-green-600/20">充足
                                            {{ p.stock }}</span>
                                    </td>
                                    <td class="p-5 max-w-xs truncate text-slate-500">{{ p.description }}</td>
                                    <td class="p-5 text-center space-x-3">
                                        <button @click="openProductModal(p)"
                                            class="text-blue-600 dark:text-blue-400 hover:underline font-medium">编辑</button>
                                        <button @click="handleDeleteProduct(p.id)"
                                            class="text-red-500 hover:underline font-medium">下架</button>
                                    </td>
                                </tr>
                            </template>
                        </tbody>
                    </table>
                </div>
            </div>

            <div v-else-if="currentTab === 'users'" class="animate-fade-in-up">
                <div class="flex justify-between items-center mb-6">
                    <button @click="fetchUsers"
                        class="text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-slate-800 px-4 py-2 rounded-lg font-medium transition-colors">↻
                        刷新列表</button>
                </div>
                <div
                    class="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 overflow-hidden">
                    <table class="w-full text-left text-sm text-slate-600 dark:text-slate-300">
                        <thead
                            class="bg-slate-50 dark:bg-slate-800/50 text-slate-500 dark:text-slate-400 font-bold border-b border-slate-100 dark:border-slate-700">
                            <tr>
                                <th class="p-5">ID</th>
                                <th class="p-5">用户</th>
                                <th class="p-5">角色</th>
                                <th class="p-5">积分</th>
                                <th class="p-5 text-center">操作</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                            <tr v-for="u in users" :key="u.id" class="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                                <td class="p-5 font-mono text-slate-400">#{{ u.id }}</td>
                                <td class="p-5 flex items-center gap-3">
                                    <div
                                        class="w-9 h-9 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center font-bold text-slate-600 dark:text-slate-300 shadow-inner">
                                        {{ u.username.charAt(0).toUpperCase() }}</div>
                                    <span class="font-bold text-slate-700 dark:text-white">{{ u.username }}</span>
                                </td>
                                <td class="p-5"><span
                                        :class="u.role === 'ADMIN' ? 'bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300' : 'bg-slate-100 dark:bg-slate-800'"
                                        class="px-2.5 py-1 rounded-md text-xs font-bold">{{ u.role }}</span></td>
                                <td class="p-5 font-mono text-amber-600 font-bold">{{ u.points || 0 }}</td>
                                <td class="p-5 text-center space-x-3">
                                    <button @click="openPointModal(u)"
                                        class="text-blue-600 dark:text-blue-400 font-medium hover:underline">修改积分</button>
                                    <button v-if="u.role !== 'ADMIN'" @click="handleDeleteUser(u.id)"
                                        class="text-red-500 font-medium hover:underline">删除</button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div v-if="showDetailModal"
                class="fixed inset-0 bg-slate-900/40 backdrop-blur-md z-50 flex items-center justify-center p-4">
                <div
                    class="bg-white/95 dark:bg-slate-900/95 backdrop-blur-xl border border-white/20 dark:border-slate-700 rounded-2xl w-full max-w-2xl shadow-2xl p-6 transform transition-all scale-100">
                    <div
                        class="flex justify-between items-center mb-6 border-b border-slate-100 dark:border-slate-800 pb-4">
                        <h3 class="text-xl font-bold text-slate-800 dark:text-white">订单详情 <span
                                class="ml-2 px-2 py-1 bg-slate-100 dark:bg-slate-800 rounded text-sm font-mono text-slate-500">#{{
                                    currentOrderDetails.id }}</span></h3>
                        <button @click="showDetailModal = false"
                            class="text-slate-400 hover:text-slate-600 dark:hover:text-white text-2xl transition-colors">×</button>
                    </div>
                    <div class="space-y-6">
                        <div
                            class="bg-slate-50 dark:bg-slate-800 p-5 rounded-xl border border-slate-100 dark:border-slate-700">
                            <h4 class="font-bold text-slate-700 dark:text-slate-200 mb-3 flex items-center gap-2">📍
                                收货信息</h4>
                            <div class="text-sm text-slate-600 dark:text-slate-400 space-y-2">
                                <p class="flex"><span class="w-16 opacity-50">联系人：</span><span
                                        class="font-medium text-slate-800 dark:text-slate-200">{{
                                            currentOrderDetails.receiverName
                                        }}</span></p>
                                <p class="flex"><span class="w-16 opacity-50">电话：</span><span class="font-mono">{{
                                    currentOrderDetails.receiverPhone }}</span></p>
                                <p class="flex"><span class="w-16 opacity-50">地址：</span><span>{{
                                    currentOrderDetails.receiverAddress
                                }}</span></p>
                            </div>
                        </div>
                        <div>
                            <h4 class="font-bold text-slate-700 dark:text-slate-200 mb-3">📦 商品清单</h4>
                            <div
                                class="divide-y divide-slate-100 dark:divide-slate-800 border border-slate-100 dark:border-slate-700 rounded-xl overflow-hidden">
                                <div v-for="item in currentOrderDetails.items" :key="item.id"
                                    class="p-4 flex items-center bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
                                    <img :src="item.imageUrl"
                                        class="w-12 h-12 rounded-lg border dark:border-slate-700 bg-white object-cover mr-4 shadow-sm">
                                    <div class="flex-1">
                                        <div class="font-bold text-slate-800 dark:text-white">{{ item.productName }}
                                        </div>
                                        <div class="text-xs text-slate-400 mt-0.5">单价: ¥{{ item.price }}</div>
                                    </div>
                                    <div class="font-mono font-bold text-slate-400 dark:text-slate-500 mr-6">x{{
                                        item.quantity }}
                                    </div>
                                    <div class="font-mono font-bold text-slate-800 dark:text-slate-200">¥{{ (item.price
                                        *
                                        item.quantity).toFixed(2) }}</div>
                                </div>
                            </div>
                            <div class="mt-4 flex justify-end items-center gap-3">
                                <span class="text-slate-500 text-sm">实付金额</span>
                                <span class="text-2xl font-bold text-blue-600 dark:text-blue-400 font-serif-sc">¥{{
                                    currentOrderDetails.totalPrice }}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div v-if="showProductModal"
                class="fixed inset-0 bg-slate-900/40 backdrop-blur-md z-50 flex items-center justify-center p-4">
                <div
                    class="bg-white/95 dark:bg-slate-900/95 backdrop-blur-xl border border-white/20 dark:border-slate-700 rounded-2xl w-full max-w-lg shadow-2xl p-6">
                    <h3 class="text-lg font-bold text-slate-800 dark:text-white mb-4">{{ editingProduct.id ? '编辑' : '新增'
                    }}商品</h3>
                    <div class="space-y-4">
                        <input v-model="editingProduct.name" type="text"
                            class="w-full bg-transparent border border-slate-300 dark:border-slate-600 rounded-lg p-2 dark:text-white focus:ring-2 focus:ring-blue-500 outline-none"
                            placeholder="商品名称">
                        <div class="grid grid-cols-2 gap-4">
                            <input v-model="editingProduct.price" type="number"
                                class="w-full bg-transparent border border-slate-300 dark:border-slate-600 rounded-lg p-2 dark:text-white focus:ring-2 focus:ring-blue-500 outline-none"
                                placeholder="价格">
                            <input v-model="editingProduct.stock" type="number"
                                class="w-full bg-transparent border border-slate-300 dark:border-slate-600 rounded-lg p-2 dark:text-white focus:ring-2 focus:ring-blue-500 outline-none"
                                placeholder="库存">
                        </div>
                        <input v-model="editingProduct.imageUrl" type="text"
                            class="w-full bg-transparent border border-slate-300 dark:border-slate-600 rounded-lg p-2 dark:text-white text-xs focus:ring-2 focus:ring-blue-500 outline-none"
                            placeholder="图片链接">
                        <textarea v-model="editingProduct.description" rows="3"
                            class="w-full bg-transparent border border-slate-300 dark:border-slate-600 rounded-lg p-2 dark:text-white focus:ring-2 focus:ring-blue-500 outline-none"
                            placeholder="描述"></textarea>
                    </div>
                    <div class="mt-6 flex justify-end gap-3">
                        <button @click="showProductModal = false"
                            class="px-4 py-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors">取消</button>
                        <button @click="saveProduct"
                            class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 shadow-lg shadow-blue-200 dark:shadow-none transition-all">保存</button>
                    </div>
                </div>
            </div>

            <div v-if="showPointModal"
                class="fixed inset-0 bg-slate-900/40 backdrop-blur-md z-50 flex items-center justify-center p-4">
                <div
                    class="bg-white/95 dark:bg-slate-900/95 backdrop-blur-xl border border-white/20 dark:border-slate-700 rounded-2xl w-full max-w-sm shadow-2xl p-6">
                    <h3 class="text-lg font-bold text-slate-800 dark:text-white mb-4">修改积分</h3>
                    <input v-model="editingUser.points" type="number"
                        class="w-full bg-transparent border border-slate-300 dark:border-slate-600 rounded-lg p-2 dark:text-white text-lg font-mono focus:ring-2 focus:ring-blue-500 outline-none text-center">
                    <div class="mt-6 flex justify-end gap-3">
                        <button @click="showPointModal = false"
                            class="px-4 py-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors">取消</button>
                        <button @click="saveUserPoints"
                            class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 shadow-lg shadow-blue-200 dark:shadow-none transition-all">确认</button>
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

.list-enter-active,
.list-leave-active {
    transition: all 0.4s ease;
}

.list-enter-from,
.list-leave-to {
    opacity: 0;
    transform: translateX(30px);
}
</style>