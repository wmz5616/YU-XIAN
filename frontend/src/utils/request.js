// src/utils/request.js
const API_BASE = import.meta.env.VITE_API_BASE_URL;

// 1. 定义核心函数
const service = async (url, options = {}) => {
  // 处理 URL
  const fullUrl = url.startsWith("http") 
    ? url 
    : `${API_BASE}${url.startsWith("/") ? url : "/" + url}`;

  // 处理 Headers (特别是 Token)
  const token = localStorage.getItem('yuxian_token');
  const headers = {
    "Content-Type": "application/json",
    ...(token ? { "Authorization": `Bearer ${token}` } : {}),
    ...options.headers,
  };

  try {
    const response = await fetch(fullUrl, { ...options, headers });
    
    // 错误处理
    if (!response.ok) {
      const errorText = await response.text();
      try {
        const json = JSON.parse(errorText);
        throw new Error(json.message || `错误 ${response.status}`);
      } catch (e) {
        throw new Error(errorText || `请求失败: ${response.status}`);
      }
    }

    // 响应解析
    const contentType = response.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
      return await response.json();
    }
    return await response.text();

  } catch (error) {
    console.error("API Error:", error);
    throw error;
  }
};

// 2. 定义辅助方法 (修改了变量名，避免冲突)
const methodHelper = {
  get: (url) => service(url, { method: 'GET' }),
  post: (url, data) => service(url, { method: 'POST', body: JSON.stringify(data) }),
  put: (url, data) => service(url, { method: 'PUT', body: JSON.stringify(data) }),
  del: (url) => service(url, { method: 'DELETE' }),
  call: service 
};

// 3. 构建主函数对象
const requestFn = (url, options) => service(url, options);

// 挂载快捷方法 (.get, .post 等)
requestFn.get = methodHelper.get;
requestFn.post = methodHelper.post;
requestFn.put = methodHelper.put;
requestFn.delete = methodHelper.del;

// === 导出部分 ===

// 1. 命名导出 (修复 HomeView.vue 等文件的 import { request } 报错)
export const request = requestFn;

// 2. 默认导出 (兼容其他写法)
export default requestFn;