import './commands'

// 在每个测试前运行，隐藏遮挡的 DevTools 按钮
beforeEach(() => {
  // 注入样式强制隐藏 Vue DevTools 的锚点按钮
  const style = document.createElement('style');
  style.innerHTML = `
    .vue-devtools__anchor-btn, .vue-devtools__panel { 
      display: none !important; 
      visibility: hidden !important; 
      pointer-events: none !important;
    }
  `;
  document.head.appendChild(style);
});