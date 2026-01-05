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
        login: resolve(__dirname, 'login/index.html'),
        signup: resolve(__dirname, 'signup/index.html'),
        features: resolve(__dirname, 'features/index.html'),
        download: resolve(__dirname, 'download/index.html'),
        setup: resolve(__dirname, 'setup/index.html'),
        'setup-manager': resolve(__dirname, 'setup/manager/index.html'),
        'setup-employee': resolve(__dirname, 'setup/employee/index.html'),
        'forgot-password': resolve(__dirname, 'forgot-password/index.html'),
        'reset-password': resolve(__dirname, 'reset-password/index.html'),
        'verify-email': resolve(__dirname, 'verify-email/index.html'),
        privacy: resolve(__dirname, 'privacy/index.html'),
        terms: resolve(__dirname, 'terms/index.html'),
        blog: resolve(__dirname, 'blog/how-to-track-employee-calls/index.html'),
      },
    },
  },
})
