import { useState, useEffect } from 'react';
import Sidebar from './Sidebar';
import PersonDetailDrawer from './PersonDetailDrawer';
import { Menu, AlertTriangle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Layout({ children }) {
    const { user } = useAuth();
    const [isCollapsed, setIsCollapsed] = useState(false);
    const [isMobileOpen, setIsMobileOpen] = useState(false);
    const [isMobile, setIsMobile] = useState(false);

    useEffect(() => {
        const handleResize = () => {
            const mobile = window.innerWidth < 1024;
            setIsMobile(mobile);
            if (mobile) {
                setIsCollapsed(false); // Reset collapse on mobile as we use drawer
            }
        };

        handleResize(); // Initial check
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    return (
        <div className="flex bg-gray-50 dark:bg-gray-900 min-h-screen transition-colors duration-300">
            {/* Mobile Header */}
            {isMobile && (
                <div className="fixed top-0 left-0 right-0 h-16 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 z-30 px-4 flex items-center justify-between lg:hidden">
                    <button
                        onClick={() => setIsMobileOpen(true)}
                        className="p-2 -ml-2 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors"
                    >
                        <Menu size={24} />
                    </button>
                    <span className="font-bold text-xl text-blue-600">MiniClick Calls</span>
                    <div className="w-8"></div>
                </div>
            )}

            <Sidebar
                isCollapsed={isCollapsed}
                toggleCollapse={() => setIsCollapsed(!isCollapsed)}
                isMobileOpen={isMobileOpen}
                setIsMobileOpen={setIsMobileOpen}
                isMobile={isMobile}
            />

            <main
                className={`
                    flex-1 p-4 lg:p-8 transition-all duration-300 ease-in-out min-w-0
                    ${isMobile ? 'mt-16' : ''}
                    ${!isMobile && isCollapsed ? 'ml-20' : 'lg:ml-64'}
                `}
            >
                {/* Verification Warning Strip */}
                {user && (Number(user.is_verified) === 0) && (
                    <div className="mb-6 p-4 rounded-xl bg-orange-500/10 border border-orange-500/20 text-orange-600 dark:text-orange-400 flex items-center gap-3 animate-in slide-in-from-top-2">
                        <AlertTriangle size={20} className="flex-shrink-0" />
                        <div className="flex-1 text-sm font-medium">
                            Your email address is not verified. Please check your inbox to verify your account.
                        </div>
                    </div>
                )}

                {children}
            </main>

            <PersonDetailDrawer />
        </div>
    );
}
