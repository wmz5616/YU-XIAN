import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ProductDetailView from '../views/ProductDetailView.vue'
import CartView from '../views/CartView.vue'
import ProfileView from '../views/ProfileView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import CheckoutView from '../views/CheckoutView.vue'
import PaymentSuccessView from '../views/PaymentSuccessView.vue'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/product/:id',
      name: 'product-detail',
      component: ProductDetailView
    },
    {
      path: '/cart',
      name: 'cart',
      component: CartView
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView
    },
    {
      path: '/profile',
      name: 'profile',
      component: ProfileView
    },
    {
      path: '/checkout',
      name: 'checkout',
      component: CheckoutView
    },
    {
      path: '/payment-success',
      name: 'payment-success',
      component: PaymentSuccessView
    }
  ]
})

NProgress.configure({ showSpinner: false }) // 去掉右边的转圈圈，只要进度条

// 开始跳转前
router.beforeEach((to, from, next) => {
  NProgress.start()
  next()
})

// 跳转结束后
router.afterEach(() => {
  NProgress.done()
})

export default router