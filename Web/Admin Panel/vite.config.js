import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'
import fs from 'fs'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    // Custom plugin for SPA fallback on dashboard routes
    {
      name: 'spa-fallback',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          // If the request is for a dashboard route (not a file with extension)
          if (req.url.startsWith('/dashboard') && !req.url.includes('.')) {
            req.url = '/dashboard/index.html';
          }
          next();
        });
      }
    }
  ],
  base: '/',
  build: {
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'index.html'),
        dashboard: resolve(__dirname, 'dashboard/index.html'),
      },
    },
  },
})
