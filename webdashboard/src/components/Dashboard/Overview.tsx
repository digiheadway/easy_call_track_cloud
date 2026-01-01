import { useState, useEffect } from 'react';
import './Pages.css';
import api from '../../api/client';

interface OverviewProps {
    organizationId: string;
}

export default function Overview({ organizationId }: OverviewProps) {
    const [stats, setStats] = useState<any[]>([]);
    const [recentActivities, setRecentActivities] = useState<any[]>([]);
    const [trends, setTrends] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadData();
    }, [organizationId]);

    const loadData = async () => {
        try {
            setLoading(true);
            const response = await api.getOverviewReport('week');
            if (response.status && response.data) {
                const metrics = response.data.metrics;
                const apiTrends = response.data.trends || [];
                setTrends(apiTrends);

                setStats([
                    {
                        label: 'Total Calls',
                        value: (metrics.total_calls || 0).toLocaleString(),
                        change: metrics.success_rate + '% Success',
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
                        value: (metrics.active_employees || 0).toLocaleString(),
                        change: 'Online Now',
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
                        value: metrics.avg_duration_formatted || '0:00',
                        change: 'Per Interaction',
                        trend: 'up',
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
                        value: (metrics.recordings_count || 0).toLocaleString(),
                        change: 'Stored in Cloud',
                        trend: 'up',
                        icon: (
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="12" cy="12" r="10" />
                                <polygon points="10 8 16 12 10 16 10 8" />
                            </svg>
                        ),
                        color: 'secondary'
                    },
                ]);
            }

            // Load some recent calls as activity
            const callsResponse = await api.getCalls({ limit: 5 });
            if (callsResponse.status && callsResponse.data) {
                const calls = Array.isArray(callsResponse.data) ? callsResponse.data : callsResponse.data.calls || [];
                setRecentActivities(calls.map((call: any) => ({
                    employee: call.employee_name || 'System',
                    action: call.type === 'Incoming' ? 'received a call from' : 'made a call to',
                    contact: call.contact_name || call.phone_number,
                    time: new Date(call.call_time).toLocaleString(),
                    type: 'call'
                })));
            }

        } catch (error) {
            console.error('Failed to load dashboard data:', error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="page-container flex items-center justify-center py-20">
                <div className="text-xl text-muted">Loading dashboard data...</div>
            </div>
        );
    }

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
                                {stat.change}
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
                        <h3 className="card-title">Call Trends (Last 7 Days)</h3>
                    </div>
                    <div className="chart-container">
                        <div className="chart-placeholder">
                            {trends.length > 0 ? (
                                <svg width="100%" height="300" viewBox="0 0 800 300">
                                    <defs>
                                        <linearGradient id="chartGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                                            <stop offset="0%" stopColor="var(--primary)" stopOpacity="0.3" />
                                            <stop offset="100%" stopColor="var(--primary)" stopOpacity="0" />
                                        </linearGradient>
                                    </defs>
                                    <path
                                        d={`M ${trends.map((t, i) => `${(i * 800) / (trends.length - 1)} ${250 - (t.call_count * 10)}`).join(' L ')}`}
                                        fill="none"
                                        stroke="var(--primary)"
                                        strokeWidth="3"
                                    />
                                    <path
                                        d={`M 0 300 L ${trends.map((t, i) => `${(i * 800) / (trends.length - 1)} ${250 - (t.call_count * 10)}`).join(' L ')} L 800 300 Z`}
                                        fill="url(#chartGradient)"
                                    />
                                </svg>
                            ) : (
                                <div className="flex items-center justify-center h-full text-muted">No trend data available</div>
                            )}
                        </div>
                    </div>
                </div>

                {/* Recent Activity */}
                <div className="card">
                    <div className="card-header">
                        <h3 className="card-title">Recent Activity</h3>
                    </div>
                    <div className="activity-list">
                        {recentActivities.length > 0 ? recentActivities.map((activity, index) => (
                            <div key={index} className="activity-item">
                                <div className={`activity-icon ${activity.type}`}>
                                    {activity.type === 'call' && (
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
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
                        )) : (
                            <div className="p-8 text-center text-muted">No recent activity</div>
                        )}
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
