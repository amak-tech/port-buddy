import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import Prerender from '@prerenderer/rollup-plugin'
import Renderer from '@prerenderer/renderer-puppeteer'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    Prerender({
      routes: ['/', '/index', '/install', '/docs', '/privacy', '/terms'],
      renderer: new Renderer({
        renderAfterDocumentEvent: 'render-event',
      }),
      staticDir: path.join(__dirname, 'dist'),
    }),
  ],
})
