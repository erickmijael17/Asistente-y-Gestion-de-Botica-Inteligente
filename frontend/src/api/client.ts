import axios from 'axios';

/** Base API sin sufijo /v1 — auth usa /auth/* y recursos usan /v1/* */
function resolveApiBaseUrl(): string {
  const raw = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';
  return raw.replace(/\/v1\/?$/, '').replace(/\/$/, '');
}

const api = axios.create({
  baseURL: resolveApiBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      localStorage.removeItem('roles');
      localStorage.removeItem('userId');
    }
    return Promise.reject(error);
  },
);

export default api;
