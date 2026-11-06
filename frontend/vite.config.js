import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // during development requests to /api go to the Spring Boot backend
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
