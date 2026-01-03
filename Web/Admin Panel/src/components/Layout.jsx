import { useState, useEffect } from 'react';
import Sidebar from './Sidebar';
import PersonDetailDrawer from './PersonDetailDrawer';
import { Menu } from 'lucide-react';

export default function Layout({ children }) {
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
                    <span className="font-bold text-xl text-blue-600">CallCloud</span>
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
                {children}
            </main>

            <PersonDetailDrawer />
        </div>
    );
}
