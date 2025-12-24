import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',          // 允许外部访问
    port: 5173,
    // allowedHosts: ['6b2ea6e4.r17.cpolar.top'], // 写你的前端穿透域名
    proxy: {
      // 代理后端 API，解决跨域
      '/api': {
        // target: 'https://59b3169e.r17.cpolar.top',  // 后端穿透地址（使用穿透时启用）
        target: 'http://localhost:8081',  // 本地开发时使用这个
        changeOrigin: true,
        secure: false  // 本地开发使用 HTTP，设置为 false
      }
    }
  },
  resolve: {
    alias: {
      '@': '/src'
    }
  }
})

