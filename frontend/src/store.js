import { reactive } from "vue";

const CART_KEY = "yuxian_cart";
const USER_KEY = "yuxian_user";

const LOGS_KEY = "yuxian_point_logs";

const savedUser = JSON.parse(localStorage.getItem(USER_KEY) || "null");
const savedCart = JSON.parse(localStorage.getItem(CART_KEY) || "[]");

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
  cart: savedCart,
  currentUser: savedUser,
  myCoupons: [],
  pointLogs: savedLogs,

  notification: { show: false, message: "", type: "success" },
  flySignal: { id: 0, rect: null, img: "" },

  get cartCount() {
    return this.cart.reduce((sum, item) => sum + item.quantity, 0);
  },

  get totalPrice() {
    return this.cart
      .reduce((sum, item) => sum + item.price * item.quantity, 0)
      .toFixed(2);
  },

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

  deductPoints(amount) {
    if (this.currentUser) {
      this.currentUser.points = (this.currentUser.points || 0) - amount;
      localStorage.setItem(USER_KEY, JSON.stringify(this.currentUser));
    }
  },

  addPointLog(log) {
    const newLog = {
      id: Date.now(),
      time: new Date().toLocaleString(),
      ...log,
    };

    this.pointLogs.unshift(newLog);

    if (this.pointLogs.length > 20) {
      this.pointLogs = this.pointLogs.slice(0, 20);
    }

    this.savePointLogs();
  },

  savePointLogs() {
    localStorage.setItem(LOGS_KEY, JSON.stringify(this.pointLogs));
  },

  addCoupon(coupon) {
    this.showNotification(
      `优惠券 ${coupon.name} 兑换成功，请刷新列表查看`,
      "success"
    );
  },

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
