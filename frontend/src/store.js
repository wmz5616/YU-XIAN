import { reactive } from "vue";

const savedUser = localStorage.getItem("yuxian_user");
const initialUser = savedUser ? JSON.parse(savedUser) : null;
const savedCart = localStorage.getItem("yuxian_cart");
const initialCart = savedCart ? JSON.parse(savedCart) : [];

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
    this.currentUser = user;
    localStorage.setItem("yuxian_user", JSON.stringify(user));
    this.showNotification(`欢迎回来，${user.displayName}！`);
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
