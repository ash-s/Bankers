import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react()],
    server: {
      // Open the app in browser at http://localhost:8081
      port: Number(env.VITE_PORT || 8081),
      strictPort: true,
      proxy: {
        '/api': {
          target: env.VITE_API_TARGET || 'http://localhost:8082',
          changeOrigin: true,
        },
      },
    },
  }
})
