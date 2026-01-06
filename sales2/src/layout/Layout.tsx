import React, { useState, useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Sidebar from '../components/common/Sidebar';
import Header from '../components/common/Header';
import LoadingBar from '../components/ui/LoadingBar';
import OfflineIndicator from '../components/common/OfflineIndicator';
import { useAppContext } from '../contexts/AppContext';

const Layout: React.FC = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const location = useLocation();
  const { isLoading } = useAppContext();

  const getPageTitle = () => {
    const path = location.pathname;
    
    if (path === '/') return 'Dashboard';
    if (path === '/leads') return 'Leads';
    if (path.startsWith('/leads/')) return 'Lead Details';
    if (path === '/tasks') return 'Tasks';
    if (path === '/calendar') return 'Calendar';
    if (path === '/activities') return 'Activities';
    
    return 'Real Estate CRM';
  };
  
  // Close sidebar on route change (mobile only)
  useEffect(() => {
    setIsSidebarOpen(false);
  }, [location]);

  const showFilterButton = location.pathname === '/leads' || location.pathname === '/tasks';

  return (
    <div className="h-screen flex overflow-hidden bg-gray-100">
      {isLoading && <LoadingBar />}
      <OfflineIndicator />
      
      {/* Sidebar overlay for mobile */}
      {isSidebarOpen && (
        <div 
          className="fixed inset-0 z-20 bg-black bg-opacity-50 md:hidden"
          onClick={() => setIsSidebarOpen(false)}
        ></div>
      )}
      
      {/* Sidebar */}
      <Sidebar 
        isOpen={isSidebarOpen} 
        onClose={() => setIsSidebarOpen(false)}
        companyName="Uptown Properties"
      />
      
      {/* Main content */}
      <div className="flex flex-col w-0 flex-1 overflow-hidden">
        <Header 
          title={getPageTitle()} 
          onOpenSidebar={() => setIsSidebarOpen(true)}
          showFilter={showFilterButton}
        />
        
        <main className="flex-1 relative overflow-y-auto focus:outline-none">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;