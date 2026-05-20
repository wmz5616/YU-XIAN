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

const stockOverview = ref({ categories: [], totalProducts: 0, totalStock: 0, lowStockCount: 0, outOfStockCount: 0, pendingReminders: 0 });
const expandedCategories = ref(new Set());
const restockReminders = ref([]);

const users = ref([]);
const showPointModal = ref(false);
const editingUser = ref({});

const Toast = Swal.mixin({
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 3000,
    timerProgressBar: true,
    customClass: {
        container: 'admin-toast-container'
    },
    didOpen: (toast) => {
        toast.addEventListener('mouseenter', Swal.stopTimer);
        toast.addEventListener('mouseleave', Swal.resumeTimer);
    }
});

const currentPageTitle = computed(() => {
    const map = {
        'dashboard': '运营概况',
        'orders': '订单管理',
        'products': '商品库管理',
        'users': '会员管理',
        'refund': '售后处理中心',
        'inventory': '库存管理'
    }
    return map[currentTab.value] || '控制台'
})

watch(() => route.path, () => { isSidebarOpen.value = false; });

let socket = null;
const playNotificationSound = () => {
    try {
        const audio = new Audio('/audio/ding.mp3');
        audio.play().catch(e => {
            console.warn('Play audio failed due to browser policies:', e);
        });
    } catch (e) {
        console.error('Audio play error:', e);
    }
};

const initWebSocket = () => {
    if (typeof (WebSocket) === "undefined") return;
    const token = localStorage.getItem('yuxian_token') || '';
    const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    const host = apiBase.replace(/^https?:\/\//, '');
    const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
    const wsUrl = `${protocol}${host}/ws/orders?token=${token}`;

    socket = new WebSocket(wsUrl);

    socket.onopen = () => {
        console.log("WebSocket已连接 (身份鉴权中...)");
    };

    socket.onmessage = (msg) => {
        if (msg.data === 'NEW_ORDER') {
            playNotificationSound();
            Toast.fire({ icon: 'info', title: '🔔 收到新订单！', text: '列表已自动刷新' });
            if (currentTab.value === 'dashboard' || currentTab.value === 'orders') {
                fetchStats();
                fetchOrders(false);
            }
        }
        if (msg.data === 'NEW_REFUND') {
            playNotificationSound();
            Toast.fire({ icon: 'warning', title: '📝 收到新的售后申请！', text: '请及时处理' });
            if (currentTab.value === 'refund') {
                fetchRefunds();
            }
        }
        if (msg.data === 'RESTOCK_REMIND') {
            playNotificationSound();
            Toast.fire({ icon: 'info', title: '🔔 收到新的补货提醒！', text: '用户反馈库存不足' });
            if (currentTab.value === 'inventory') {
                fetchStockOverview();
                fetchRestockReminders();
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
const openProductModal = (p) => { editingProduct.value = p ? { ...p } : { name: '', price: 0, stock: 100, category: '鱼类', origin: '', imageUrl: '', description: '' }; showProductModal.value = true; };
const saveProduct = async () => {
    try {
        if (editingProduct.value.id) await request.put(`/api/products/${editingProduct.value.id}`, editingProduct.value);
        else await request.post('/api/products', editingProduct.value);
        showProductModal.value = false; fetchProducts(); Toast.fire('保存成功', '', 'success');
    } catch (e) { Swal.fire('Error', e.message, 'error'); }
};
const handleDeleteProduct = async (id) => {
    const result = await Swal.fire({
        title: '确认下架该商品？',
        text: '下架后将从数据库中永久删除，无法恢复',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#ef4444',
        confirmButtonText: '确认下架',
        cancelButtonText: '取消'
    });
    if (result.isConfirmed) {
        try {
            await request.delete(`/api/products/${id}`);
            fetchProducts();
            Toast.fire('已下架', '', 'success');
        } catch (e) {
            Swal.fire('下架失败', e.message || '系统繁忙', 'error');
        }
    }
};

const toggleCategory = (catName) => {
    if (expandedCategories.value.has(catName)) {
        expandedCategories.value.delete(catName);
    } else {
        expandedCategories.value.add(catName);
    }
};

const fetchStockOverview = async () => {
    try {
        const res = await request.get('/api/products/stock-overview');
        stockOverview.value = res;
    } catch (e) {
        console.error('库存总览加载失败', e);
    }
};

const fetchRestockReminders = async () => {
    try {
        const res = await request.get('/api/products/admin/restock-reminders');
        restockReminders.value = res || [];
    } catch (e) {
        console.error('补货提醒加载失败', e);
    }
};

const handleResolveReminder = async (productId) => {
    const confirm = await Swal.fire({
        title: '确认已补货？',
        text: '该商品的所有补货提醒将被标记为已处理',
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#10b981',
        confirmButtonText: '确认已补货'
    });
    if (!confirm.isConfirmed) return;
    try {
        await request.post(`/api/products/admin/restock-reminders/${productId}/resolve`);
        Toast.fire('已标记为已补货', '', 'success');
        fetchRestockReminders();
        fetchStockOverview();
    } catch (e) {
        Swal.fire('操作失败', e.message || '系统繁忙', 'error');
    }
};

const handleActiveRestock = async (p) => {
    const { value: amountStr } = await Swal.fire({
        title: `为【${p.name}】补货`,
        input: 'number',
        inputLabel: '请输入新增的补货数量',
        inputPlaceholder: '例如：50',
        inputValue: 50,
        showCancelButton: true,
        confirmButtonColor: '#3b82f6',
        confirmButtonText: '确定入库',
        inputValidator: (value) => {
            if (!value || isNaN(value) || parseInt(value) <= 0) {
                return '请输入有效的大于0的数量！';
            }
        }
    });

    if (amountStr) {
        const addAmount = parseInt(amountStr);
        try {
            await request.put(`/api/products/${p.id}`, { stock: p.stock + addAmount });
            
            if (p.reminderCount > 0) {
                await request.post(`/api/products/admin/restock-reminders/${p.id}/resolve`).catch(() => {});
            }

            Toast.fire('补货成功', `已增加 ${addAmount} 件库存`, 'success');
            fetchStockOverview();
            fetchRestockReminders();
        } catch (e) {
            Swal.fire('操作失败', e.message || '系统繁忙', 'error');
        }
    }
};

const getStockBarColor = (status) => {
    if (status === 'OUT') return 'bg-red-500';
    if (status === 'LOW') return 'bg-amber-500';
    return 'bg-emerald-500';
};

const getStockBadge = (status) => {
    if (status === 'OUT') return { text: '缺货', cls: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300' };
    if (status === 'LOW') return { text: '低库存', cls: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300' };
    return { text: '充足', cls: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300' };
};

const getCategoryIcon = (name) => {
    const icons = {
        '鱼类': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="w-7 h-7 text-blue-500"><path d="M6.5 12c3-7 11-7 14-2-3 5-11 5-14 2z"/><circle cx="17" cy="11.5" r="1" fill="currentColor" stroke="none"/><path d="M2 10l3 2-3 2"/><path d="M10.5 10c1 .5 1.5 1.5 1.5 2.5"/></svg>',
        '虾类': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="w-7 h-7 text-orange-500"><path d="M17 4c2 0 4 2 4 4s-2 4-4 4H9"/><path d="M9 12c-2 0-5 2-5 5s2 3 4 3c1 0 2-.5 2.5-1.5"/><path d="M14 12c0 3-1 5-3.5 6.5"/><circle cx="19" cy="7" r="1" fill="currentColor" stroke="none"/><path d="M17 4l2-2M18 5l2.5-1"/></svg>',
        '蟹类': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="w-7 h-7 text-red-500"><ellipse cx="12" cy="14" rx="6" ry="4"/><path d="M6 14c-2-1-4-3-4-5M18 14c2-1 4-3 4-5"/><path d="M2 9l1.5 1M22 9l-1.5 1"/><path d="M8 10c0-2 1.5-4 4-4s4 2 4 4"/><circle cx="10" cy="13" r="0.5" fill="currentColor" stroke="none"/><circle cx="14" cy="13" r="0.5" fill="currentColor" stroke="none"/></svg>',
        '贝类': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="w-7 h-7 text-teal-500"><path d="M12 3C7 3 3 8 3 13c0 3 2 5 4.5 6h9c2.5-1 4.5-3 4.5-6 0-5-4-10-9-10z"/><path d="M12 3v16M8 6c-1 2-2 5-2 8M16 6c1 2 2 5 2 8"/></svg>',
        '头足类': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="w-7 h-7 text-purple-500"><ellipse cx="12" cy="8" rx="5" ry="5"/><path d="M7 12c-1 3 0 6 1 9M10 12c0 3-.5 6-1.5 9M14 12c0 3 .5 6 1.5 9M17 12c1 3 0 6-1 9"/><circle cx="10" cy="7" r="1" fill="currentColor" stroke="none"/><circle cx="14" cy="7" r="1" fill="currentColor" stroke="none"/></svg>',
    };
    return icons[name] || '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="w-7 h-7 text-slate-400"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/><path d="M3.27 6.96L12 12.01l8.73-5.05M12 22.08V12"/></svg>';
};

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
    else if (tab === 'inventory') { fetchStockOverview(); fetchRestockReminders(); }
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

                <a @click="switchTab('inventory')"
                    :class="['flex items-center space-x-3 px-4 py-3 rounded-xl cursor-pointer transition-all duration-300 font-medium',
                        currentTab === 'inventory'
                            ? 'bg-blue-50 text-blue-600 dark:bg-blue-600 dark:text-white shadow-sm'
                            : 'text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-slate-200']">
                    <span></span><span>库存管理</span>
                    <span v-if="stockOverview.lowStockCount + stockOverview.outOfStockCount > 0" class="ml-auto bg-red-500 text-white text-[10px] w-5 h-5 rounded-full flex items-center justify-center font-bold">{{ stockOverview.lowStockCount + stockOverview.outOfStockCount }}</span>
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
                    <div v-for="(item, idx) in [
                        { label: '总销售额', val: '¥' + stats.totalSales.toLocaleString(), svg: '<svg viewBox=&quot;0 0 24 24&quot; fill=&quot;none&quot; stroke=&quot;currentColor&quot; stroke-width=&quot;1.5&quot; stroke-linecap=&quot;round&quot; stroke-linejoin=&quot;round&quot; class=&quot;w-6 h-6&quot;><line x1=&quot;12&quot; y1=&quot;1&quot; x2=&quot;12&quot; y2=&quot;23&quot;/><path d=&quot;M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6&quot;/></svg>', color: 'text-emerald-500 bg-emerald-50 dark:bg-emerald-900/20' },
                        { label: '待发货订单', val: stats.pendingOrders, svg: '<svg viewBox=&quot;0 0 24 24&quot; fill=&quot;none&quot; stroke=&quot;currentColor&quot; stroke-width=&quot;1.5&quot; stroke-linecap=&quot;round&quot; stroke-linejoin=&quot;round&quot; class=&quot;w-6 h-6&quot;><path d=&quot;M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9&quot;/><path d=&quot;M13.73 21a2 2 0 01-3.46 0&quot;/></svg>', color: 'text-amber-500 bg-amber-50 dark:bg-amber-900/20' },
                        { label: '注册用户', val: stats.totalUsers, svg: '<svg viewBox=&quot;0 0 24 24&quot; fill=&quot;none&quot; stroke=&quot;currentColor&quot; stroke-width=&quot;1.5&quot; stroke-linecap=&quot;round&quot; stroke-linejoin=&quot;round&quot; class=&quot;w-6 h-6&quot;><path d=&quot;M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2&quot;/><circle cx=&quot;9&quot; cy=&quot;7&quot; r=&quot;4&quot;/><path d=&quot;M23 21v-2a4 4 0 00-3-3.87&quot;/><path d=&quot;M16 3.13a4 4 0 010 7.75&quot;/></svg>', color: 'text-blue-500 bg-blue-50 dark:bg-blue-900/20' },
                        { label: '库存商品', val: stats.totalProducts, svg: '<svg viewBox=&quot;0 0 24 24&quot; fill=&quot;none&quot; stroke=&quot;currentColor&quot; stroke-width=&quot;1.5&quot; stroke-linecap=&quot;round&quot; stroke-linejoin=&quot;round&quot; class=&quot;w-6 h-6&quot;><path d=&quot;M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z&quot;/><path d=&quot;M3.27 6.96L12 12.01l8.73-5.05M12 22.08V12&quot;/></svg>', color: 'text-purple-500 bg-purple-50 dark:bg-purple-900/20' }
                    ]"
                        :key="idx"
                        class="bg-white dark:bg-slate-900 p-6 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 hover:shadow-md transition-shadow">
                        <div class="flex justify-between items-start">
                            <div>
                                <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">{{ item.label }}
                                </p>
                                <h3 class="text-2xl font-black text-slate-800 dark:text-white mt-2">{{ item.val }}</h3>
                            </div>
                            <span :class="['p-3 rounded-xl', item.color]" v-html="item.svg"></span>
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
                    <div class="overflow-x-auto">
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

            <div v-else-if="currentTab === 'inventory'" class="animate-fade-in-up space-y-6">
                <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
                    <div class="bg-white dark:bg-slate-900 p-6 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800">
                        <div class="flex justify-between items-start">
                            <div>
                                <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">总库存量</p>
                                <h3 class="text-2xl font-black text-slate-800 dark:text-white mt-2">{{ stockOverview.totalStock.toLocaleString() }}</h3>
                            </div>
                            <span class="p-3 rounded-xl bg-blue-50 dark:bg-blue-900/20"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="w-6 h-6 text-blue-500"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/><path d="M3.27 6.96L12 12.01l8.73-5.05M12 22.08V12"/></svg></span>
                        </div>
                        <p class="text-xs text-slate-400 mt-2">共 {{ stockOverview.totalProducts }} 种商品</p>
                    </div>
                    <div class="bg-white dark:bg-slate-900 p-6 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800">
                        <div class="flex justify-between items-start">
                            <div>
                                <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">低库存预警</p>
                                <h3 class="text-2xl font-black text-amber-500 mt-2">{{ stockOverview.lowStockCount }}</h3>
                            </div>
                            <span class="p-3 rounded-xl bg-amber-50 dark:bg-amber-900/20"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="w-6 h-6 text-amber-500"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg></span>
                        </div>
                        <p class="text-xs text-slate-400 mt-2">库存低于 20 件</p>
                    </div>
                    <div class="bg-white dark:bg-slate-900 p-6 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800">
                        <div class="flex justify-between items-start">
                            <div>
                                <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">缺货商品</p>
                                <h3 class="text-2xl font-black text-red-500 mt-2">{{ stockOverview.outOfStockCount }}</h3>
                            </div>
                            <span class="p-3 rounded-xl bg-red-50 dark:bg-red-900/20"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="w-6 h-6 text-red-500"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg></span>
                        </div>
                        <p class="text-xs text-slate-400 mt-2">库存为 0，急需补货</p>
                    </div>
                    <div class="bg-white dark:bg-slate-900 p-6 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800">
                        <div class="flex justify-between items-start">
                            <div>
                                <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">补货提醒</p>
                                <h3 class="text-2xl font-black text-purple-500 mt-2">{{ stockOverview.pendingReminders }}</h3>
                            </div>
                            <span class="p-3 rounded-xl bg-purple-50 dark:bg-purple-900/20"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="w-6 h-6 text-purple-500"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg></span>
                        </div>
                        <p class="text-xs text-slate-400 mt-2">来自用户的待处理提醒</p>
                    </div>
                </div>

                <div class="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 overflow-hidden">
                    <div class="p-5 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center">
                        <h2 class="font-bold text-slate-800 dark:text-white flex items-center gap-2">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="w-5 h-5 text-blue-500"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg> 品类库存总览
                        </h2>
                        <button @click="expandedCategories.size > 0 ? expandedCategories.clear() : stockOverview.categories.forEach(c => expandedCategories.add(c.name))"
                            class="text-xs text-blue-600 hover:underline font-bold">
                            {{ expandedCategories.size > 0 ? '全部收起' : '全部展开' }}
                        </button>
                    </div>

                    <div class="divide-y divide-slate-100 dark:divide-slate-800">
                        <div v-for="cat in stockOverview.categories" :key="cat.name">
                            <div @click="toggleCategory(cat.name)"
                                class="flex items-center justify-between p-5 cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors">
                                <div class="flex items-center gap-4">
                                    <span class="w-10 h-10 rounded-xl bg-slate-50 dark:bg-slate-800 flex items-center justify-center flex-shrink-0" v-html="getCategoryIcon(cat.name)"></span>
                                    <div>
                                        <h3 class="font-bold text-slate-800 dark:text-white text-lg">{{ cat.name }}</h3>
                                        <p class="text-xs text-slate-400 mt-0.5">{{ cat.productCount }} 种商品 · 总库存 {{ cat.totalStock }} 件</p>
                                    </div>
                                </div>
                                <div class="flex items-center gap-3">
                                    <span v-if="cat.outOfStockCount > 0" class="bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300 px-2 py-0.5 rounded-full text-[10px] font-bold">
                                        {{ cat.outOfStockCount }} 缺货
                                    </span>
                                    <span v-if="cat.lowStockCount > 0" class="bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300 px-2 py-0.5 rounded-full text-[10px] font-bold">
                                        {{ cat.lowStockCount }} 低库存
                                    </span>
                                    <div class="w-32 h-2 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden hidden sm:block">
                                        <div class="h-full rounded-full transition-all duration-700"
                                            :class="cat.outOfStockCount > 0 ? 'bg-red-500' : cat.lowStockCount > 0 ? 'bg-amber-500' : 'bg-emerald-500'"
                                            :style="`width: ${Math.min((cat.totalStock / (cat.productCount * 200)) * 100, 100)}%`"></div>
                                    </div>
                                    <svg class="w-5 h-5 text-slate-400 transition-transform duration-300"
                                        :class="expandedCategories.has(cat.name) ? 'rotate-180' : ''"
                                        fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                                    </svg>
                                </div>
                            </div>

                            <transition name="expand">
                                <div v-if="expandedCategories.has(cat.name)" class="bg-slate-50/50 dark:bg-slate-800/20">
                                    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3 p-4">
                                        <div v-for="p in cat.products" :key="p.id"
                                            class="bg-white dark:bg-slate-900 rounded-xl p-4 border border-slate-100 dark:border-slate-800 hover:shadow-md transition-all duration-300 group">
                                            <div class="flex items-center gap-3 mb-3">
                                                <img :src="p.imageUrl" class="w-12 h-12 rounded-lg object-cover border border-slate-100 dark:border-slate-700 flex-shrink-0" />
                                                <div class="flex-1 min-w-0">
                                                    <h4 class="font-bold text-sm text-slate-800 dark:text-white truncate">{{ p.name }}</h4>
                                                    <p class="text-[10px] text-slate-400 truncate">{{ p.origin }}</p>
                                                </div>
                                                <span :class="['px-2 py-0.5 rounded-full text-[10px] font-bold', getStockBadge(p.stockStatus).cls]">
                                                    {{ getStockBadge(p.stockStatus).text }}
                                                </span>
                                            </div>
                                            <div class="flex items-center gap-2 mb-2">
                                                <div class="flex-1 h-2 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                                                    <div class="h-full rounded-full transition-all duration-700"
                                                        :class="getStockBarColor(p.stockStatus)"
                                                        :style="`width: ${Math.min((p.stock / 200) * 100, 100)}%`"></div>
                                                </div>
                                                <span class="text-xs font-mono font-bold min-w-[40px] text-right"
                                                    :class="p.stockStatus === 'OUT' ? 'text-red-500' : p.stockStatus === 'LOW' ? 'text-amber-500' : 'text-slate-600 dark:text-slate-300'">
                                                    {{ p.stock }}
                                                </span>
                                            </div>
                                            <div class="flex items-center justify-between mt-1">
                                                <div class="flex flex-col">
                                                    <span class="text-xs text-orange-500 font-mono font-bold">¥{{ p.price }}</span>
                                                    <div v-if="p.reminderCount > 0" class="flex items-center gap-1 text-[10px] text-purple-500 font-bold mt-1">
                                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="w-3 h-3 text-purple-500"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>
                                                        <span>{{ p.reminderCount }} 人催补</span>
                                                    </div>
                                                </div>
                                                <button @click.stop="handleActiveRestock(p)" class="flex items-center gap-1 px-2.5 py-1 text-[10px] font-bold text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-md transition-colors dark:bg-blue-900/30 dark:text-blue-400 dark:hover:bg-blue-900/50">
                                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-3 h-3"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                                                    补货
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </transition>
                        </div>
                    </div>
                </div>

                <div v-if="restockReminders.length > 0"
                    class="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 overflow-hidden">
                    <div class="p-5 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center">
                        <h2 class="font-bold text-slate-800 dark:text-white flex items-center gap-2">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="w-5 h-5 text-purple-500"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg> 用户补货提醒
                            <span class="bg-purple-100 text-purple-700 text-[10px] px-2 py-0.5 rounded-full font-bold">
                                {{ restockReminders.length }} 个商品待处理
                            </span>
                        </h2>
                    </div>
                    <div class="divide-y divide-slate-100 dark:divide-slate-800">
                        <div v-for="item in restockReminders" :key="item.productId"
                            class="p-5 flex flex-col sm:flex-row items-start sm:items-center gap-4 hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors">
                            <div class="flex items-center gap-3 flex-1 min-w-0">
                                <img :src="item.imageUrl" class="w-14 h-14 rounded-xl object-cover border border-slate-100 dark:border-slate-700 flex-shrink-0" />
                                <div class="flex-1 min-w-0">
                                    <div class="flex items-center gap-2 mb-1 flex-wrap">
                                        <h4 class="font-bold text-slate-800 dark:text-white truncate">{{ item.productName }}</h4>
                                        <span class="bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 text-[10px] px-2 py-0.5 rounded">{{ item.category }}</span>
                                    </div>
                                    <div class="flex items-center gap-3 text-xs text-slate-400">
                                        <span>当前库存: <b class="text-red-500">{{ item.currentStock }}</b></span>
                                        <span>·</span>
                                        <span class="text-purple-500 font-bold">{{ item.reminderCount }} 位用户催补</span>
                                    </div>
                                    <div class="flex flex-wrap gap-1 mt-2">
                                        <span v-for="r in item.reminders.slice(0, 5)" :key="r.id"
                                            class="bg-slate-100 dark:bg-slate-800 text-[10px] px-2 py-0.5 rounded-full text-slate-500">
                                            {{ r.username }} · {{ new Date(r.createTime).toLocaleDateString() }}
                                        </span>
                                        <span v-if="item.reminders.length > 5"
                                            class="bg-slate-100 dark:bg-slate-800 text-[10px] px-2 py-0.5 rounded-full text-slate-500">
                                            +{{ item.reminders.length - 5 }} 更多
                                        </span>
                                    </div>
                                </div>
                            </div>
                            <div class="flex gap-2 flex-shrink-0">
                                <button @click="openProductModal(products.find(p => p.id === item.productId) || { id: item.productId, name: item.productName, stock: item.currentStock })"
                                    class="px-4 py-2 text-xs font-bold border border-blue-200 text-blue-600 rounded-lg hover:bg-blue-50 dark:border-blue-800 dark:hover:bg-blue-900/20 transition">
                                    修改库存
                                </button>
                                <button @click="handleResolveReminder(item.productId)"
                                    class="px-4 py-2 text-xs font-bold bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 shadow-sm transition">
                                    已补货 ✓
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <div v-if="restockReminders.length === 0 && stockOverview.categories.length > 0"
                    class="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-100 dark:border-slate-800 p-12 text-center">
                    <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-emerald-50 dark:bg-emerald-900/20 flex items-center justify-center"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="w-8 h-8 text-emerald-500"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg></div>
                    <h3 class="text-lg font-bold text-slate-600 dark:text-slate-300 mb-1">暂无补货提醒</h3>
                    <p class="text-sm text-slate-400">当用户发现库存不足时会向您发送提醒</p>
                </div>
            </div>

            <Teleport to="body">
                <div v-if="showProductModal"
                    class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
                    <div class="bg-white dark:bg-slate-900 p-6 rounded-2xl w-full max-w-lg shadow-2xl animate-scale-up border border-slate-100 dark:border-slate-800">
                        <h3 class="font-bold mb-4 text-lg dark:text-white">{{ editingProduct.id ? '编辑' : '新增' }}商品</h3>
                        <div class="space-y-4">
                            <input v-model="editingProduct.name"
                                class="w-full p-3 border rounded-xl dark:bg-slate-800 dark:border-slate-700 dark:text-white"
                                placeholder="商品名称">
                            <div class="grid grid-cols-2 gap-4">
                                <select v-model="editingProduct.category"
                                    class="p-3 border rounded-xl dark:bg-slate-800 dark:border-slate-700 dark:text-white appearance-none">
                                    <option value="鱼类">鱼类</option>
                                    <option value="虾类">虾类</option>
                                    <option value="蟹类">蟹类</option>
                                    <option value="贝类">贝类</option>
                                    <option value="头足类">头足类</option>
                                </select>
                                <input v-model="editingProduct.origin"
                                    class="p-3 border rounded-xl dark:bg-slate-800 dark:border-slate-700 dark:text-white"
                                    placeholder="产地（如:舟山/进口）">
                            </div>
                            <div class="grid grid-cols-2 gap-4">
                                <input v-model="editingProduct.price" type="number" step="0.01"
                                    class="p-3 border rounded-xl dark:bg-slate-800 dark:border-slate-700 dark:text-white"
                                    placeholder="价格">
                                <input v-model="editingProduct.stock" type="number"
                                    class="p-3 border rounded-xl dark:bg-slate-800 dark:border-slate-700 dark:text-white"
                                    placeholder="库存">
                            </div>
                            <input v-model="editingProduct.imageUrl"
                                class="w-full p-3 border rounded-xl dark:bg-slate-800 dark:border-slate-700 dark:text-white"
                                placeholder="图片URL（如: /images/xxx.jpg 或 https://...)"
                            >
                            <div v-if="editingProduct.imageUrl" class="flex items-center gap-3">
                                <img :src="editingProduct.imageUrl" class="w-16 h-16 rounded-lg object-cover border dark:border-slate-700" @error="$event.target.style.display='none'">
                                <span class="text-xs text-slate-400">图片预览</span>
                            </div>
                            <textarea v-model="editingProduct.description" rows="3"
                                class="w-full p-3 border rounded-xl dark:bg-slate-800 dark:border-slate-700 dark:text-white resize-none"
                                placeholder="商品描述"></textarea>
                        </div>
                        <div class="flex justify-end gap-3 mt-6">
                            <button @click="showProductModal = false"
                                class="px-4 py-2 text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg">取消</button>
                            <button @click="saveProduct"
                                class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">保存</button>
                        </div>
                    </div>
                </div>
            </Teleport>

            <Teleport to="body">
                <div v-if="showPointModal"
                    class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
                    <div class="bg-white dark:bg-slate-900 p-6 rounded-2xl w-full max-w-sm shadow-2xl animate-scale-up border border-slate-100 dark:border-slate-800">
                        <h3 class="font-bold mb-4 dark:text-white">修改积分</h3>
                        <input v-model="editingUser.points" type="number"
                            class="w-full p-3 border rounded-xl mb-4 dark:bg-slate-800 dark:border-slate-700 dark:text-white">
                        <div class="flex justify-end gap-3">
                            <button @click="showPointModal = false" class="px-4 py-2 text-slate-500">取消</button>
                            <button @click="saveUserPoints" class="px-4 py-2 bg-blue-600 text-white rounded-lg">保存</button>
                        </div>
                    </div>
                </div>
            </Teleport>

            <Teleport to="body">
                <div v-if="showDetailModal"
                    class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
                    <div class="bg-white dark:bg-slate-900 p-6 rounded-2xl w-full max-w-2xl shadow-2xl animate-scale-up border border-slate-100 dark:border-slate-800">
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
            </Teleport>

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

.expand-enter-active,
.expand-leave-active {
    transition: all 0.3s ease;
    overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
    opacity: 0;
    max-height: 0;
}

.expand-enter-to,
.expand-leave-from {
    opacity: 1;
    max-height: 2000px;
}
</style>