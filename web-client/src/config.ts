// Runtime env (Docker) takes precedence over build-time env (Vite dev).
export const API_URL =
  window.__ENV__?.PUBLIC_API_URL ??
  import.meta.env.VITE_API_URL ??
  'http://localhost:8080'
