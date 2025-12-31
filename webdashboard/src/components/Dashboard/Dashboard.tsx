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
        name: string;
        email: string;
        organizationName: string;
        organizationId: string;
    };
    onLogout: () => void;
}

type TabType = 'overview' | 'employees' | 'calls' | 'recordings' | 'reports' | 'settings';

export default function Dashboard({ user, onLogout }: DashboardProps) {
    const [activeTab, setActiveTab] = useState<TabType>('overview');
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

    const renderContent = () => {
        switch (activeTab) {
            case 'overview':
                return <Overview organizationId={user.organizationId} />;
            case 'employees':
                return <Employees organizationId={user.organizationId} />;
            case 'calls':
                return <Calls organizationId={user.organizationId} />;
            case 'recordings':
                return <Recordings organizationId={user.organizationId} />;
            case 'reports':
                return <Reports organizationId={user.organizationId} />;
            case 'settings':
                return <Settings user={user} onLogout={onLogout} />;
            default:
                return <Overview organizationId={user.organizationId} />;
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
                                <circle cx="24" cy="24" r="20" fill="url(#gradient)" />
                                <path d="M24 14C18.48 14 14 18.48 14 24C14 29.52 18.48 34 24 34C29.52 34 34 29.52 34 24C34 18.48 29.52 14 24 14ZM24 30C20.69 30 18 27.31 18 24C18 20.69 20.69 18 24 18C27.31 18 30 20.69 30 24C30 27.31 27.31 30 24 30Z" fill="white" />
                                <defs>
                                    <linearGradient id="gradient" x1="4" y1="4" x2="44" y2="44">
                                        <stop offset="0%" stopColor="#6366f1" />
                                        <stop offset="100%" stopColor="#8b5cf6" />
                                    </linearGradient>
                                </defs>
                            </svg>
                        </div>
                        {!sidebarCollapsed && (
                            <div className="brand-text">
                                <h2>CallCloud</h2>
                                <span className="badge badge-primary">{user.organizationId}</span>
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
                        {!sidebarCollapsed && <span>Overview</span>}
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
                        className={`nav-item ${activeTab === 'calls' ? 'active' : ''}`}
                        onClick={() => setActiveTab('calls')}
                    >
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
                        </svg>
                        {!sidebarCollapsed && <span>Calls</span>}
                    </button>

                    <button
                        className={`nav-item ${activeTab === 'recordings' ? 'active' : ''}`}
                        onClick={() => setActiveTab('recordings')}
                    >
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="12" cy="12" r="10" />
                            <polygon points="10 8 16 12 10 16 10 8" />
                        </svg>
                        {!sidebarCollapsed && <span>Recordings</span>}
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
                        <h1>{activeTab.charAt(0).toUpperCase() + activeTab.slice(1)}</h1>
                        <p className="text-muted">{user.organizationName}</p>
                    </div>

                    <div className="top-bar-actions">
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
