import VanillaTilt from 'vanilla-tilt'

export const vTilt = {
  mounted(el, binding) {
    // 初始化 3D 倾斜效果
    VanillaTilt.init(el, {
      max: 15,            // 最大倾斜角度 (度)
      speed: 400,         // 转换速度 (越小越快)
      glare: true,        // 开启炫光效果
      "max-glare": 0.4,   // 炫光透明度 (0-1)
      scale: 1.05,        // 悬浮时微缩放 (1.05倍)
      perspective: 1000,  // 透视深度
      ...binding.value    // 允许外部传入自定义配置
    })
  },
  unmounted(el) {
    // 组件销毁时清理，防止内存泄漏
    if (el.vanillaTilt) {
      el.vanillaTilt.destroy()
    }
  }
}