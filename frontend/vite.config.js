import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/ws/chat':         { target: 'http://localhost:8081', ws: true, changeOrigin: true },
      '/api/v1/resumes': { target: 'http://localhost:8081', changeOrigin: true },
      '/api/v1/match':   { target: 'http://localhost:8081', changeOrigin: true },
      '/api/v1/ai':      { target: 'http://localhost:8082', changeOrigin: true },
      '/api/v1/latex':   { target: 'http://localhost:8083', changeOrigin: true },
      '/api/v1/auth':    { target: 'http://localhost:8084', changeOrigin: true },
    },
  },
})
