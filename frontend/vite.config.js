import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import importToCDN from 'vite-plugin-cdn-import'

export default defineConfig({
  plugins: [
    vue(),
    importToCDN({
      modules: [
        {
          name: 'vue',
          var: 'Vue',
          path: 'https://cdn.jsdelivr.net/npm/vue@3.4.21/dist/vue.global.prod.js'
        },
        {
          name: 'element-plus',
          var: 'ElementPlus',
          path: 'https://cdn.jsdelivr.net/npm/element-plus@2.6.1/dist/index.full.min.js',
          css: 'https://cdn.jsdelivr.net/npm/element-plus@2.6.1/dist/index.min.css'
        },
        {
          name: 'vue-router',
          var: 'VueRouter',
          path: 'https://cdn.jsdelivr.net/npm/vue-router@4.3.0/dist/vue-router.global.prod.js'
        },
        {
          name: 'vue-demi',
          var: 'VueDemi',
          path: 'https://cdn.jsdelivr.net/npm/vue-demi@0.14.6/lib/index.iife.js'
        },
        {
          name: 'pinia',
          var: 'Pinia',
          path: 'https://cdn.jsdelivr.net/npm/pinia@2.1.7/dist/pinia.iife.prod.js'
        },
        {
          name: 'axios',
          var: 'axios',
          path: 'https://cdn.jsdelivr.net/npm/axios@1.6.7/dist/axios.min.js'
        }
      ]
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true
      }
    }
  },
  build: {
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks: {
          'monaco-editor': ['monaco-editor']
        }
      }
    }
  }
})
