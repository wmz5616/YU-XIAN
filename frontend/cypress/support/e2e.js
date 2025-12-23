import "./commands";

beforeEach(() => {
  const style = document.createElement("style");
  style.innerHTML = `
    .vue-devtools__anchor-btn, .vue-devtools__panel { 
      display: none !important; 
      visibility: hidden !important; 
      pointer-events: none !important;
    }
  `;
  document.head.appendChild(style);
});
