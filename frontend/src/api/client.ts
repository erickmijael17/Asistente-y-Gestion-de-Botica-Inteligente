import axios from 'axios';

export const EVENTO_SESION_EXPIRADA = 'botica:sesion-expirada';

function resolveApiBaseUrl(): string {
  const raw = import.meta.env.VITE_API_URL ?? '/api';
  return raw.replace(/\/$/, '');
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
      window.dispatchEvent(new Event(EVENTO_SESION_EXPIRADA));
    }
    return Promise.reject(error);
  },
);

export default api;
