import { reactive, computed } from "vue";

// 统一存储 Key
const CART_KEY = "yuxian_cart";
const USER_KEY = "yuxian_user";

const savedUser = JSON.parse(localStorage.getItem(USER_KEY) || "null");
const savedCart = JSON.parse(localStorage.getItem(CART_KEY) || "[]");

export const store = reactive({
  // ✅ 核心数据源
  cart: savedCart,
  currentUser: savedUser,

  // 全局通知 & 动画信号
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

  // --- 核心方法 ---

  // 1. 添加商品
  addToCart(product, event = null) {
    const existingItem = this.cart.find((item) => item.id === product.id);

    if (existingItem) {
      existingItem.quantity++;
    } else {
      this.cart.push({
        ...product, // 保留原商品所有字段
        id: product.id, // 确保 ID 存在
        name: product.name,
        price: product.price,
        imageUrl: product.imageUrl || "/images/default.jpg",
        quantity: 1,
      });
    }

    this.saveCart();

    // 触发动画或通知
    if (event) {
      this.triggerFly(event, product.imageUrl);
    } else {
      this.showNotification(`已将 ${product.name} 加入购物车`);
    }
  },

  // 2. 更新数量 (CartView 专用)
  updateCartItem(productId, delta) {
    const item = this.cart.find((i) => i.id === productId);
    if (item) {
      item.quantity += delta;
      if (item.quantity <= 0) {
        this.removeFromCart(productId);
      } else {
        this.saveCart();
      }
    }
  },

  // 3. 移除商品
  removeFromCart(productId) {
    this.cart = this.cart.filter((item) => item.id !== productId);
    this.saveCart();
  },

  // 4. 清空购物车
  clearCart() {
    this.cart = [];
    this.saveCart();
  },

  // 5. 持久化
  saveCart() {
    localStorage.setItem(CART_KEY, JSON.stringify(this.cart));
  },

  // --- 兼容性方法 (修复报错的关键) ---

  // ✅ 修复：补充 getProductCount 方法
  getProductCount(productId) {
    const item = this.cart.find((item) => item.id === productId);
    return item ? item.quantity : 0;
  },

  // ✅ 修复：补充 decreaseItem 方法 (部分旧组件可能在用)
  decreaseItem(productId) {
    this.updateCartItem(productId, -1);
  },

  // --- 辅助功能 ---

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

    // 创建一个“瘦身版”的用户对象用于存储
    // 我们只存基本信息，不存巨大的 Base64 头像，防止 LocalStorage 爆满
    const userToSave = { ...user };

    // 如果头像数据太长（说明是 Base64），就不存到本地缓存里
    // 页面刷新后，ProfileView 会通过 API 重新拉取最新的头像
    if (userToSave.avatar && userToSave.avatar.length > 200) {
      userToSave.avatar = null; // 或者设置为一个默认的小图片 URL
    }

    try {
      localStorage.setItem("yuxian_user", JSON.stringify(userToSave));
      this.showNotification(`欢迎回来，${user.displayName || user.username}！`);
    } catch (e) {
      console.error("缓存写入失败:", e);
      this.showNotification(
        "登录成功（但缓存已满，下次需重新登录）",
        "warning"
      );
    }
  },

  logout() {
    this.currentUser = null;
    this.clearCart(); // 退出登录清空购物车
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
