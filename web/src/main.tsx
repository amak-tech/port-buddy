import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './index.css'
import { AuthProvider } from './auth/AuthContext'
import { LoadingProvider } from './components/LoadingContext'

const rootElement = document.getElementById('root')!;
const app = (
    <LoadingProvider>
      <AuthProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </AuthProvider>
    </LoadingProvider>
);

const prerenderedRoute = rootElement.getAttribute('data-prerendered-route');
const currentRoute = window.location.pathname.replace(/\/$/, '') || '/';
const shouldHydrate = rootElement.hasChildNodes() && (
  prerenderedRoute === currentRoute || 
  (currentRoute === '/' && prerenderedRoute === '/index') ||
  (currentRoute === '/index' && prerenderedRoute === '/')
);

if (shouldHydrate) {
  ReactDOM.hydrateRoot(rootElement, app);
} else {
  ReactDOM.createRoot(rootElement).render(app);
}
