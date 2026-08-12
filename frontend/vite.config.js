import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// El puerto 5173 es el que el backend permite en su configuracion de CORS.
// Si se cambia aqui, hay que cambiarlo tambien en CORS_ALLOWED_ORIGINS del backend.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
})
