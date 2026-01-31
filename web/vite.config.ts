import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import Prerender from '@prerenderer/rollup-plugin'
import Renderer from '@prerenderer/renderer-puppeteer'
import path from 'path'
import fs from 'fs'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());

  return {
    define: {
      'process.env.NODE_ENV': JSON.stringify('development')
    },
    plugins: [
      react(),
      Prerender({
        routes: ['/', '/index', '/install', '/docs', '/docs/guides/minecraft-server', '/docs/guides/hytale-server', '/privacy', '/terms', '/contacts'],
        renderer: new Renderer({
          renderAfterDocumentEvent: 'render-event',
        }),
        staticDir: path.join(__dirname, 'dist'),
        postProcess(renderedRoute) {
          const route = renderedRoute.route;
          renderedRoute.html = renderedRoute.html.replace(
            '<div id="root">',
            `<div id="root" data-prerendered-route="${route}">`
          );

          let templatePath = '';

          // Check for specific guide html files
          if (route.startsWith('/docs/guides/')) {
            const guideName = route.substring('/docs/guides/'.length);
            const potentialPath = `public/pages/docs/guides/${guideName}.html`;
            if (fs.existsSync(path.join(__dirname, potentialPath))) {
              templatePath = potentialPath;
            }
          }

          if (!templatePath) {
            if (route === '/' || route === '/index') templatePath = 'public/pages/index.html';
            else if (route === '/install') templatePath = 'public/pages/install.html';
            else if (route === '/docs' || route.startsWith('/docs/')) templatePath = 'public/pages/docs.html';
            else if (route === '/privacy') templatePath = 'public/pages/privacy.html';
            else if (route === '/terms') templatePath = 'public/pages/terms.html';
            else if (route === '/contacts') templatePath = 'public/pages/contacts.html';
          }

          if (templatePath) {
            let template = fs.readFileSync(path.join(__dirname, templatePath), 'utf8');

            // Replace environment variables
            Object.keys(env).forEach((key) => {
              if (key.startsWith('VITE_')) {
                template = template.replace(new RegExp(`%${key}%`, 'g'), env[key]);
              }
            });

            const titleMatch = template.match(/<title>(.*?)<\/title>/);
            const headContent = template.match(/<head>([\s\S]*?)<\/head>/);

            if (titleMatch) {
              renderedRoute.html = renderedRoute.html.replace(/<title>.*?<\/title>/, titleMatch[0]);
            }
            if (headContent) {
              // Extract meta and link tags from template head
              const tags = headContent[1].match(/<(meta|link|script type="application\/ld\+json")[\s\S]*?>([\s\S]*?<\/(script)>)?/g);
              if (tags) {
                const headEndIndex = renderedRoute.html.indexOf('</head>');
                let newHead = renderedRoute.html.substring(0, headEndIndex);
                tags.forEach(tag => {
                  // Avoid duplicating tags if they already exist with same name/property/rel
                  // Simple check to avoid some common duplicates
                  const nameMatch = tag.match(/(name|property|rel|type)="([^"]+)"/);
                  if (nameMatch) {
                    const attr = nameMatch[1];
                    const val = nameMatch[2];
                    if (renderedRoute.html.includes(`${attr}="${val}"`)) {
                      // Replace existing tag if found (simplified)
                      const existingTagRegex = new RegExp(`<[^>]*${attr}="${val}"[^>]*>`, 'g');
                      newHead = newHead.replace(existingTagRegex, tag);
                      return;
                    }
                  }
                  newHead += `\n    ${tag}`;
                });
                renderedRoute.html = newHead + renderedRoute.html.substring(headEndIndex);
              }
            }
          }
          return renderedRoute;
        }
      }),
    ],
  build: {
    minify: false,
    sourcemap: true,
  },
};
});
