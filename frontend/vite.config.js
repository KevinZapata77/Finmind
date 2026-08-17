import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// El puerto 5173 es el unico que el backend acepta en su configuracion de CORS
// (CORS_ALLOWED_ORIGINS en el .env del backend).
//
// strictPort: si 5173 esta ocupado, Vite FALLA en vez de saltar al 5174.
// Antes saltaba en silencio y la aplicacion cargaba pero ninguna llamada a la
// API funcionaba, con un error de "no pudimos conectar" que no decia la causa
// real. Es mejor que no arranque y lo diga.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true },
})
