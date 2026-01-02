import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { useState, useEffect } from 'react';
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
    CreditCard,
    HardDrive,
    X,
    Bell,
    ChevronDown,
    LayoutGrid
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
    const { user, logout, unreadNotificationsCount } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    // Manage Section Collapse State
    const [isManageOpen, setIsManageOpen] = useState(true);

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const mainItems = [
        { name: 'Dashboard', icon: LayoutDashboard, path: '/' },
        { name: 'Reports', icon: BarChart2, path: '/reports' },
        { name: 'Calls', icon: Phone, path: '/calls' },
    ];

    const manageItems = [
        { name: 'Employees', icon: Users, path: '/employees' },
        { name: 'Exclusions', icon: UserX, path: '/excluded' },
        { name: 'Storage', icon: HardDrive, path: '/storage' },
    ];

    const bottomItems = [
        { name: 'Notifications', icon: Bell, path: '/notifications' },
        { name: 'Plans', icon: CreditCard, path: '/plans' },
        { name: 'Settings', icon: Settings, path: '/settings' },
    ];

    // Open manage section automatically if we are on a manage page
    useEffect(() => {
        if (manageItems.some(item => location.pathname === item.path)) {
            setIsManageOpen(true);
        }
    }, [location.pathname]);

    const navLinkClass = ({ isActive }) => cn(
        "flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200",
        isActive
            ? "bg-blue-50 text-blue-600"
            : "text-gray-600 hover:bg-gray-50 hover:text-gray-900",
        isCollapsed && "justify-center px-2"
    );

    const sidebarContent = (
        <div className="flex flex-col h-full bg-white border-r border-gray-200">
            {/* Header */}
            <div className={cn("p-6 flex items-center h-20 transition-all duration-300", isCollapsed ? "justify-center px-2" : "justify-between")}>
                {/* Logo Area */}
                <div className={cn("flex items-center gap-2 text-blue-600 font-bold text-xl transition-all duration-300",
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
            <nav className="flex-1 px-3 space-y-1 overflow-y-auto pt-4 scrollbar-hide">
                {/* Main Section */}
                <div className="space-y-1">
                    {mainItems.map((item) => (
                        <NavLink
                            key={item.name}
                            to={item.path}
                            onClick={() => isMobile && setIsMobileOpen(false)}
                            className={navLinkClass}
                            title={isCollapsed ? item.name : undefined}
                        >
                            <div className="relative">
                                <item.icon size={18} className="flex-shrink-0" />
                                {item.name === 'Notifications' && unreadNotificationsCount > 0 && (
                                    <span className="absolute -top-1.5 -right-1.5 w-4 h-4 bg-red-500 text-white text-[9px] font-black rounded-full flex items-center justify-center border-2 border-white">
                                        {unreadNotificationsCount > 9 ? '9+' : unreadNotificationsCount}
                                    </span>
                                )}
                            </div>
                            <span className={cn("whitespace-nowrap transition-all duration-300", isCollapsed ? "w-0 opacity-0 hidden" : "block")}>
                                {item.name}
                            </span>
                        </NavLink>
                    ))}
                </div>

                {/* Manage Section */}
                <div className="pt-4 mt-4 border-t border-gray-50">
                    {!isCollapsed && (
                        <div
                            className="flex items-center justify-between px-3 mb-2 cursor-pointer group"
                            onClick={() => setIsManageOpen(!isManageOpen)}
                        >
                            <span className="text-[10px] font-black uppercase tracking-widest text-gray-400 group-hover:text-blue-500 transition-colors">Manage</span>
                            <ChevronDown
                                size={12}
                                className={cn("text-gray-400 transition-transform duration-300", isManageOpen ? "rotate-0" : "-rotate-90")}
                            />
                        </div>
                    )}
                    {isCollapsed && (
                        <div className="flex justify-center mb-2">
                            <div className="w-6 h-[1px] bg-gray-100" />
                        </div>
                    )}

                    {(isManageOpen || isCollapsed) && (
                        <div className="space-y-1">
                            {manageItems.map((item) => (
                                <NavLink
                                    key={item.name}
                                    to={item.path}
                                    onClick={() => isMobile && setIsMobileOpen(false)}
                                    className={navLinkClass}
                                    title={isCollapsed ? item.name : undefined}
                                >
                                    <item.icon size={18} className="flex-shrink-0" />
                                    <span className={cn("whitespace-nowrap transition-all duration-300", isCollapsed ? "w-0 opacity-0 hidden" : "block")}>
                                        {item.name}
                                    </span>
                                </NavLink>
                            ))}
                        </div>
                    )}
                </div>
            </nav>

            {/* Bottom Utilities */}
            <div className="px-3 py-4 space-y-1 border-t border-gray-100">
                {bottomItems.map((item) => (
                    <NavLink
                        key={item.name}
                        to={item.path}
                        onClick={() => isMobile && setIsMobileOpen(false)}
                        className={navLinkClass}
                        title={isCollapsed ? item.name : undefined}
                    >
                        <div className="relative">
                            <item.icon size={18} className="flex-shrink-0" />
                            {item.name === 'Notifications' && unreadNotificationsCount > 0 && (
                                <span className="absolute -top-1.5 -right-1.5 w-4 h-4 bg-red-500 text-white text-[9px] font-black rounded-full flex items-center justify-center border-2 border-white">
                                    {unreadNotificationsCount > 9 ? '9+' : unreadNotificationsCount}
                                </span>
                            )}
                        </div>
                        <span className={cn("whitespace-nowrap transition-all duration-300", isCollapsed ? "w-0 opacity-0 hidden" : "block")}>
                            {item.name}
                        </span>
                    </NavLink>
                ))}
            </div>

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
