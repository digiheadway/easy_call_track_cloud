import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.tsx';
import './index.css';
import { registerSW } from 'virtual:pwa-register';
import toast from 'react-hot-toast';

// Register service worker with enhanced offline support
const updateSW = registerSW({
  onNeedRefresh() {
    toast((t) => (
      <div className="flex items-center gap-2">
        <span>New version available!</span>
        <button
          className="px-2 py-1 bg-blue-600 text-white rounded"
          onClick={() => {
            updateSW(true);
            toast.dismiss(t.id);
          }}
        >
          Update
        </button>
      </div>
    ), {
      duration: Infinity,
      position: 'bottom-center'
    });
  },
  onOfflineReady() {
    toast.success('App ready for offline use', {
      position: 'bottom-center',
      duration: 3000
    });
  },
  onRegisteredSW(swUrl, r) {
    r && setInterval(async () => {
      if (!(!r.installing && navigator.onLine)) return;
      const resp = await fetch(swUrl, {
        cache: 'no-store',
        headers: {
          'cache': 'no-store',
          'cache-control': 'no-cache',
        },
      });
      if (resp?.status === 200) await r.update();
    }, 60 * 60 * 1000); // Check for updates every hour
  }
});

// Handle offline/online status
window.addEventListener('online', () => {
  toast.success('Back online!', { position: 'bottom-center' });
});

window.addEventListener('offline', () => {
  toast.error('You are offline', { position: 'bottom-center' });
});

// Prevent bounce effect on iOS
document.body.addEventListener('touchmove', (e) => {
  if (e.target === document.body) {
    e.preventDefault();
  }
}, { passive: false });

// Handle standalone mode detection
if (window.matchMedia('(display-mode: standalone)').matches) {
  document.documentElement.classList.add('standalone');
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
);