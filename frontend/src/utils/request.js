import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL;

const service = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('yuxian_token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    console.error('Request Error:', error);
    return Promise.reject(error);
  }
);

service.interceptors.response.use(
  response => {
    return response.data;
  },
  error => {
    console.error('API Error:', error);
    const message = error.response?.data?.message || error.message || '请求失败';
    return Promise.reject(new Error(message));
  }
);

const requestFn = (url, options = {}) => {
  const { body, ...rest } = options;
  const config = {
    url,
    ...rest
  };

  if (body && !config.data) {
    config.data = body;
  }

  return service(config);
};

requestFn.get = (url, config) => service.get(url, config);
requestFn.post = (url, data, config) => service.post(url, data, config);
requestFn.put = (url, data, config) => service.put(url, data, config);
requestFn.delete = (url, config) => service.delete(url, config);

export const request = requestFn;
export default requestFn;
