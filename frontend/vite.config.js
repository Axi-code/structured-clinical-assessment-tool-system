import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: [
      {
        find: /^element-plus$/,
        replacement: resolve(__dirname, 'src/vendor/element-plus.js')
      },
      {
        find: '@',
        replacement: resolve(__dirname, 'src')
      }
    ]
  },
  define: {
    global: 'globalThis',
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  optimizeDeps: {
    esbuildOptions: {
      define: {
        global: 'globalThis'
      }
    }
  }
})

