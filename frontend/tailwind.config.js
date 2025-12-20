/** @type {import('tailwindcss').Config} */
export default {
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
      },

      transitionTimingFunction: {
        'spring': 'cubic-bezier(0.175, 0.885, 0.32, 1.275)',
      },
      // 2. 添加有色光晕阴影 (Blue / Orange)
      boxShadow: {
        'glow-blue': '0 10px 25px -5px rgba(59, 130, 246, 0.5), 0 8px 10px -6px rgba(59, 130, 246, 0.1)',
        'glow-orange': '0 10px 25px -5px rgba(249, 115, 22, 0.5), 0 8px 10px -6px rgba(249, 115, 22, 0.1)',
      },
      // 3. 补充关键帧动画
      animation: {
        'fade-in-up': 'fadeInUp 0.5s ease-out forwards',
        'bounce-slow': 'bounce-slow 3s infinite',
      },
      keyframes: {
        fadeInUp: {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'bounce-slow': {
          '0%, 100%': { transform: 'translateY(-5%)' },
          '50%': { transform: 'translateY(5%)' },
        }
      }
    },
  },
  plugins: [],
}