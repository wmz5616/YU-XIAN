import { createRouter, createWebHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import ProductDetailView from "../views/ProductDetailView.vue";
import CartView from "../views/CartView.vue";
import ProfileView from "../views/ProfileView.vue";
import LoginView from "../views/LoginView.vue";
import RegisterView from "../views/RegisterView.vue";
import CheckoutView from "../views/CheckoutView.vue";
import PaymentSuccessView from "../views/PaymentSuccessView.vue";
import NotFoundView from "../views/NotFoundView.vue";

import NProgress from "nprogress";
import "nprogress/nprogress.css";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    } else {
      return { top: 0, behavior: "smooth" };
    }
  },
  routes: [
    {
      path: "/",
      name: "home",
      component: HomeView,
      meta: { title: "首页" },
    },
    {
      path: "/product/:id",
      name: "product-detail",
      component: ProductDetailView,
      meta: { title: "商品详情" },
    },
    {
      path: "/cart",
      name: "cart",
      component: CartView,
      meta: { title: "购物车" },
    },
    {
      path: "/login",
      name: "login",
      component: LoginView,
      meta: { title: "会员登录" },
    },
    {
      path: "/register",
      name: "register",
      component: RegisterView,
      meta: { title: "注册会员" },
    },
    {
      path: "/profile",
      name: "profile",
      component: ProfileView,
      meta: { title: "个人中心" },
    },
    {
      path: "/checkout",
      name: "checkout",
      component: CheckoutView,
      meta: { title: "确认订单" },
    },
    {
      path: "/payment-success",
      name: "payment-success",
      component: PaymentSuccessView,
      meta: { title: "支付成功" },
    },
    {
      path: "/:pathMatch(.*)*",
      name: "not-found",
      component: NotFoundView,
      meta: { title: "页面未找到" },
    },
    {
      path: "/admin",
      name: "admin",
      component: () => import("../views/AdminView.vue"),
      meta: { requiresAdmin: true },
    }
  ],
});

NProgress.configure({ showSpinner: false });

router.beforeEach((to, from, next) => {
  NProgress.start();

  const siteTitle = "渔鲜直供";
  document.title = to.meta.title
    ? `${to.meta.title} - ${siteTitle}`
    : siteTitle;

  next();
});

router.afterEach(() => {
  NProgress.done();
});

export default router;
