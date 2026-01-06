import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { toast } from 'react-toastify';

interface AuthContextType {
  isAuthenticated: boolean;
  isVerifying: boolean;
  showVerificationModal: boolean;
  verifyCode: (code: string) => Promise<boolean>;
  logout: () => void;
  openVerificationModal: () => void;
  closeVerificationModal: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const VERIFICATION_API_URL = 'https://prop.digiheadway.in/api/2fa/';
const SESSION_COOKIE_NAME = 'sales_auth_session';
const SESSION_DURATION = 3 * 24 * 60 * 60 * 1000; // 3 days in milliseconds

// Cookie utility functions
const setCookie = (name: string, value: string, days: number) => {
  const expires = new Date();
  expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);
  document.cookie = `${name}=${value};expires=${expires.toUTCString()};path=/;SameSite=Strict`;
};

const getCookie = (name: string): string | null => {
  const nameEQ = name + "=";
  const ca = document.cookie.split(';');
  for (let i = 0; i < ca.length; i++) {
    let c = ca[i];
    while (c.charAt(0) === ' ') c = c.substring(1, c.length);
    if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length);
  }
  return null;
};

const deleteCookie = (name: string) => {
  document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/;`;
};

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);
  const [showVerificationModal, setShowVerificationModal] = useState(false);

  // Check authentication status on mount
  useEffect(() => {
    const sessionData = getCookie(SESSION_COOKIE_NAME);
    if (sessionData) {
      try {
        const session = JSON.parse(sessionData);
        const now = Date.now();
        
        // Check if session is still valid (within 3 days)
        if (session.timestamp && (now - session.timestamp) < SESSION_DURATION) {
          setIsAuthenticated(true);
        } else {
          // Session expired, clear it
          deleteCookie(SESSION_COOKIE_NAME);
          setIsAuthenticated(false);
        }
      } catch (error) {
        // Invalid session data, clear it
        deleteCookie(SESSION_COOKIE_NAME);
        setIsAuthenticated(false);
      }
    } else {
      setIsAuthenticated(false);
    }
  }, []);

  // Set up session expiration check
  useEffect(() => {
    if (isAuthenticated) {
      const checkSessionExpiration = () => {
        const sessionData = getCookie(SESSION_COOKIE_NAME);
        if (sessionData) {
          try {
            const session = JSON.parse(sessionData);
            const now = Date.now();
            
            if (session.timestamp && (now - session.timestamp) >= SESSION_DURATION) {
              // Session expired
              logout();
              toast.info('Your session has expired. Please verify again.');
            }
          } catch (error) {
            logout();
          }
        }
      };

      // Check every hour
      const interval = setInterval(checkSessionExpiration, 60 * 60 * 1000);
      
      return () => clearInterval(interval);
    }
  }, [isAuthenticated]);

  const verifyCode = async (code: string): Promise<boolean> => {
    setIsVerifying(true);
    
    try {
      const response = await fetch(`${VERIFICATION_API_URL}?code=${code}`);
      
      if (!response.ok) {
        throw new Error('Network error');
      }
      
      const result = await response.text();
      
      if (result.trim() === 'valid') {
        // Create session data
        const sessionData = {
          timestamp: Date.now(),
          verified: true
        };
        
        // Set cookie for 3 days
        setCookie(SESSION_COOKIE_NAME, JSON.stringify(sessionData), 3);
        
        setIsAuthenticated(true);
        setShowVerificationModal(false);
        toast.success('Verification successful!');
        return true;
      } else {
        toast.error('Invalid verification code. Please try again.');
        return false;
      }
    } catch (error) {
      console.error('Verification error:', error);
      toast.error('Verification failed. Please check your connection and try again.');
      return false;
    } finally {
      setIsVerifying(false);
    }
  };

  const logout = () => {
    setIsAuthenticated(false);
    deleteCookie(SESSION_COOKIE_NAME);
    setShowVerificationModal(false);
    toast.info('You have been logged out.');
  };

  const openVerificationModal = () => {
    setShowVerificationModal(true);
  };

  const closeVerificationModal = () => {
    setShowVerificationModal(false);
  };

  const contextValue: AuthContextType = {
    isAuthenticated,
    isVerifying,
    showVerificationModal,
    verifyCode,
    logout,
    openVerificationModal,
    closeVerificationModal,
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
