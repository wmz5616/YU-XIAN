import { reactive } from "vue";

// 统一存储 Key
const CART_KEY = "yuxian_cart";
const USER_KEY = "yuxian_user";
const COUPON_KEY = "yuxian_coupons"; // 优惠券存储
const LOGS_KEY = "yuxian_point_logs"; // 🆕 积分明细存储

// 初始化读取
const savedUser = JSON.parse(localStorage.getItem(USER_KEY) || "null");
const savedCart = JSON.parse(localStorage.getItem(CART_KEY) || "[]");
const savedCoupons = JSON.parse(localStorage.getItem(COUPON_KEY) || "[]");
// 🆕 读取积分明细，如果没有记录，给两条默认的
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
  // ✅ 核心数据源
  cart: savedCart,
  currentUser: savedUser,
  myCoupons: savedCoupons,
  pointLogs: savedLogs, // 🆕 积分明细状态

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

  // --- 购物车方法 ---
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

  // --- 🎟️ 优惠券逻辑 (确保字段匹配) ---
  addCoupon(coupon) {
    const newCoupon = {
      id: Date.now(),
      couponName: coupon.name, // 👈 关键映射：把 name 转为 couponName
      amount: coupon.amount,
      minSpend: coupon.amount * 10,
      status: "UNUSED",
      receiveTime: new Date().toISOString(), // 存 ISO 格式方便后续处理
      type: "EXCHANGE",
    };
    this.myCoupons.unshift(newCoupon);
    this.saveCoupons();
  },

  saveCoupons() {
    localStorage.setItem(COUPON_KEY, JSON.stringify(this.myCoupons));
  },

  // --- 📝 积分明细逻辑 (保留最近5条) ---
  addPointLog(log) {
    const newLog = {
      id: Date.now(),
      time: new Date().toLocaleString(),
      ...log,
    };

    this.pointLogs.unshift(newLog); // 加到最前面

    // ✅ 限制只保留最近 5 条
    if (this.pointLogs.length > 5) {
      this.pointLogs = this.pointLogs.slice(0, 5);
    }

    this.savePointLogs();
  },

  savePointLogs() {
    localStorage.setItem(LOGS_KEY, JSON.stringify(this.pointLogs));
  },

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
    const userToSave = { ...user };
    if (userToSave.avatar && userToSave.avatar.length > 200)
      userToSave.avatar = null;
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
    localStorage.removeItem(COUPON_KEY);
    localStorage.removeItem(LOGS_KEY);
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
