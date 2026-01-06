import React, { useState, useEffect } from 'react';
import { RefreshCw, X } from 'lucide-react';

const PWAUpdatePrompt: React.FC = () => {
  const [showUpdatePrompt, setShowUpdatePrompt] = useState(false);
  const [updateSW, setUpdateSW] = useState<((reloadPage?: boolean) => Promise<void>) | null>(null);

  useEffect(() => {
    // This will be set by the service worker registration
    const handleSWUpdate = (event: CustomEvent) => {
      setUpdateSW(() => event.detail.updateSW);
      setShowUpdatePrompt(true);
    };

    window.addEventListener('sw-update-available', handleSWUpdate as EventListener);

    return () => {
      window.removeEventListener('sw-update-available', handleSWUpdate as EventListener);
    };
  }, []);

  const handleUpdate = async () => {
    if (updateSW) {
      await updateSW(true);
    }
  };

  const handleDismiss = () => {
    setShowUpdatePrompt(false);
  };

  if (!showUpdatePrompt) {
    return null;
  }

  return (
    <div className="fixed top-4 left-4 right-4 md:left-auto md:right-4 md:max-w-sm z-50">
      <div className="bg-white rounded-lg shadow-lg border border-gray-200 p-4">
        <div className="flex items-start justify-between">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <div className="w-10 h-10 bg-green-600 rounded-lg flex items-center justify-center">
                <RefreshCw className="w-5 h-5 text-white" />
              </div>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-gray-900">
                Update Available
              </h3>
              <p className="text-xs text-gray-500 mt-1">
                A new version is ready to install
              </p>
            </div>
          </div>
          <button
            onClick={handleDismiss}
            className="flex-shrink-0 ml-2 text-gray-400 hover:text-gray-500"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
        <div className="mt-3 flex space-x-2">
          <button
            onClick={handleUpdate}
            className="flex-1 bg-green-600 text-white text-xs font-medium py-2 px-3 rounded-md hover:bg-green-700 transition-colors"
          >
            Update Now
          </button>
          <button
            onClick={handleDismiss}
            className="flex-1 bg-gray-100 text-gray-700 text-xs font-medium py-2 px-3 rounded-md hover:bg-gray-200 transition-colors"
          >
            Later
          </button>
        </div>
      </div>
    </div>
  );
};

export default PWAUpdatePrompt;