/** @type {import('tailwindcss').Config} */
export default {
  // ✅ 核心修改：开启 class 模式
  darkMode: 'class', 
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        'serif-sc': ['Noto Serif SC', 'serif'],
      }
    },
  },
  plugins: [],
}