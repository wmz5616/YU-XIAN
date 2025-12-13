import { reactive } from "vue";

const savedUser = localStorage.getItem("yuxian_user");
let initialUser = null;
try {
  if (savedUser && savedUser !== "undefined") {
    initialUser = JSON.parse(savedUser);
  } else {
    localStorage.removeItem("yuxian_user");
  }
} catch (e) {
  console.error("用户信息解析失败，已重置", e);
  localStorage.removeItem("yuxian_user");
}

const savedCart = localStorage.getItem("yuxian_cart");
let initialCart = [];
try {
  if (savedCart && savedCart !== "undefined") {
    initialCart = JSON.parse(savedCart);
  } else {
    localStorage.removeItem("yuxian_cart");
  }
} catch (e) {
  console.error("购物车数据解析失败，已重置", e);
  localStorage.removeItem("yuxian_cart");
}

export const store = reactive({
  items: initialCart,
  currentUser: initialUser,

  notification: { show: false, message: "", type: "success" },

  flySignal: { id: 0, rect: null, img: "" },

  get cartCount() {
    return this.items.reduce((sum, item) => sum + item.quantity, 0);
  },

  get totalPrice() {
    return this.items
      .reduce((sum, item) => sum + item.price * item.quantity, 0)
      .toFixed(2);
  },

  addToCart(product, event = null) {
    const existingItem = this.items.find((item) => item.id === product.id);

    if (existingItem) {
      existingItem.quantity++;
    } else {
      this.items.push({ ...product, quantity: 1 });
    }

    this.saveCart();

    if (event) {
      this.triggerFly(event, product.imageUrl);
    } else {
      this.showNotification(`已将 ${product.name} 加入购物车`);
    }
  },

  decreaseItem(productId) {
    const item = this.items.find((item) => item.id === productId);
    if (item) {
      if (item.quantity > 1) {
        item.quantity--;
      } else {
        this.items = this.items.filter((i) => i.id !== productId);
      }
      this.saveCart();
    }
  },

  removeItem(productId) {
    this.items = this.items.filter((item) => item.id !== productId);
    this.saveCart();
  },

  getProductCount(productId) {
    const item = this.items.find((item) => item.id === productId);
    return item ? item.quantity : 0;
  },

  triggerFly(event, imgUrl) {
    const rect = event.target.getBoundingClientRect();
    this.flySignal.rect = {
      left: rect.left + rect.width / 2,
      top: rect.top + rect.height / 2,
    };
    this.flySignal.img = imgUrl;
    this.flySignal.id++;
  },

  clearCart() {
    this.items = [];
    this.saveCart();
  },

  saveCart() {
    localStorage.setItem("yuxian_cart", JSON.stringify(this.items));
  },

  login(user) {
    if (!user) {
      console.error("尝试登录无效用户");
      return;
    }
    this.currentUser = user;
    localStorage.setItem("yuxian_user", JSON.stringify(user));

    const name = user.displayName || user.username || "用户";
    this.showNotification(`欢迎回来，${name}！`);
  },

  logout() {
    this.currentUser = null;
    this.items = [];
    localStorage.removeItem("yuxian_user");
    localStorage.removeItem("yuxian_cart");
    localStorage.removeItem("yuxian_token");
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
