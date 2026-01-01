import { useState } from 'react';
import './Dashboard.css';
import Overview from './Overview';
import Employees from './Employees';
import Calls from './Calls';
import Recordings from './Recordings';
import Reports from './Reports';
import Settings from './Settings';

interface DashboardProps {
    user: {
        id: number;
        name: string;
        email: string;
        organizationName: string;
        org_id: string;
        role: string;
    };
    onLogout: () => void;
    theme: 'dark' | 'light';
    toggleTheme: () => void;
}

type TabType = 'overview' | 'employees' | 'calls' | 'recordings' | 'reports' | 'settings';

export default function Dashboard({ user, onLogout, theme, toggleTheme }: DashboardProps) {
    const [activeTab, setActiveTab] = useState<TabType>('overview');
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

    const renderContent = () => {
        switch (activeTab) {
            case 'overview':
                return <Overview organizationId={user.org_id} />;
            case 'reports':
                return <Reports organizationId={user.org_id} />;
            case 'calls':
                return <Calls organizationId={user.org_id} />;
            case 'employees':
                return <Employees organizationId={user.org_id} />;
            case 'recordings':
                return <Recordings organizationId={user.org_id} />;
            case 'settings':
                return <Settings user={user} onLogout={onLogout} />;
            default:
                return <Overview organizationId={user.org_id} />;
        }
    };

    return (
        <div className="dashboard">
            {/* Sidebar */}
            <aside className={`sidebar ${sidebarCollapsed ? 'collapsed' : ''}`}>
                <div className="sidebar-header">
                    <div className="sidebar-brand">
                        <div className="brand-icon-small">
                            <svg width="32" height="32" viewBox="0 0 48 48" fill="none">
                                <circle cx="24" cy="24" r="20" fill="var(--primary)" fillOpacity="0.2" />
                                <path d="M24 14C18.48 14 14 18.48 14 24C14 29.52 18.48 34 24 34C29.52 34 34 29.52 34 24C34 18.48 29.52 14 24 14ZM24 30C20.69 30 18 27.31 18 24C18 20.69 20.69 18 24 18C27.31 18 30 20.69 30 24C30 27.31 27.31 30 24 30Z" fill="var(--primary)" />
                            </svg>
                        </div>
                        {!sidebarCollapsed && (
                            <div className="brand-text">
                                <h2>CallCloud</h2>
                                <span className="badge badge-primary">{user.org_id}</span>
                            </div>
                        )}
                    </div>
                    <button
                        className="btn btn-ghost sidebar-toggle"
                        onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
                    >
                        {sidebarCollapsed ? '→' : '←'}
                    </button>
                </div>

                <nav className="sidebar-nav">
                    <button
                        className={`nav-item ${activeTab === 'overview' ? 'active' : ''}`}
                        onClick={() => setActiveTab('overview')}
                    >
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <rect x="3" y="3" width="7" height="7" rx="1" />
                            <rect x="14" y="3" width="7" height="7" rx="1" />
                            <rect x="14" y="14" width="7" height="7" rx="1" />
                            <rect x="3" y="14" width="7" height="7" rx="1" />
                        </svg>
                        {!sidebarCollapsed && <span>Dashboard</span>}
                    </button>

                    <button
                        className={`nav-item ${activeTab === 'reports' ? 'active' : ''}`}
                        onClick={() => setActiveTab('reports')}
                    >
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <line x1="12" y1="20" x2="12" y2="10" />
                            <line x1="18" y1="20" x2="18" y2="4" />
                            <line x1="6" y1="20" x2="6" y2="16" />
                        </svg>
                        {!sidebarCollapsed && <span>Reports</span>}
                    </button>

                    <button
                        className={`nav-item ${activeTab === 'calls' ? 'active' : ''}`}
                        onClick={() => setActiveTab('calls')}
                    >
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
                        </svg>
                        {!sidebarCollapsed && <span>Calls</span>}
                    </button>

                    <button
                        className={`nav-item ${activeTab === 'employees' ? 'active' : ''}`}
                        onClick={() => setActiveTab('employees')}
                    >
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                            <circle cx="9" cy="7" r="4" />
                            <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                        </svg>
                        {!sidebarCollapsed && <span>Employees</span>}
                    </button>

                    <button
                        className={`nav-item ${activeTab === 'settings' ? 'active' : ''}`}
                        onClick={() => setActiveTab('settings')}
                    >
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="12" cy="12" r="3" />
                            <path d="M12 1v6m0 6v6m8.66-9l-5.2 3m-5.2 3-5.2 3M20.66 7l-5.2 3m-5.2 3L5.34 17" />
                        </svg>
                        {!sidebarCollapsed && <span>Settings</span>}
                    </button>

                </nav>

                <div className="sidebar-footer">
                    <div className="user-profile">
                        <div className="user-avatar">
                            {user.name.charAt(0).toUpperCase()}
                        </div>
                        {!sidebarCollapsed && (
                            <div className="user-info">
                                <div className="user-name">{user.name}</div>
                                <div className="user-email">{user.email}</div>
                            </div>
                        )}
                    </div>
                </div>
            </aside>

            {/* Main Content */}
            <main className="main-content">
                <header className="top-bar">
                    <div className="page-title">
                        <h1>
                            {activeTab === 'overview' ? 'Dashboard' :
                                activeTab.charAt(0).toUpperCase() + activeTab.slice(1)}
                        </h1>
                        <p className="text-muted">{user.organizationName}</p>
                    </div>

                    <div className="top-bar-actions">
                        <button className="btn btn-ghost" onClick={toggleTheme} aria-label="Toggle theme">
                            {theme === 'dark' ? (
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <circle cx="12" cy="12" r="5" />
                                    <line x1="12" y1="1" x2="12" y2="3" />
                                    <line x1="12" y1="21" x2="12" y2="23" />
                                    <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
                                    <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
                                    <line x1="1" y1="12" x2="3" y2="12" />
                                    <line x1="21" y1="12" x2="23" y2="12" />
                                    <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
                                    <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
                                </svg>
                            ) : (
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
                                </svg>
                            )}
                        </button>
                        <button className="btn btn-ghost">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                            </svg>
                        </button>
                        <button className="btn btn-primary" onClick={onLogout}>
                            Logout
                        </button>
                    </div>
                </header>

                <div className="content-area fade-in">
                    {renderContent()}
                </div>
            </main>
        </div>
    );
}
