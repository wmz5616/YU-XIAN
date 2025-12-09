<script setup>
import { ref, onMounted } from 'vue'
import { store } from '../store.js'
import { useRouter } from 'vue-router'

const router = useRouter()
const orders = ref([])
const showAddressModal = ref(false)
const newAddress = ref({ contact: '', phone: '', detail: '', tag: '家' })
const isLocating = ref(false)

onMounted(async () => {
  if (!store.currentUser) {
    router.push('/login')
    return
  }
  try {
    const res = await fetch(`http://localhost:8080/api/products/orders?username=${store.currentUser.username}`)
    orders.value = await res.json()
  } catch (error) {
    console.error('获取订单失败', error)
  }
})

const locateUser = () => {
  if (typeof AMap === 'undefined') {
    store.showNotification('高德地图加载失败，请检查网络或Key', 'error')
    return
  }

  isLocating.value = true
  store.showNotification('正在调用高德定位...')

  AMap.plugin('AMap.Geolocation', function () {
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      needAddress: true, 
      extensions: 'all'
    })

    geolocation.getCurrentPosition(function (status, result) {
      isLocating.value = false
      console.log('高德定位结果:', status, result)

      if (status === 'complete') {
        let address = result.formattedAddress

        if (!address || address.length === 0) {
          const comp = result.addressComponent
          if (comp) {
            address = `${comp.province}${comp.city}${comp.district}${comp.street || ''}${comp.streetNumber || ''}`
          } else {
            address = `当前位置 (经度:${result.position.lng}, 纬度:${result.position.lat})`
          }
        }
        newAddress.value.detail = address
        store.showNotification('定位成功')
      } else {
        console.error('定位失败原因:', result)
        store.showNotification('定位失败：' + (result.message || '权限被拒绝或超时'), 'error')
      }
    })
  })
}

const saveAddress = async () => {
  if (!newAddress.value.contact || !newAddress.value.phone || !newAddress.value.detail) {
    store.showNotification('请填写完整地址信息', 'error')
    return
  }

  const currentAddresses = store.currentUser.addresses || []
  const isDefault = currentAddresses.length === 0
  const updatedAddresses = [...currentAddresses, { ...newAddress.value, isDefault }]

  try {
    const res = await fetch('http://localhost:8080/api/users/address', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: store.currentUser.username,
        addresses: updatedAddresses
      })
    })

    if (res.ok) {
      const updatedUser = await res.json()
      store.login(updatedUser)
      showAddressModal.value = false
      newAddress.value = { contact: '', phone: '', detail: '', tag: '家' }
      store.showNotification('地址添加成功')
    }
  } catch (e) {
    store.showNotification('保存失败', 'error')
  }
}

const removeAddress = async (index) => {
  if (!confirm('确定要删除这个地址吗？')) return
  const updatedAddresses = [...store.currentUser.addresses]
  updatedAddresses.splice(index, 1)

  try {
    const res = await fetch('http://localhost:8080/api/users/address', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: store.currentUser.username, addresses: updatedAddresses })
    })
    if (res.ok) {
      store.login(await res.json())
      store.showNotification('地址已删除')
    }
  } catch (e) { store.showNotification('删除失败', 'error') }
}

const getStatusStyle = (status) => {
  switch (status) {
    case '待发货': return 'bg-amber-50 text-amber-600 border-amber-200'
    case '运输中': return 'bg-blue-50 text-blue-600 border-blue-200'
    case '已送达': return 'bg-green-50 text-green-600 border-green-200'
    default: return 'bg-slate-50 text-slate-500 border-slate-200'
  }
}

const formatDate = (isoString) => {
  if (!isoString) return ''
  return new Date(isoString).toLocaleString('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  })
}

const getProductImage = (nameStr) => {
  if (!nameStr) return '/images/default.jpg'
  
  // 贝类
  if (nameStr.includes('象拔蚌')) return '/images/xiangbabang.jpg'
  if (nameStr.includes('鲍鱼')) return '/images/baoyu.jpg'
  if (nameStr.includes('扇贝')) return '/images/shanbei.jpg'
  if (nameStr.includes('生蚝')) return '/images/shenghao.jpg'
  if (nameStr.includes('花蛤')) return '/images/huage.jpg'
  if (nameStr.includes('海螺')) return '/images/hailuo.jpg'
  if (nameStr.includes('蛏子')) return '/images/chengzi.jpg'
  if (nameStr.includes('文蛤')) return '/images/wenge.jpg'
  if (nameStr.includes('青口')) return '/images/qingkou.jpg'
  if (nameStr.includes('田螺')) return '/images/tianluo.jpg'
  if (nameStr.includes('赤贝')) return '/images/chibei.jpg'
  if (nameStr.includes('象牙蚌')) return '/images/xiangyabang.jpg'
  if (nameStr.includes('带子')) return '/images/daizi.jpg'
  if (nameStr.includes('血蚶')) return '/images/xuehan.jpg'
  if (nameStr.includes('响螺')) return '/images/xiangluo.jpg'
  if (nameStr.includes('河蚌')) return '/images/hebang.jpg'
  if (nameStr.includes('泥螺')) return '/images/niluo.jpg'
  if (nameStr.includes('日月贝')) return '/images/riyuebei.jpg'
  if (nameStr.includes('鸟蛤')) return '/images/niaoge.jpg'

  // 虾类
  if (nameStr.includes('波士顿龙虾')) return '/images/boshidunlongxia.jpg'
  if (nameStr.includes('澳洲龙虾')) return '/images/aozhoulongxia.jpg'
  if (nameStr.includes('虾蛄')) return '/images/pipixia.jpg'
  if (nameStr.includes('小龙虾')) return '/images/xiaolongxia.jpg'
  if (nameStr.includes('基围虾')) return '/images/jiweixia.jpg'
  if (nameStr.includes('对虾')) return '/images/duixia.jpg'
  if (nameStr.includes('虎虾')) return '/images/huxia.jpg'
  if (nameStr.includes('明虾')) return '/images/mingxia.jpg'
  if (nameStr.includes('南美白对虾')) return '/images/nanmeibaiduixia.jpg'
  if (nameStr.includes('沼虾')) return '/images/zhaoxia.jpg'
  if (nameStr.includes('甜虾')) return '/images/tianxia.jpg'
  if (nameStr.includes('白虾')) return '/images/baixia.jpg'
  if (nameStr.includes('竹节虾')) return '/images/zhujiexia.jpg'
  if (nameStr.includes('斑节对虾')) return '/images/banjieduixia.jpg'
  if (nameStr.includes('罗氏沼虾')) return '/images/luoshizhaoxia.jpg'
  if (nameStr.includes('九节虾')) return '/images/jiujiexia.jpg'
  if (nameStr.includes('鹰爪虾')) return '/images/yingzhaoxia.jpg'
  if (nameStr.includes('草虾')) return '/images/caoxia.jpg'
  if (nameStr.includes('红虾')) return '/images/hongxia.jpg'
  if (nameStr.includes('北极甜虾')) return '/images/beijitianxia.jpg'

  // 蟹类
  if (nameStr.includes('帝王蟹')) return '/images/diwangxie.jpg'
  if (nameStr.includes('大闸蟹')) return '/images/dazhaxie.jpg'
  if (nameStr.includes('青蟹') && !nameStr.includes('潮汕')) return '/images/qingxie.jpg'
  if (nameStr.includes('梭子蟹') && !nameStr.includes('三疣') && !nameStr.includes('远海')) return '/images/suozixie.jpg'
  if (nameStr.includes('珍宝蟹')) return '/images/zhenbaoxie.jpg'
  if (nameStr.includes('花蟹') && !nameStr.includes('红花')) return '/images/huaxie.jpg'
  if (nameStr.includes('面包蟹')) return '/images/mianbaoxie.jpg'
  if (nameStr.includes('三疣梭子蟹')) return '/images/sanyousuozixie.jpg'
  if (nameStr.includes('红花蟹')) return '/images/honghuaxie.jpg'
  if (nameStr.includes('毛蟹') && !nameStr.includes('红毛')) return '/images/maoxie.jpg'
  if (nameStr.includes('黄油蟹')) return '/images/huangyouxie.jpg'
  if (nameStr.includes('六月黄')) return '/images/liuyuehuang.jpg'
  if (nameStr.includes('紫蟹')) return '/images/zixie.jpg'
  if (nameStr.includes('石蟹')) return '/images/shixie.jpg'
  if (nameStr.includes('兰花蟹')) return '/images/lanhuaxie.jpg'
  if (nameStr.includes('红毛蟹')) return '/images/hongmaoxie.jpg'
  if (nameStr.includes('晶莹蟹')) return '/images/jingyingxie.jpg'
  if (nameStr.includes('和乐蟹')) return '/images/helexie.jpg'
  if (nameStr.includes('远海梭子蟹')) return '/images/yuanhaisuozixie.jpg'
  if (nameStr.includes('潮汕青蟹')) return '/images/chaoshanqingxie.jpg'

  // 鱼类
  if (nameStr.includes('三文鱼') || nameStr.includes('虹鳟')) return '/images/sanwenyu.jpg'
  if (nameStr.includes('石斑鱼')) return '/images/shibanyu.jpg'
  if (nameStr.includes('大黄鱼')) return '/images/dahuangyu.jpg'
  if (nameStr.includes('带鱼')) return '/images/daiyu.jpg'
  if (nameStr.includes('鳕鱼')) return '/images/xueyu.jpg'
  if (nameStr.includes('鲈鱼')) return '/images/luyu.jpg'
  if (nameStr.includes('鲳鱼') && !nameStr.includes('金鲳')) return '/images/changyu.jpg'
  if (nameStr.includes('马鲛鱼')) return '/images/majiaoyu.jpg'
  if (nameStr.includes('小黄鱼')) return '/images/xiaohuangyu.jpg'
  if (nameStr.includes('草鱼')) return '/images/caoyu.jpg'
  if (nameStr.includes('鲤鱼')) return '/images/liyu.jpg'
  if (nameStr.includes('黑鱼')) return '/images/heiyu.jpg'
  if (nameStr.includes('多宝鱼')) return '/images/duobaoyu.jpg'
  if (nameStr.includes('多春鱼')) return '/images/duochunyu.jpg'
  if (nameStr.includes('金枪鱼')) return '/images/jinqiangyu.jpg'
  if (nameStr.includes('罗非鱼')) return '/images/luofeiyu.jpg'
  if (nameStr.includes('鳗鱼')) return '/images/manyu.jpg'
  if (nameStr.includes('偏口鱼')) return '/images/piankouyu.jpg'
  if (nameStr.includes('武昌鱼')) return '/images/wuchangyu.jpg'
  if (nameStr.includes('金鲳鱼')) return '/images/jinchangyu.jpg'

  // 头足类
  if (nameStr.includes('北太平洋巨型章鱼')) return '/images/beitaipingyangjuxingzhangyu.jpg'
  if (nameStr.includes('鱿鱼') && !nameStr.includes('赤') && !nameStr.includes('阿根廷')) return '/images/youyu.jpg'
  if (nameStr.includes('墨鱼')) return '/images/moyu.jpg'
  if (nameStr.includes('章鱼')) return '/images/zhangyu.jpg'
  if (nameStr.includes('笔管鱼')) return '/images/biguanyu.jpg'
  if (nameStr.includes('短蛸')) return '/images/duanshao.jpg'
  if (nameStr.includes('柔鱼')) return '/images/rouyu.jpg'
  if (nameStr.includes('花枝')) return '/images/huazhi.jpg'
  if (nameStr.includes('软丝')) return '/images/ruansi.jpg'
  if (nameStr.includes('小管')) return '/images/xiaoguan.jpg'
  if (nameStr.includes('透抽')) return '/images/touchou.jpg'
  if (nameStr.includes('真蛸')) return '/images/zhenshao.jpg'
  if (nameStr.includes('曼氏无针乌贼')) return '/images/manshiwuzhenwuzei.jpg'
  if (nameStr.includes('剑尖枪乌贼')) return '/images/jianjianqiangwuzei.jpg'
  if (nameStr.includes('赤鱿')) return '/images/chiyou.jpg'
  if (nameStr.includes('阿根廷鱿鱼')) return '/images/agentingyouyu.jpg'
  if (nameStr.includes('日本枪乌贼')) return '/images/ribenqiangwuzei.jpg'
  if (nameStr.includes('目斗鱼')) return '/images/mudouyu.jpg'
  if (nameStr.includes('长蛸')) return '/images/changshao.jpg'
  if (nameStr.includes('八腕总目')) return '/images/bawan.jpg'

  return '/images/default.jpg'
}
const handleAvatarUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    store.showNotification('图片太大了，请小于 2MB', 'error')
    return
  }
  const reader = new FileReader()
  reader.readAsDataURL(file)
  reader.onload = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/users/avatar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: store.currentUser.username, avatar: reader.result })
      })
      if (res.ok) {
        store.login(await res.json())
        store.showNotification('头像更新成功！')
      }
    } catch (e) { store.showNotification('上传失败', 'error') }
  }
}
</script>

<template>
  <div class="container mx-auto px-4 py-8 max-w-6xl min-h-screen">

    <div v-if="showAddressModal"
      class="fixed inset-0 bg-black/60 z-[100] flex items-center justify-center p-4 backdrop-blur-sm transition-opacity">
      <div class="bg-white rounded-2xl p-8 w-full max-w-md shadow-2xl animate-scale-up border border-slate-100">
        <h3 class="text-xl font-bold mb-6 text-slate-900">新增地址</h3>
        <div class="space-y-4">
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1 block">联系人</label>
            <input v-model="newAddress.contact" placeholder="例如: 张三"
              class="w-full border border-slate-300 p-3 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition">
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1 block">手机号</label>
            <input v-model="newAddress.phone" placeholder="例如: 13800138000"
              class="w-full border border-slate-300 p-3 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition">
          </div>

          <div>
            <div class="flex justify-between items-center mb-1">
              <label class="text-xs font-bold text-slate-500 block">详细地址</label>
              <button @click="locateUser"
                class="flex items-center gap-1 text-xs text-blue-600 hover:text-blue-700 transition font-bold"
                :disabled="isLocating">
                <svg v-if="!isLocating" class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"></path>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"></path>
                </svg>
                <svg v-else class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <span>{{ isLocating ? '高德定位中...' : '定位当前位置' }}</span>
              </button>
            </div>
            <textarea v-model="newAddress.detail" placeholder="点击定位自动填写"
              class="w-full border border-slate-300 p-3 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none transition resize-none h-24"></textarea>
          </div>

          <div>
            <label class="text-xs font-bold text-slate-500 mb-2 block">标签</label>
            <div class="flex gap-3">
              <button @click="newAddress.tag = '家'"
                :class="`flex-1 py-2.5 rounded-xl border font-bold text-sm transition ${newAddress.tag === '家' ? 'bg-slate-800 text-white border-slate-800' : 'border-slate-200 text-slate-500 hover:border-slate-300'}`">家</button>
              <button @click="newAddress.tag = '公司'"
                :class="`flex-1 py-2.5 rounded-xl border font-bold text-sm transition ${newAddress.tag === '公司' ? 'bg-slate-800 text-white border-slate-800' : 'border-slate-200 text-slate-500 hover:border-slate-300'}`">公司</button>
            </div>
          </div>
        </div>
        <div class="mt-8 flex gap-4">
          <button @click="showAddressModal = false"
            class="flex-1 py-3 text-slate-500 hover:bg-slate-50 rounded-xl font-medium transition">取消</button>
          <button @click="saveAddress"
            class="flex-1 py-3 bg-blue-900 text-white rounded-xl font-bold shadow-lg hover:bg-blue-800 transition transform hover:-translate-y-0.5">保存</button>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
      <div class="lg:col-span-4 space-y-6">
        <div
          class="bg-white rounded-3xl p-6 shadow-xl shadow-slate-200/50 border border-white relative overflow-hidden group">
          <div class="absolute top-0 left-0 w-full h-24 bg-gradient-to-r from-blue-900 to-blue-800"></div>
          <div class="relative z-10 flex flex-col items-center mt-4">
            <div class="relative cursor-pointer">
              <input type="file" accept="image/*" class="absolute inset-0 w-full h-full opacity-0 z-20 cursor-pointer"
                @change="handleAvatarUpload" />
              <div class="w-20 h-20 rounded-full border-4 border-white shadow-lg overflow-hidden bg-slate-100">
                <img v-if="store.currentUser?.avatar" :src="store.currentUser.avatar"
                  class="w-full h-full object-cover" />
                <div v-else class="w-full h-full flex items-center justify-center text-2xl font-bold text-slate-300">
                  {{ store.currentUser?.displayName?.charAt(0) }}
                </div>
              </div>
              <div class="absolute bottom-0 right-0 bg-white rounded-full p-1 shadow-sm border border-slate-100">
                <img src="/icons/icon-search.png" class="w-3 h-3 opacity-50" />
              </div>
            </div>
            <div class="text-center mt-3">
              <h2 class="text-xl font-bold text-slate-800">{{ store.currentUser?.displayName || '未设置昵称' }}</h2>
              <div class="flex items-center justify-center gap-2 mt-1">
                <span class="text-xs text-slate-400">会员名:</span>
                <span class="font-mono text-xs bg-slate-100 px-2 py-0.5 rounded text-slate-600 border border-slate-200">
                  {{ store.currentUser?.username }}
                </span>
              </div>
            </div>
            <div class="flex justify-between w-full mt-6 px-4 pt-6 border-t border-slate-100">
              <div class="text-center">
                <div class="text-lg font-bold text-slate-800">{{ orders.length }}</div>
                <div class="text-xs text-slate-400">全部订单</div>
              </div>
              <div class="text-center">
                <div class="text-lg font-bold text-slate-800">0</div>
                <div class="text-xs text-slate-400">优惠券</div>
              </div>
              <div class="text-center">
                <div class="text-lg font-bold text-slate-800">0</div>
                <div class="text-xs text-slate-400">积分</div>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-3xl p-6 shadow-sm border border-slate-100">
          <div class="flex justify-between items-center mb-4">
            <h3 class="font-bold text-slate-800">收货地址</h3>
            <button @click="showAddressModal = true" class="text-xs text-blue-600 hover:underline font-medium">+
              新增</button>
          </div>
          <div v-if="store.currentUser?.addresses?.length > 0" class="space-y-3">
            <div v-for="(addr, index) in store.currentUser.addresses" :key="index"
              class="p-4 rounded-xl bg-slate-50 border border-slate-100 relative group hover:border-blue-300 transition">
              <button @click="removeAddress(index)"
                class="absolute top-2 right-2 text-slate-300 hover:text-red-500 opacity-0 group-hover:opacity-100 transition px-2">×</button>
              <div class="flex items-start gap-4">
                <div class="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 font-bold text-xs"
                  :class="addr.tag === '家' ? 'bg-orange-100 text-orange-700' : 'bg-blue-100 text-blue-700'">
                  {{ addr.tag }}
                </div>
                <div class="flex-1 min-w-0 pt-0.5">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="font-bold text-sm text-slate-800">{{ addr.contact }}</span>
                    <span class="text-xs text-slate-400 font-mono">{{ addr.phone }}</span>
                    <span v-if="addr.isDefault"
                      class="text-[10px] bg-red-50 text-red-500 px-1.5 rounded border border-red-100">默认</span>
                  </div>
                  <p class="text-xs text-slate-500 leading-relaxed truncate">{{ addr.detail }}</p>
                </div>
              </div>
            </div>
          </div>
          <div v-else
            class="text-center py-8 text-slate-400 text-xs bg-slate-50 rounded-xl border border-dashed border-slate-200 cursor-pointer hover:bg-slate-100 transition"
            @click="showAddressModal = true">
            暂无地址，点击添加
          </div>
        </div>
      </div>

      <div class="lg:col-span-8">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-xl font-bold text-slate-800">我的订单记录</h2>
          <div class="flex gap-2">
            <input type="text" placeholder="搜索订单号/商品..."
              class="bg-white border border-slate-200 rounded-lg px-3 py-1.5 text-sm w-48 focus:outline-none focus:border-blue-500 transition">
          </div>
        </div>
        <div v-if="orders.length > 0" class="space-y-5">
          <div v-for="order in orders" :key="order.id"
            class="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm hover:shadow-lg transition-all duration-300">
            <div class="flex justify-between items-center pb-4 border-b border-slate-50 mb-4">
              <span class="text-xs font-mono text-slate-400">Order #{{ 20250000 + order.id }}</span>
              <div class="flex items-center gap-3">
                <span class="text-xs text-slate-400">{{ formatDate(order.createTime) }}</span>
                <span :class="`px-2.5 py-0.5 text-xs font-bold rounded border ${getStatusStyle(order.status)}`">
                  {{ order.status }}
                </span>
              </div>
            </div>
            <div class="flex gap-5">
              <div class="w-20 h-20 bg-slate-100 rounded-lg overflow-hidden border border-slate-200 flex-shrink-0">
                <img :src="getProductImage(order.productNames)" class="w-full h-full object-cover" />
              </div>
              <div class="flex-1 flex flex-col justify-between">
                <div>
                  <h3 class="font-bold text-slate-800 text-base mb-1 line-clamp-1">{{ order.productNames }}</h3>
                  <p class="text-xs text-slate-400">规格：标准装 | 配送：顺丰冷链</p>
                </div>
                <div class="flex justify-between items-end">
                  <span class="text-lg font-bold text-slate-900 font-serif-sc">¥{{ order.totalPrice }}</span>
                  <div class="flex gap-2">
                    <button
                      class="px-3 py-1.5 border border-slate-200 rounded-md text-xs text-slate-600 hover:bg-slate-50 transition">申请售后</button>
                    <button
                      class="px-3 py-1.5 bg-blue-900 text-white rounded-md text-xs hover:bg-blue-800 transition shadow-md shadow-blue-900/10">再来一单</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else
          class="bg-white rounded-3xl border border-dashed border-slate-200 p-12 text-center h-96 flex flex-col items-center justify-center">
          <div class="w-20 h-20 bg-slate-50 rounded-full flex items-center justify-center mb-4">
            <img src="/icons/empty-order.png" class="w-10 h-10 opacity-40 grayscale" />
          </div>
          <h3 class="font-bold text-slate-800 mb-1">暂无订单</h3>
          <p class="text-sm text-slate-400 mb-6">快去挑选您心仪的海鲜吧</p>
          <button @click="router.push('/')"
            class="px-6 py-2 bg-slate-900 text-white text-sm rounded-full hover:bg-slate-800 transition">去逛逛</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.input-field {
  @apply w-full border border-slate-200 bg-slate-50 p-3 rounded-xl focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 outline-none transition text-sm;
}

.tag-btn {
  @apply flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-500 font-bold text-sm hover:border-slate-300 transition;
}

.tag-btn.active {
  @apply bg-slate-900 text-white border-slate-900 shadow-md;
}

.btn-primary {
  @apply flex-1 py-3 bg-blue-600 text-white rounded-xl font-bold shadow-lg shadow-blue-600/30 hover:bg-blue-700 transition active:scale-95;
}

.btn-secondary {
  @apply flex-1 py-3 text-slate-500 hover:bg-slate-50 rounded-xl font-medium transition;
}

@keyframes shine {
  100% {
    left: 125%;
  }
}

.animate-shine {
  animation: shine 2.5s infinite;
}
</style>