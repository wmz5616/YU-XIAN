<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { store } from '../store.js'
import { request } from '@/utils/request'
import Swal from 'sweetalert2'

const router = useRouter()
const loading = ref(false)
const isSigned = ref(false)

const pointLogs = ref([])

const fetchLogs = async () => {
    try {
        const res = await request.get('/api/users/point-logs')
        pointLogs.value = res || []
    } catch (e) {
        console.error(e)
    }
}

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
    if (store.currentUser) {
        fetchLogs()
    }
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
                exchangeId: item.id
            });

            if (res && res.success) {
                store.currentUser.points = res.points;
                store.login(store.currentUser, true);
                
                await fetchLogs();

                Swal.fire('兑换成功', '优惠券已发放', 'success');
            }
        } catch (e) {
            Swal.fire('兑换失败', e.message || '系统繁忙', 'error');
        } finally {
            loading.value = false;
        }
    }
}

const handleSignIn = async () => {
    if (!store.currentUser) return router.push('/login');

    loading.value = true;
    try {
        const res = await request.post('/api/users/signin');
        if (res && res.success) {
            isSigned.value = true;
            localStorage.setItem(`sign_date_${store.currentUser.username}`, new Date().toLocaleDateString());
            
            store.currentUser.points = res.points;
            store.login(store.currentUser, true);
            
            await fetchLogs();

            Swal.fire('签到成功', `获得 ${res.reward} 积分`, 'success');
        }
    } catch (e) {
        Swal.fire('签到失败', e.message || '系统繁忙', 'error');
    } finally {
        loading.value = false;
    }
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
                  v-tilt="{ speed: 1000, max: 5, glare: true, 'max-glare': 0.1, scale: 1.02 }"
                  class="group relative bg-white rounded-[24px] shadow-lg shadow-slate-100 hover:shadow-xl transition-all duration-500 overflow-hidden border border-slate-100"
                >
                  <div class="absolute -right-10 -top-10 w-40 h-40 rounded-full blur-3xl opacity-10 group-hover:opacity-20 transition-opacity bg-gradient-to-br" :class="item.color"></div>

                  <div class="p-6 relative z-10">
                    <div class="flex justify-between items-start mb-6">
                      <div class="w-14 h-14 rounded-2xl bg-gradient-to-br flex items-center justify-center text-white shadow-lg shadow-indigo-100 transform group-hover:scale-110 transition-all duration-500" :class="item.color">
                        <span class="font-serif-sc font-black text-2xl">¥</span>
                      </div>
                      
                      <div class="text-right">
                         <div class="text-4xl font-black text-slate-800 font-serif-sc tracking-tight leading-none group-hover:text-transparent group-hover:bg-clip-text group-hover:bg-gradient-to-br transition-all" :class="item.color">
                            {{ item.amount }}
                         </div>
                         <div class="text-[10px] font-bold tracking-[0.2em] text-slate-400 uppercase mt-1">Coupon</div>
                      </div>
                    </div>

                    <div class="mb-4">
                        <h4 class="font-bold text-slate-800 text-lg mb-1 leading-tight">{{ item.name }}</h4>
                        <p class="text-xs text-slate-400 font-medium">满 <span class="text-slate-600 font-bold">¥{{ item.amount * 10 }}</span> 可用</p>
                    </div>
                  </div>

                  <div class="relative flex items-center justify-between px-4">
                      <div class="w-4 h-4 rounded-full bg-slate-50 -ml-6 shadow-inner"></div>
                      <div class="flex-1 border-t-2 border-dashed border-slate-100 h-px"></div>
                      <div class="w-4 h-4 rounded-full bg-slate-50 -mr-6 shadow-inner"></div>
                  </div>

                  <div class="p-6 pt-4 flex items-center justify-between bg-slate-50/50 backdrop-blur-sm">
                       <div>
                          <div class="text-[10px] text-slate-400 font-bold uppercase mb-0.5">Need</div>
                          <div class="flex items-baseline gap-1 text-cyan-600 font-black font-mono text-xl">
                             {{ item.cost }} <span class="text-[10px] font-bold opacity-60">PTS</span>
                          </div>
                       </div>
                       
                       <button @click.stop="handleExchange(item)" 
                          :disabled="loading || (store.currentUser?.points || 0) < item.cost"
                          class="relative overflow-hidden px-6 py-2.5 bg-slate-900 text-white text-xs font-bold rounded-xl shadow-lg hover:shadow-cyan-500/30 hover:scale-105 active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed disabled:shadow-none disabled:bg-slate-300 group/btn"
                       >
                          <span class="relative z-10 group-hover/btn:hidden">Redeem</span>
                          <span class="relative z-10 hidden group-hover/btn:inline-block">兑换</span>
                          <div class="absolute inset-0 bg-gradient-to-r from-cyan-500 to-blue-500 opacity-0 group-hover/btn:opacity-100 transition-opacity"></div>
                       </button>
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
              <div v-for="(log, idx) in pointLogs" :key="log.id" 
                 class="flex items-center gap-4 p-3 rounded-2xl hover:bg-slate-50 transition-colors group animate-fade-in-right"
                 :style="{ animationDelay: `${idx * 0.05}s` }"
              >
                 <div class="w-10 h-10 rounded-full flex items-center justify-center text-lg shadow-sm border border-slate-100 group-hover:scale-110 transition-transform"
                   :class="log.type === 1 ? 'bg-green-50 text-green-500' : 'bg-orange-50 text-orange-500'"
                 >
                   {{ log.type === 1 ? '🎁' : '🛒' }}
                 </div>
                 
                 <div class="flex-1 min-w-0">
                    <div class="font-bold text-slate-700 text-sm truncate">{{ log.description || (log.type === 1 ? '系统赠送' : '商品兑换') }}</div>
                    <div class="text-[10px] text-slate-400 font-mono">{{ new Date(log.createTime).toLocaleString() }}</div>
                 </div>
                 
                 <div class="font-black font-mono text-sm"
                   :class="log.type === 1 ? 'text-green-500' : 'text-orange-500'"
                 >
                   {{ log.type === 1 ? '+' : '-' }}{{ log.amount }}
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

.card-glass {
  @apply rounded-[32px] shadow-2xl shadow-cyan-900/20 text-white;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.crystal-ball {
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.05) 50%, rgba(255, 255, 255, 0) 100%);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 
    inset 0 0 20px rgba(255, 255, 255, 0.2),
    inset 0 -10px 20px rgba(0, 0, 0, 0.1),
    0 15px 35px rgba(34, 211, 238, 0.3),
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

.animate-fade-in-right {
  animation: fadeInRight 0.5s ease-out forwards;
  opacity: 0;
}
@keyframes fadeInRight {
  from { opacity: 0; transform: translateX(10px); }
  to { opacity: 1; transform: translateX(0); }
}

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