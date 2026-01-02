import { NavLink, useNavigate } from 'react-router-dom';
import {
    LayoutDashboard,
    BarChart2,
    Phone,
    Users,
    Settings,
    LogOut,
    Cloud,
    UserX,
    ChevronLeft,
    ChevronRight,
    X
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs) {
    return twMerge(clsx(inputs));
}

export default function Sidebar({
    isCollapsed,
    toggleCollapse,
    isMobileOpen,
    setIsMobileOpen,
    isMobile
}) {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const navItems = [
        { name: 'Dashboard', icon: LayoutDashboard, path: '/' },
        { name: 'Reports', icon: BarChart2, path: '/reports' },
        { name: 'Calls', icon: Phone, path: '/calls' },
        { name: 'Employees', icon: Users, path: '/employees' },
        { name: 'Exclusions', icon: UserX, path: '/excluded' },
        { name: 'Settings', icon: Settings, path: '/settings' },
    ];

    const sidebarContent = (
        <div className="flex flex-col h-full bg-white border-r border-gray-200">
            {/* Header */}
            <div className={cn("p-6 flex items-center h-20 transition-all duration-300", isCollapsed ? "justify-center px-2" : "justify-between")}>
                {/* Logo Area - Only visible when not collapsed or on mobile */}
                <div className={cn("flex items-center gap-2 text-blue-600 font-bold text-xl overflow-hidden transition-all duration-300",
                    (isCollapsed && !isMobile) ? "w-0 opacity-0 hidden" : "flex")}>
                    <Cloud size={24} className="flex-shrink-0" />
                    <span className="whitespace-nowrap">CallCloud</span>
                </div>

                {/* Desktop Toggle Button */}
                {!isMobile && (
                    <button
                        onClick={toggleCollapse}
                        className={cn("p-1.5 rounded-lg hover:bg-gray-100 text-gray-500 transition-colors",
                            isCollapsed ? "mx-auto" : "ml-auto"
                        )}
                        title={isCollapsed ? "Expand Sidebar" : "Collapse Sidebar"}
                    >
                        {isCollapsed ? <ChevronRight size={20} /> : <ChevronLeft size={20} />}
                    </button>
                )}

                {/* Mobile Close Button */}
                {isMobile && (
                    <button
                        onClick={() => setIsMobileOpen(false)}
                        className="p-1 rounded-md hover:bg-gray-100 text-gray-500 ml-auto"
                    >
                        <X size={20} />
                    </button>
                )}
            </div>

            {/* Navigation */}
            <nav className="flex-1 px-3 space-y-1 overflow-y-auto">
                {navItems.map((item) => (
                    <NavLink
                        key={item.name}
                        to={item.path}
                        onClick={() => isMobile && setIsMobileOpen(false)}
                        className={({ isActive }) => cn(
                            "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-200",
                            isActive
                                ? "bg-blue-50 text-blue-600"
                                : "text-gray-600 hover:bg-gray-50 hover:text-gray-900",
                            isCollapsed && "justify-center px-2"
                        )}
                        title={isCollapsed ? item.name : undefined}
                    >
                        <item.icon size={20} className="flex-shrink-0" />
                        <span className={cn("whitespace-nowrap transition-all duration-300", isCollapsed ? "w-0 opacity-0 hidden" : "block")}>
                            {item.name}
                        </span>
                    </NavLink>
                ))}
            </nav>

            {/* Footer */}
            <div className="p-4 border-t border-gray-100 mt-auto">
                <div className={cn("flex items-center gap-3 mb-4 px-2", isCollapsed && "justify-center flex-col")}>
                    <div className="w-9 h-9 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-xs flex-shrink-0">
                        {user?.name?.substring(0, 2).toUpperCase() || 'AD'}
                    </div>
                    <div className={cn("flex-1 min-w-0 transition-opacity duration-300", isCollapsed ? "opacity-0 w-0 hidden" : "opacity-100")}>
                        <p className="text-sm font-semibold truncate">{user?.name || 'Admin'}</p>
                        <p className="text-xs text-gray-500 truncate">{user?.email}</p>
                    </div>
                </div>
                <button
                    onClick={handleLogout}
                    className={cn(
                        "flex items-center gap-3 w-full px-3 py-2 rounded-lg text-sm font-medium text-gray-600 hover:bg-red-50 hover:text-red-600 transition-colors border border-transparent hover:border-red-100",
                        isCollapsed && "justify-center"
                    )}
                    title={isCollapsed ? "Logout" : undefined}
                >
                    <LogOut size={20} />
                    <span className={cn("whitespace-nowrap transition-all duration-300", isCollapsed ? "w-0 opacity-0 hidden" : "block")}>Logout</span>
                </button>
            </div>
        </div>
    );

    return (
        <>
            {/* Mobile Overlay */}
            {isMobile && isMobileOpen && (
                <div
                    className="fixed inset-0 bg-black/50 z-30 lg:hidden"
                    onClick={() => setIsMobileOpen(false)}
                />
            )}

            {/* Sidebar */}
            <aside
                className={cn(
                    "fixed top-0 bottom-0 left-0 z-40 bg-white transition-all duration-300 shadow-xl lg:shadow-none border-r border-gray-200",
                    isMobile
                        ? (isMobileOpen ? "translate-x-0 w-64" : "-translate-x-full w-64")
                        : (isCollapsed ? "w-20" : "w-64")
                )}
            >
                {sidebarContent}
            </aside>
        </>
    );
}
