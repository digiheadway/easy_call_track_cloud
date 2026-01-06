import React, { useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import VerificationModal from './VerificationModal';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const { isAuthenticated, showVerificationModal, openVerificationModal, closeVerificationModal } = useAuth();

  useEffect(() => {
    // If not authenticated, show verification modal
    if (!isAuthenticated) {
      openVerificationModal();
    }
  }, [isAuthenticated, openVerificationModal]);

  // Show loading state while checking authentication
  if (!isAuthenticated) {
    return (
      <>
        <div className="min-h-screen bg-gray-50 flex items-center justify-center">
          <div className="text-center">
            <div className="w-16 h-16 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
            <h2 className="text-xl font-semibold text-gray-900 mb-2">
              Verifying Access
            </h2>
            <p className="text-gray-600">
              Please complete the verification process to continue.
            </p>
          </div>
        </div>
        <VerificationModal 
          isOpen={showVerificationModal} 
          onClose={closeVerificationModal} 
        />
      </>
    );
  }

  // If authenticated, render children
  return <>{children}</>;
};

export default ProtectedRoute;
