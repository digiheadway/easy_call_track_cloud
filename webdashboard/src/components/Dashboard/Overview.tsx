import './Pages.css';

interface OverviewProps {
    organizationId: string;
}

export default function Overview({ organizationId }: OverviewProps) {
    // Mock data - in real app, this would come from API
    const stats = [
        {
            label: 'Total Calls',
            value: '2,459',
            change: '+12.5%',
            trend: 'up',
            icon: (
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
                </svg>
            ),
            color: 'primary'
        },
        {
            label: 'Active Employees',
            value: '48',
            change: '+3',
            trend: 'up',
            icon: (
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                    <circle cx="9" cy="7" r="4" />
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                </svg>
            ),
            color: 'success'
        },
        {
            label: 'Avg Call Duration',
            value: '4:32',
            change: '-8.2%',
            trend: 'down',
            icon: (
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10" />
                    <polyline points="12 6 12 12 16 14" />
                </svg>
            ),
            color: 'info'
        },
        {
            label: 'Recordings',
            value: '1,842',
            change: '+18.3%',
            trend: 'up',
            icon: (
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10" />
                    <polygon points="10 8 16 12 10 16 10 8" />
                </svg>
            ),
            color: 'secondary'
        },
    ];

    const recentActivities = [
        { employee: 'John Smith', action: 'Made a call to', contact: '+1 234 567 8900', time: '2 min ago', type: 'call' },
        { employee: 'Sarah Johnson', action: 'Added new employee', contact: 'Mike Wilson', time: '15 min ago', type: 'employee' },
        { employee: 'David Lee', action: 'Uploaded recording for', contact: 'Client Meeting', time: '1 hour ago', type: 'recording' },
        { employee: 'Emma Davis', action: 'Generated report', contact: 'Weekly Summary', time: '2 hours ago', type: 'report' },
        { employee: 'James Brown', action: 'Made a call to', contact: '+1 987 654 3210', time: '3 hours ago', type: 'call' },
    ];

    return (
        <div className="page-container">
            {/* Stats Grid */}
            <div className="stats-grid">
                {stats.map((stat, index) => (
                    <div key={index} className="stat-card card">
                        <div className="stat-icon" style={{ background: `var(--${stat.color})` }}>
                            {stat.icon}
                        </div>
                        <div className="stat-content">
                            <div className="stat-label">{stat.label}</div>
                            <div className="stat-value">{stat.value}</div>
                            <div className={`stat-change ${stat.trend}`}>
                                {stat.trend === 'up' ? '↑' : '↓'} {stat.change}
                            </div>
                        </div>
                    </div>
                ))}
            </div>

            {/* Charts and Activity */}
            <div className="dashboard-grid">
                {/* Call Trends Chart */}
                <div className="card">
                    <div className="card-header">
                        <h3 className="card-title">Call Trends</h3>
                        <select className="form-select" style={{ width: 'auto' }}>
                            <option>Last 7 Days</option>
                            <option>Last 30 Days</option>
                            <option>Last 90 Days</option>
                        </select>
                    </div>
                    <div className="chart-container">
                        <div className="chart-placeholder">
                            <svg width="100%" height="300" viewBox="0 0 800 300">
                                <defs>
                                    <linearGradient id="chartGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                                        <stop offset="0%" stopColor="var(--primary)" stopOpacity="0.3" />
                                        <stop offset="100%" stopColor="var(--primary)" stopOpacity="0" />
                                    </linearGradient>
                                </defs>
                                <path
                                    d="M 0 250 L 100 200 L 200 220 L 300 150 L 400 180 L 500 120 L 600 160 L 700 100 L 800 140"
                                    fill="none"
                                    stroke="var(--primary)"
                                    strokeWidth="3"
                                />
                                <path
                                    d="M 0 250 L 100 200 L 200 220 L 300 150 L 400 180 L 500 120 L 600 160 L 700 100 L 800 140 L 800 300 L 0 300 Z"
                                    fill="url(#chartGradient)"
                                />
                            </svg>
                        </div>
                    </div>
                </div>

                {/* Recent Activity */}
                <div className="card">
                    <div className="card-header">
                        <h3 className="card-title">Recent Activity</h3>
                        <button className="btn btn-ghost text-sm">View All</button>
                    </div>
                    <div className="activity-list">
                        {recentActivities.map((activity, index) => (
                            <div key={index} className="activity-item">
                                <div className={`activity-icon ${activity.type}`}>
                                    {activity.type === 'call' && (
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
                                        </svg>
                                    )}
                                    {activity.type === 'employee' && (
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                                            <circle cx="8.5" cy="7" r="4" />
                                            <line x1="20" y1="8" x2="20" y2="14" />
                                            <line x1="23" y1="11" x2="17" y2="11" />
                                        </svg>
                                    )}
                                    {activity.type === 'recording' && (
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <circle cx="12" cy="12" r="10" />
                                            <polygon points="10 8 16 12 10 16 10 8" />
                                        </svg>
                                    )}
                                    {activity.type === 'report' && (
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <line x1="12" y1="20" x2="12" y2="10" />
                                            <line x1="18" y1="20" x2="18" y2="4" />
                                            <line x1="6" y1="20" x2="6" y2="16" />
                                        </svg>
                                    )}
                                </div>
                                <div className="activity-content">
                                    <div className="activity-text">
                                        <strong>{activity.employee}</strong> {activity.action} <span className="text-primary">{activity.contact}</span>
                                    </div>
                                    <div className="activity-time">{activity.time}</div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* Quick Actions */}
            <div className="quick-actions">
                <h3 className="mb-3">Quick Actions</h3>
                <div className="actions-grid">
                    <button className="action-card card">
                        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <line x1="12" y1="5" x2="12" y2="19" />
                            <line x1="5" y1="12" x2="19" y2="12" />
                        </svg>
                        <span>Add Employee</span>
                    </button>
                    <button className="action-card card">
                        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                            <polyline points="7 10 12 15 17 10" />
                            <line x1="12" y1="15" x2="12" y2="3" />
                        </svg>
                        <span>Export Data</span>
                    </button>
                    <button className="action-card card">
                        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                            <polyline points="14 2 14 8 20 8" />
                            <line x1="16" y1="13" x2="8" y2="13" />
                            <line x1="16" y1="17" x2="8" y2="17" />
                            <polyline points="10 9 9 9 8 9" />
                        </svg>
                        <span>Generate Report</span>
                    </button>
                    <button className="action-card card">
                        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="12" cy="12" r="3" />
                            <path d="M12 1v6m0 6v6" />
                        </svg>
                        <span>Settings</span>
                    </button>
                </div>
            </div>
        </div>
    );
}
