import axios from 'axios';

const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

// Attach JWT to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('ff_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Global error handling
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('ff_token');
      localStorage.removeItem('ff_user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export default api;

// ── Helpers ──────────────────────────────────────────────────────────────────
export const extractError = (err) => {
  const d = err?.response?.data;
  if (!d) return err?.message || 'Something went wrong. Please try again.';
  if (typeof d === 'string') return d;
  if (d.message) return d.message;
  if (d.fieldErrors?.length) return d.fieldErrors.map(f => `${f.field}: ${f.message}`).join(' | ');
  return 'Something went wrong.';
};
