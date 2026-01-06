import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.tsx';
import './index.css';
import { registerSW } from 'virtual:pwa-register';

// Enhanced PWA registration with update handling
const updateSW = registerSW({
  onNeedRefresh() {
    // Dispatch custom event for update prompt
    window.dispatchEvent(new CustomEvent('sw-update-available', {
      detail: { updateSW }
    }));
  },
  onOfflineReady() {
    console.log('App ready to work offline');
    // You could show a toast notification here
  },
  onRegistered(r) {
    console.log('SW Registered: ' + r);
  },
  onRegisterError(error) {
    console.log('SW registration error', error);
  }
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>
);