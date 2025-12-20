<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '../store.js'
import { request } from '@/utils/request'
import Swal from 'sweetalert2'

const router = useRouter()
const loading = ref(false)
const isSigned = ref(false)

const pointLogs = ref(store.pointLogs || [])

const exchangeableCoupons = ref([
    { id: 101, amount: 5, cost: 500, name: '无门槛立减券', color: 'from-orange-400 to-red-500' },
    { id: 102, amount: 20, cost: 1800, name: '满200可用', color: 'from-blue-400 to-indigo-500' },
    { id: 103, amount: 50, cost: 4000, name: '海鲜盛宴专享', color: 'from-purple-400 to-pink-500' },
    { id: 104, amount: 100, cost: 8000, name: '至尊VIP礼券', color: 'from-slate-700 to-slate-900' },
])

onMounted(() => {
    const today = new Date().toLocaleDateString()
    const lastSign = localStorage.getItem(`sign_date_${store.currentUser?.username}`)
    if (lastSign === today) isSigned.value = true
})

const handleExchange = async (item) => {
    if (!store.currentUser) {
        Swal.fire('请先登录', '', 'warning');
        router.push('/login');
        return;
    }

    const currentPoints = store.currentUser.points || 0;
    if (currentPoints < item.cost) {
        Swal.fire('积分不足', `还差 ${item.cost - currentPoints} 积分`, 'error');
        return;
    }

    const confirm = await Swal.fire({
        title: '确认兑换?',
        text: `消耗 ${item.cost} 积分兑换 [${item.name}]`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#f59e0b',
        confirmButtonText: '确定'
    });

    if (confirm.isConfirmed) {
        loading.value = true;
        try {
            const res = await request.post('/api/coupons/exchange', {
                username: store.currentUser.username,
                amount: item.amount,
                cost: item.cost,
                name: item.name
            });

            if (res && res.success) {
                store.currentUser.points = res.points;
                store.login(store.currentUser);
                store.addPointLog({
                    type: 'expense',
                    title: `兑换: ${item.name}`,
                    amount: item.cost
                });
                store.addCoupon({
                    name: item.name,
                    amount: item.amount
                });

                Swal.fire('兑换成功', '优惠券已发放', 'success');
            }
        } catch (e) {
            Swal.fire('兑换失败', e.message || '系统繁忙', 'error');
        } finally {
            loading.value = false;
        }
    }
}

const handleSignIn = () => {
    if (!store.currentUser) return router.push('/login');

    isSigned.value = true;
    localStorage.setItem(`sign_date_${store.currentUser.username}`, new Date().toLocaleDateString());

    const reward = 10;
    store.currentUser.points = (store.currentUser.points || 0) + reward;
    store.login(store.currentUser);

    store.addPointLog({ type: 'income', title: '每日签到', amount: reward });
    Swal.fire('签到成功', `获得 ${reward} 积分`, 'success');
}
</script>

<template>
  <div class="min-h-screen bg-slate-50 font-sans relative overflow-hidden selection:bg-cyan-500 selection:text-white">

    <div class="fixed inset-0 pointer-events-none">
      <div class="absolute top-[-10%] right-[-5%] w-[50vw] h-[50vw] bg-blue-400/10 rounded-full blur-[100px] animate-blob"></div>
      <div class="absolute bottom-[-10%] left-[-10%] w-[50vw] h-[50vw] bg-cyan-400/10 rounded-full blur-[100px] animate-blob animation-delay-4000"></div>
    </div>

    <div class="relative max-w-5xl mx-auto px-6 py-10">
      
      <header class="flex justify-between items-center mb-10 animate-fade-down">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 bg-white rounded-2xl flex items-center justify-center shadow-lg shadow-blue-100 text-cyan-600 group cursor-pointer" @click="router.push('/profile')">
             <svg class="w-6 h-6 group-hover:scale-110 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path></svg>
          </div>
          <div>
            <h1 class="text-2xl font-black text-slate-800 tracking-tight">会员积分中心</h1>
            <p class="text-sm text-slate-500">Points & Rewards</p>
          </div>
        </div>
        <div class="px-4 py-2 bg-white/60 backdrop-blur-md rounded-full text-xs font-bold text-slate-500 border border-white/50 shadow-sm">
          今日汇率: 100 积分 = ¥1.00
        </div>
      </header>

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
        <div class="lg:col-span-8 space-y-8 animate-fade-in-up" style="animation-delay: 0.1s;">
          
          <div class="card-glass bg-aurora p-8 md:p-10 relative overflow-hidden group">
            <div class="absolute top-0 right-0 w-64 h-64 bg-cyan-400/20 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2 group-hover:bg-cyan-300/30 transition-colors duration-700"></div>
            
            <div class="relative z-10 flex flex-col md:flex-row justify-between items-center gap-8">
              <div class="text-center md:text-left">
                <div class="text-cyan-200 text-sm font-bold tracking-[0.2em] uppercase mb-2 opacity-80">My Assets</div>
                <div class="text-6xl md:text-7xl font-black text-white font-serif-sc tracking-tighter shimmer-text drop-shadow-2xl">
                  {{ store.currentUser?.points || 0 }}
                </div>
                <div class="text-white/60 text-xs mt-2 font-mono">当前可用积分余额</div>
              </div>

              <div class="relative group/sign perspective-500">
                <button 
                  @click="handleSignIn" 
                  :disabled="isSigned || loading"
                  class="crystal-ball relative w-36 h-36 rounded-full flex flex-col items-center justify-center transition-all duration-500 disabled:opacity-90 disabled:grayscale-[0.5] group-hover/sign:scale-105"
                >
                  <div class="absolute inset-0 rounded-full bg-cyan-400/20 blur-md animate-pulse"></div>
                  
                  <div class="relative z-20 flex flex-col items-center">
                     <template v-if="isSigned">
                        <svg class="w-14 h-14 text-white drop-shadow-[0_2px_4px_rgba(0,0,0,0.3)] filter" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path></svg>
                        <span class="text-[10px] text-white font-bold mt-1 tracking-wider">已签到</span>
                     </template>
                     <template v-else>
                        <span class="text-4xl mb-1 filter drop-shadow-lg transform group-hover/sign:-translate-y-1 transition-transform">📅</span>
                        <span class="text-sm font-bold text-white tracking-widest drop-shadow-md">签到</span>
                        <span class="text-[10px] text-cyan-100 mt-1 font-mono">+Points</span>
                     </template>
                  </div>
                  
                  <div class="absolute -inset-1 rounded-full border border-white/40 border-t-white border-l-transparent rotate-ring pointer-events-none"></div>
                </button>
                
                <div class="absolute -bottom-4 left-1/2 -translate-x-1/2 w-24 h-4 bg-black/20 blur-md rounded-[100%]"></div>
              </div>
            </div>
          </div>

          <div>
             <h3 class="text-xl font-black text-slate-800 mb-6 flex items-center gap-2">
               <span class="w-2 h-6 bg-cyan-500 rounded-full"></span>
               积分兑好礼
             </h3>
             
             <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <div v-for="item in exchangeableCoupons" :key="item.id" 
                  v-tilt="{ speed: 500, max: 10, glare: true, 'max-glare': 0.3 }"
                  class="group relative bg-white rounded-[24px] p-1 shadow-sm transition-all duration-300 border border-slate-100 overflow-hidden cursor-default"
                  :class="item.shadow"
                >
                  <div class="absolute inset-0 opacity-0 group-hover:opacity-10 transition-opacity duration-500 bg-gradient-to-br" :class="item.color"></div>

                  <div class="relative bg-white rounded-[20px] p-5 h-full flex flex-col z-10">
                    <div class="flex justify-between items-start mb-4">
                      <div class="w-12 h-12 rounded-2xl bg-gradient-to-br flex items-center justify-center text-white shadow-lg transform group-hover:scale-110 transition-transform duration-300" :class="item.color">
                        <span class="font-bold text-lg">¥</span>
                      </div>
                      <div class="text-right">
                         <div class="font-black text-2xl text-slate-800 font-serif-sc">¥{{ item.amount }}</div>
                         <div class="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Coupon</div>
                      </div>
                    </div>
                    
                    <h4 class="font-bold text-slate-700 text-lg mb-1">{{ item.name }}</h4>
                    <p class="text-xs text-slate-400 mb-6">满 ¥{{ item.amount * 10 }} 可用</p>

                    <div class="mt-auto flex items-center justify-between border-t border-slate-50 pt-4">
                       <div class="text-cyan-600 font-bold font-mono flex items-center gap-1">
                          <span class="text-lg">{{ item.cost }}</span>
                          <span class="text-[10px] text-slate-400 font-sans uppercase">Points</span>
                       </div>
                       <button @click.stop="handleExchange(item)" :disabled="loading || (store.currentUser?.points || 0) < item.cost"
                          class="px-5 py-2 bg-slate-900 text-white text-xs font-bold rounded-xl shadow-lg shadow-slate-200 hover:bg-cyan-500 hover:shadow-cyan-200 hover:scale-105 active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed disabled:shadow-none disabled:bg-slate-300"
                       >
                          兑换
                       </button>
                    </div>
                  </div>
                </div>
             </div>
          </div>
        </div>

        <aside class="lg:col-span-4 animate-fade-in-up" style="animation-delay: 0.2s;">
          <div class="bg-white/80 backdrop-blur-xl rounded-[32px] p-6 border border-white shadow-lg shadow-slate-100/50 h-fit sticky top-28">
            <h3 class="font-black text-slate-800 text-lg mb-6 flex items-center gap-2">
              <span>📜</span> 积分明细
            </h3>

            <div v-if="pointLogs.length === 0" class="flex flex-col items-center justify-center py-10 text-slate-400">
               <div class="text-4xl mb-2 opacity-30">🥥</div>
               <span class="text-xs">暂无积分记录</span>
            </div>

            <div class="space-y-4 max-h-[500px] overflow-y-auto custom-scrollbar pr-2">
              <div v-for="(log, idx) in pointLogs" :key="idx" 
                 class="flex items-center gap-4 p-3 rounded-2xl hover:bg-slate-50 transition-colors group animate-fade-in-right"
                 :style="{ animationDelay: `${idx * 0.05}s` }"
              >
                 <div class="w-10 h-10 rounded-full flex items-center justify-center text-lg shadow-sm border border-slate-100 group-hover:scale-110 transition-transform"
                   :class="log.amount > 0 ? 'bg-green-50 text-green-500' : 'bg-orange-50 text-orange-500'"
                 >
                   {{ log.amount > 0 ? '🎁' : '🛒' }}
                 </div>
                 
                 <div class="flex-1 min-w-0">
                    <div class="font-bold text-slate-700 text-sm truncate">{{ log.reason || (log.amount > 0 ? '系统赠送' : '商品兑换') }}</div>
                    <div class="text-[10px] text-slate-400 font-mono">{{ log.date || new Date().toLocaleDateString() }}</div>
                 </div>
                 
                 <div class="font-black font-mono text-sm"
                   :class="log.amount > 0 ? 'text-green-500' : 'text-orange-500'"
                 >
                   {{ log.amount > 0 ? '+' : '' }}{{ log.amount }}
                 </div>
              </div>
            </div>
            
             <div class="mt-6 pt-6 border-t border-slate-100 text-center">
              <button class="text-xs font-bold text-cyan-600 hover:text-cyan-700 transition flex items-center justify-center gap-1 mx-auto">
                查看更多记录 <span>↓</span>
              </button>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 1. 极光背景 */
.bg-aurora {
  background: linear-gradient(-45deg, #0891b2, #1e3a8a, #0ea5e9, #0f172a);
  background-size: 400% 400%;
  animation: auroraFlow 10s ease infinite;
}
@keyframes auroraFlow {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* 2. 玻璃容器 */
.card-glass {
  @apply rounded-[32px] shadow-2xl shadow-cyan-900/20 text-white;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 3. 超拟真 3D 水晶球 */
.crystal-ball {
  /* 基础玻璃材质 */
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.05) 50%, rgba(255, 255, 255, 0) 100%);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 
    inset 0 0 20px rgba(255, 255, 255, 0.2), /* 内部高光 */
    inset 0 -10px 20px rgba(0, 0, 0, 0.1), /* 内部阴影 */
    0 15px 35px rgba(34, 211, 238, 0.3), /* 外部彩色投影 */
    0 5px 15px rgba(0, 0, 0, 0.1);
}

.crystal-ball:hover {
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0.1) 50%, rgba(255, 255, 255, 0) 100%);
  box-shadow: 
    inset 0 0 30px rgba(255, 255, 255, 0.4),
    0 20px 40px rgba(34, 211, 238, 0.5);
}

.crystal-ball:active {
  transform: scale(0.95);
}

.rotate-ring {
  animation: spin 6s linear infinite;
}

/* 4. 文字流光 */
.shimmer-text {
  background: linear-gradient(110deg, #ffffff 30%, #cffafe 50%, #ffffff 70%);
  background-size: 200% auto;
  color: transparent;
  background-clip: text;
  -webkit-background-clip: text;
  animation: shine 5s linear infinite;
}

@keyframes shine {
  to { background-position: 200% center; }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 5. 列表交错动画 */
.animate-fade-in-right {
  animation: fadeInRight 0.5s ease-out forwards;
  opacity: 0;
}
@keyframes fadeInRight {
  from { opacity: 0; transform: translateX(10px); }
  to { opacity: 1; transform: translateX(0); }
}

/* 基础动画 */
.animate-fade-down { animation: fadeDown 0.6s ease-out; }
@keyframes fadeDown {
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}

.animate-fade-in-up { animation: fadeInUp 0.6s ease-out forwards; opacity: 0; }
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background-color: #cbd5e1; border-radius: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background-color: transparent; }
</style>