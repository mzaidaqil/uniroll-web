import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // In development the browser calls /api on this server and Vite forwards it to Spring Boot,
    // so the browser sees one origin and no CORS setup is needed
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
