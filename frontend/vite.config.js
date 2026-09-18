import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server:{
    host: '0.0.0.0',
    port: 7001,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 1800000,
        proxyTimeout: 1800000,
      },
    },
  },
})
