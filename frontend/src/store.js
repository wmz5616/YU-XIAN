import { reactive } from "vue";

// 统一存储 Key
const CART_KEY = "yuxian_cart";
const USER_KEY = "yuxian_user";
// [删除] const COUPON_KEY = "yuxian_coupons";
const LOGS_KEY = "yuxian_point_logs";

// 初始化读取
const savedUser = JSON.parse(localStorage.getItem(USER_KEY) || "null");
const savedCart = JSON.parse(localStorage.getItem(CART_KEY) || "[]");
// [删除] const savedCoupons = JSON.parse(localStorage.getItem(COUPON_KEY) || "[]");

// 初始化积分明细
const savedLogs = JSON.parse(
  localStorage.getItem(LOGS_KEY) ||
    JSON.stringify([
      {
        id: 1,
        type: "income",
        title: "系统奖励",
        amount: 100,
        time: new Date().toLocaleString(),
      },
      {
        id: 2,
        type: "income",
        title: "首次登录",
        amount: 50,
        time: new Date().toLocaleString(),
      },
    ])
);

export const store = reactive({
  // --- 核心数据源 ---
  cart: savedCart,
  currentUser: savedUser,
  myCoupons: [], // 修复：默认为空数组，完全依赖 API 获取
  pointLogs: savedLogs, // 修复：继续依赖本地存储作为数据源

  notification: { show: false, message: "", type: "success" },
  flySignal: { id: 0, rect: null, img: "" },

  // --- 计算属性 ---
  get cartCount() {
    return this.cart.reduce((sum, item) => sum + item.quantity, 0);
  },

  get totalPrice() {
    return this.cart
      .reduce((sum, item) => sum + item.price * item.quantity, 0)
      .toFixed(2);
  },

  // --- 🛒 购物车方法 ---
  addToCart(product, event = null) {
    const existingItem = this.cart.find((item) => item.id === product.id);
    if (existingItem) {
      existingItem.quantity++;
    } else {
      this.cart.push({
        ...product,
        id: product.id,
        quantity: 1,
        imageUrl: product.imageUrl || "/images/default.jpg",
      });
    }
    this.saveCart();
    if (event) this.triggerFly(event, product.imageUrl);
    else this.showNotification(`已将 ${product.name} 加入购物车`);
  },

  updateCartItem(productId, quantity) {
    const item = this.cart.find((i) => i.id === productId);
    if (item) {
      item.quantity = quantity;
      if (item.quantity <= 0) this.removeFromCart(productId);
      else this.saveCart();
    }
  },

  removeFromCart(productId) {
    this.cart = this.cart.filter((item) => item.id !== productId);
    this.saveCart();
  },

  clearCart() {
    this.cart = [];
    this.saveCart();
  },

  saveCart() {
    localStorage.setItem(CART_KEY, JSON.stringify(this.cart));
  },

  // --- 👤 用户 & 积分管理 ---

  // 1. 扣除积分并同步保存
  deductPoints(amount) {
    if (this.currentUser) {
      this.currentUser.points = (this.currentUser.points || 0) - amount;
      localStorage.setItem(USER_KEY, JSON.stringify(this.currentUser));
    }
  },

  // 2. 增加积分明细 (保留最近20条)
  addPointLog(log) {
    const newLog = {
      id: Date.now(),
      time: new Date().toLocaleString(),
      ...log, // 传入 type, title, amount
    };

    // 插入到数组最前面
    this.pointLogs.unshift(newLog);

    if (this.pointLogs.length > 20) {
      this.pointLogs = this.pointLogs.slice(0, 20);
    }

    this.savePointLogs();
  },

  savePointLogs() {
    // 修复：积分明细应始终保存，不应在 logout 时清除
    localStorage.setItem(LOGS_KEY, JSON.stringify(this.pointLogs));
  },

  // --- 🎟️ 优惠券逻辑 (修复重复问题) ---
  // 3. 兑换/领取优惠券
  addCoupon(coupon) {
    // 修复：兑换优惠券后，不应该再手动加入 myCoupons。
    // 因为这会导致 CouponView.vue 在渲染时出现重复数据。
    // 兑换成功后，应该依赖 CouponView 或 ProfileView 重新调用 API 获取最新列表。
    this.showNotification(
      `优惠券 ${coupon.name} 兑换成功，请刷新列表查看`,
      "success"
    );
  },

  // [删除] saveCoupons() 方法

  // --- 其他辅助 ---
  getProductCount(productId) {
    const item = this.cart.find((item) => item.id === productId);
    return item ? item.quantity : 0;
  },

  decreaseItem(productId) {
    const item = this.cart.find((item) => item.id === productId);
    if (item) this.updateCartItem(productId, item.quantity - 1);
  },

  triggerFly(event, imgUrl) {
    if (!event || !event.target) return;
    const rect = event.target.getBoundingClientRect();
    this.flySignal = {
      id: this.flySignal.id + 1,
      rect: {
        left: rect.left + rect.width / 2,
        top: rect.top + rect.height / 2,
      },
      img: imgUrl,
    };
  },

  login(user) {
    this.currentUser = user;
    if (this.currentUser.points === undefined) {
      this.currentUser.points = 0;
    }
    const userToSave = { ...this.currentUser };
    try {
      localStorage.setItem(USER_KEY, JSON.stringify(userToSave));
      this.showNotification(`欢迎回来，${user.displayName || user.username}！`);
    } catch (e) {
      console.error(e);
    }
  },

  logout() {
    this.currentUser = null;
    this.myCoupons = [];
    this.pointLogs = [];
    this.clearCart();
    localStorage.removeItem(USER_KEY);
    // [删除] localStorage.removeItem(COUPON_KEY);
    // ✅ 修复：不删除 LOGS_KEY，保持明细持久化
    // localStorage.removeItem(LOGS_KEY);
    this.showNotification("您已安全退出", "success");
  },

  showNotification(msg, type = "success") {
    this.notification.message = msg;
    this.notification.type = type;
    this.notification.show = true;
    setTimeout(() => {
      this.notification.show = false;
    }, 3000);
  },
});
