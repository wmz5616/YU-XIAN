const API_BASE = import.meta.env.VITE_API_BASE_URL;

/**
 * 通用请求封装
 * @param {string} url
 * @param {object} options
 */
export const request = async (url, options = {}) => {
  const fullUrl = `${API_BASE}${url.startsWith("/") ? url : "/" + url}`;

  const defaultHeaders = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  const config = {
    ...options,
    headers: defaultHeaders,
  };

  try {
    const response = await fetch(fullUrl, config);
    if (!response.ok) {
      throw new Error(`HTTP Error: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error("API请求失败:", error);
    throw error;
  }
};
