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

// 2. 挂载快捷方法 (模仿 Axios)
const request = {
  // 让 request(url) 直接生效 (兼容旧代码)
  ...((url, options) => service(url, options)),
  
  // 显式方法
  get: (url) => service(url, { method: 'GET' }),
  post: (url, data) => service(url, { method: 'POST', body: JSON.stringify(data) }),
  put: (url, data) => service(url, { method: 'PUT', body: JSON.stringify(data) }),
  del: (url) => service(url, { method: 'DELETE' }),
  // 这一行让 request(...) 函数调用也能工作
  call: service 
};

// 关键：让 request 本身作为一个函数可调用，同时挂载 .get/.post
const requestFn = (url, options) => service(url, options);
requestFn.get = request.get;
requestFn.post = request.post;
requestFn.put = request.put;
requestFn.delete = request.del;

// 3. 只使用默认导出 (最稳妥的方式)
export default requestFn;