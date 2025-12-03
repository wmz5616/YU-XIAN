// 这是一个 Vue 自定义指令
export const vScrollReveal = {
  mounted: (el) => {
    // 1. 先给元素加上“隐藏”状态
    el.classList.add('reveal-hidden')

    // 2. 创建一个观察者 (IntersectionObserver)
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        // 如果元素进入了视口 (isIntersecting)
        if (entry.isIntersecting) {
          // 加上“激活”状态，触发 CSS 动画
          el.classList.add('reveal-active')
          // 动画只要一次，触发后就不再观察了
          observer.unobserve(el)
        }
      })
    }, {
      threshold: 0.1, // 元素出现 10% 时触发
      rootMargin: "0px 0px -50px 0px" // 稍微提前一点触发
    })

    // 3. 开始观察
    observer.observe(el)
  }
}