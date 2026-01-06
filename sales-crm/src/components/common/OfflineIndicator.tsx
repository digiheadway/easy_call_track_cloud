import React, { useState, useEffect } from 'react';
import { WifiOff, Wifi } from 'lucide-react';

const OfflineIndicator: React.FC = () => {
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [showOfflineMessage, setShowOfflineMessage] = useState(false);

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      setShowOfflineMessage(false);
    };

    const handleOffline = () => {
      setIsOnline(false);
      setShowOfflineMessage(true);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  if (isOnline && !showOfflineMessage) {
    return null;
  }

  return (
    <div className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
      isOnline ? 'bg-green-600' : 'bg-red-600'
    }`}>
      <div className="flex items-center justify-center py-2 px-4">
        <div className="flex items-center text-white text-sm">
          {isOnline ? (
            <>
              <Wifi className="w-4 h-4 mr-2" />
              <span>Back online</span>
            </>
          ) : (
            <>
              <WifiOff className="w-4 h-4 mr-2" />
              <span>You're offline. Some features may be limited.</span>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default OfflineIndicator;