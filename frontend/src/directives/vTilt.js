import VanillaTilt from 'vanilla-tilt'

export const vTilt = {
  mounted(el, binding) {
    VanillaTilt.init(el, {
      max: 15,
      speed: 400,
      glare: true,
      "max-glare": 0.4,
      scale: 1.05,
      perspective: 1000,
      ...binding.value
    })
  },
  unmounted(el) {

    if (el.vanillaTilt) {
      el.vanillaTilt.destroy()
    }
  }
}