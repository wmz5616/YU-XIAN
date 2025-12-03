import { reactive } from 'vue'

const savedUser = localStorage.getItem('yuxian_user')
const initialUser = savedUser ? JSON.parse(savedUser) : null

// 从本地存储恢复购物车（防止刷新后购物车变空）
const savedCart = localStorage.getItem('yuxian_cart')
const initialCart = savedCart ? JSON.parse(savedCart) : []

export const store = reactive({
  items: initialCart, // 购物车数组 (每个 item 都有 quantity 字段)
  currentUser: initialUser,
  
  notification: { show: false, message: '', type: 'success' },
  
  // 飞入动画信号
  flySignal: { id: 0, rect: null, img: '' },

  // 计算总数量 (所有商品的 quantity 之和)
  get cartCount() {
    return this.items.reduce((sum, item) => sum + item.quantity, 0)
  },

  // 计算总金额
  get totalPrice() {
    return this.items.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2)
  },
  
  // 【核心修改】合并添加逻辑
  addToCart(product, event = null) {
    // 1. 查找购物车里是否已存在该商品
    const existingItem = this.items.find(item => item.id === product.id)

    if (existingItem) {
      // 2. 如果存在，数量 +1
      existingItem.quantity++
    } else {
      // 3. 如果不存在，推入新对象，并初始化数量为 1
      // 注意：要深拷贝一份，防止污染原数据
      this.items.push({ ...product, quantity: 1 })
    }

    // 4. 保存到本地存储
    this.saveCart()

    // 触发动画或提示
    if (event) {
      this.triggerFly(event, product.imageUrl)
    } else {
      this.showNotification(`已将 ${product.name} 加入购物车`)
    }
  },

  // 【新增】减少数量
  decreaseItem(productId) {
    const item = this.items.find(item => item.id === productId)
    if (item) {
      if (item.quantity > 1) {
        item.quantity--
      } else {
        // 如果只剩1个再减，就移出购物车
        this.items = this.items.filter(i => i.id !== productId)
      }
      this.saveCart()
    }
  },

  // 【新增】直接移除商品
  removeItem(productId) {
    this.items = this.items.filter(item => item.id !== productId)
    this.saveCart()
  },

  getProductCount(productId) {
    const item = this.items.find(item => item.id === productId)
    return item ? item.quantity : 0
  },

  triggerFly(event, imgUrl) {
    const rect = event.target.getBoundingClientRect()
    this.flySignal.rect = {
      left: rect.left + rect.width / 2,
      top: rect.top + rect.height / 2
    }
    this.flySignal.img = imgUrl
    this.flySignal.id++
  },

  clearCart() {
    this.items = []
    this.saveCart()
  },

  // 持久化购物车
  saveCart() {
    localStorage.setItem('yuxian_cart', JSON.stringify(this.items))
  },

  login(user) {
    this.currentUser = user
    localStorage.setItem('yuxian_user', JSON.stringify(user))
    this.showNotification(`欢迎回来，${user.displayName}！`)
  },
  
  logout() {
    this.currentUser = null
    this.items = [] // 退出时清空购物车
    localStorage.removeItem('yuxian_user')
    localStorage.removeItem('yuxian_cart')
    this.showNotification('您已安全退出', 'success')
  },
  
  showNotification(msg, type = 'success') {
    this.notification.message = msg
    this.notification.type = type
    this.notification.show = true
    setTimeout(() => {
      this.notification.show = false
    }, 3000)
  }
})