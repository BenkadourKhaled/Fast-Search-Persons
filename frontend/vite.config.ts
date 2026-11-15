import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath, URL } from 'node:url';

// 8080 par défaut ; surcharge possible via .env ou variable d'environnement.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const backend =
    env.FASTSEARCH_BACKEND_URL || process.env.FASTSEARCH_BACKEND_URL || 'http://localhost:8080';

  const proxy = {
    '/api': {
      target: backend,
      changeOrigin: true,
    },
    '/actuator': {
      target: backend,
      changeOrigin: true,
    },
  };

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 5173,
      proxy,
    },
    preview: {
      port: 4173,
      proxy,
    },
    optimizeDeps: {
      // Pré-bundler Lucide évite des centaines de requêtes de modules en dev.
      include: ['lucide-react'],
    },
  };
});
