const API_BASE = import.meta.env.VITE_API_BASE_URL;
const service = async (url, options = {}) => {
  const fullUrl = url.startsWith("http")
    ? url
    : `${API_BASE}${url.startsWith("/") ? url : "/" + url}`;

  const token = localStorage.getItem("yuxian_token");
  const headers = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  try {
    const response = await fetch(fullUrl, { ...options, headers });
    if (!response.ok) {
      const errorText = await response.text();
      try {
        const json = JSON.parse(errorText);
        throw new Error(json.message || `错误 ${response.status}`);
      } catch (e) {
        throw new Error(errorText || `请求失败: ${response.status}`);
      }
    }

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

const methodHelper = {
  get: (url) => service(url, { method: "GET" }),
  post: (url, data) =>
    service(url, { method: "POST", body: JSON.stringify(data) }),
  put: (url, data) =>
    service(url, { method: "PUT", body: JSON.stringify(data) }),
  del: (url) => service(url, { method: "DELETE" }),
  call: service,
};

const requestFn = (url, options) => service(url, options);

requestFn.get = methodHelper.get;
requestFn.post = methodHelper.post;
requestFn.put = methodHelper.put;
requestFn.delete = methodHelper.del;

export const request = requestFn;
export default requestFn;
