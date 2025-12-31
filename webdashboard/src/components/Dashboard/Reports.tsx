import { useState } from 'react';
import './Pages.css';

interface ReportsProps {
    organizationId: string;
}

export default function Reports({ organizationId: _organizationId }: ReportsProps) {
    const [reportType, setReportType] = useState('overview');
    const [dateRange, setDateRange] = useState('week');

    const handleExport = (format: string) => {
        alert(`Exporting report as ${format.toUpperCase()}...`);
    };

    return (
        <div className="page-container">
            {/* Header */}
            <div className="page-header">
                <div className="filter-group">
                    <select className="form-select" value={reportType} onChange={(e) => setReportType(e.target.value)}>
                        <option value="overview">Overview Report</option>
                        <option value="employee">Employee Performance</option>
                        <option value="calls">Call Analytics</option>
                        <option value="recordings">Recording Statistics</option>
                    </select>
                    <select className="form-select" value={dateRange} onChange={(e) => setDateRange(e.target.value)}>
                        <option value="today">Today</option>
                        <option value="week">This Week</option>
                        <option value="month">This Month</option>
                        <option value="quarter">This Quarter</option>
                        <option value="year">This Year</option>
                    </select>
                </div>
                <div className="export-buttons">
                    <button className="btn btn-secondary" onClick={() => handleExport('pdf')}>
                        Export PDF
                    </button>
                    <button className="btn btn-secondary" onClick={() => handleExport('csv')}>
                        Export CSV
                    </button>
                </div>
            </div>

            {/* Key Metrics */}
            <div className="metrics-grid">
                <div className="metric-card card">
                    <div className="metric-header">
                        <span className="metric-label">Total Calls</span>
                        <span className="metric-change positive">+12.5%</span>
                    </div>
                    <div className="metric-value">2,459</div>
                    <div className="metric-footer">
                        <div className="mini-chart">
                            <svg width="100%" height="40" viewBox="0 0 200 40">
                                <path
                                    d="M 0 35 L 25 30 L 50 32 L 75 22 L 100 25 L 125 18 L 150 20 L 175 12 L 200 15"
                                    fill="none"
                                    stroke="var(--success)"
                                    strokeWidth="2"
                                />
                            </svg>
                        </div>
                    </div>
                </div>

                <div className="metric-card card">
                    <div className="metric-header">
                        <span className="metric-label">Call Duration</span>
                        <span className="metric-change negative">-3.2%</span>
                    </div>
                    <div className="metric-value">4:32</div>
                    <div className="metric-footer">
                        <div className="mini-chart">
                            <svg width="100%" height="40" viewBox="0 0 200 40">
                                <path
                                    d="M 0 15 L 25 18 L 50 16 L 75 22 L 100 20 L 125 25 L 150 23 L 175 28 L 200 30"
                                    fill="none"
                                    stroke="var(--error)"
                                    strokeWidth="2"
                                />
                            </svg>
                        </div>
                    </div>
                </div>

                <div className="metric-card card">
                    <div className="metric-header">
                        <span className="metric-label">Success Rate</span>
                        <span className="metric-change positive">+5.8%</span>
                    </div>
                    <div className="metric-value">87.3%</div>
                    <div className="metric-footer">
                        <div className="progress-bar">
                            <div className="progress-fill" style={{ width: '87.3%' }}></div>
                        </div>
                    </div>
                </div>

                <div className="metric-card card">
                    <div className="metric-header">
                        <span className="metric-label">Recordings</span>
                        <span className="metric-change positive">+18.3%</span>
                    </div>
                    <div className="metric-value">1,842</div>
                    <div className="metric-footer">
                        <div className="mini-chart">
                            <svg width="100%" height="40" viewBox="0 0 200 40">
                                <path
                                    d="M 0 30 L 25 28 L 50 25 L 75 20 L 100 22 L 125 15 L 150 12 L 175 10 L 200 8"
                                    fill="none"
                                    stroke="var(--primary)"
                                    strokeWidth="2"
                                />
                            </svg>
                        </div>
                    </div>
                </div>
            </div>

            {/* Charts */}
            <div className="reports-grid">
                {/* Call Volume Chart */}
                <div className="card chart-card">
                    <div className="card-header">
                        <h3 className="card-title">Call Volume by Day</h3>
                    </div>
                    <div className="chart-container">
                        <svg width="100%" height="300" viewBox="0 0 700 300">
                            <defs>
                                <linearGradient id="barGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                                    <stop offset="0%" stopColor="var(--primary)" stopOpacity="0.8" />
                                    <stop offset="100%" stopColor="var(--primary)" stopOpacity="0.3" />
                                </linearGradient>
                            </defs>
                            {/* Bars */}
                            <rect x="50" y="100" width="80" height="150" fill="url(#barGradient)" rx="4" />
                            <rect x="150" y="80" width="80" height="170" fill="url(#barGradient)" rx="4" />
                            <rect x="250" y="120" width="80" height="130" fill="url(#barGradient)" rx="4" />
                            <rect x="350" y="60" width="80" height="190" fill="url(#barGradient)" rx="4" />
                            <rect x="450" y="90" width="80" height="160" fill="url(#barGradient)" rx="4" />
                            <rect x="550" y="110" width="80" height="140" fill="url(#barGradient)" rx="4" />
                            {/* Labels */}
                            <text x="90" y="280" fill="var(--text-muted)" fontSize="12" textAnchor="middle">Mon</text>
                            <text x="190" y="280" fill="var(--text-muted)" fontSize="12" textAnchor="middle">Tue</text>
                            <text x="290" y="280" fill="var(--text-muted)" fontSize="12" textAnchor="middle">Wed</text>
                            <text x="390" y="280" fill="var(--text-muted)" fontSize="12" textAnchor="middle">Thu</text>
                            <text x="490" y="280" fill="var(--text-muted)" fontSize="12" textAnchor="middle">Fri</text>
                            <text x="590" y="280" fill="var(--text-muted)" fontSize="12" textAnchor="middle">Sat</text>
                        </svg>
                    </div>
                </div>

                {/* Employee Performance */}
                <div className="card">
                    <div className="card-header">
                        <h3 className="card-title">Top Performers</h3>
                    </div>
                    <div className="performance-list">
                        {[
                            { name: 'John Smith', calls: 145, avatar: 'J', score: 95 },
                            { name: 'Sarah Johnson', calls: 132, avatar: 'S', score: 92 },
                            { name: 'David Lee', calls: 128, avatar: 'D', score: 89 },
                            { name: 'Emma Davis', calls: 121, avatar: 'E', score: 87 },
                            { name: 'James Brown', calls: 115, avatar: 'J', score: 85 },
                        ].map((performer, index) => (
                            <div key={index} className="performance-item">
                                <div className="rank-badge">{index + 1}</div>
                                <div className="performer-avatar">{performer.avatar}</div>
                                <div className="performer-info">
                                    <div className="performer-name">{performer.name}</div>
                                    <div className="performer-stats">{performer.calls} calls</div>
                                </div>
                                <div className="performer-score">
                                    <div className="score-circle">
                                        <svg width="50" height="50" viewBox="0 0 50 50">
                                            <circle cx="25" cy="25" r="20" fill="none" stroke="var(--bg-tertiary)" strokeWidth="4" />
                                            <circle
                                                cx="25"
                                                cy="25"
                                                r="20"
                                                fill="none"
                                                stroke="var(--primary)"
                                                strokeWidth="4"
                                                strokeDasharray={`${2 * Math.PI * 20 * performer.score / 100} ${2 * Math.PI * 20}`}
                                                transform="rotate(-90 25 25)"
                                            />
                                        </svg>
                                        <span className="score-text">{performer.score}</span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* Department Breakdown */}
            <div className="card">
                <div className="card-header">
                    <h3 className="card-title">Department Breakdown</h3>
                </div>
                <div className="department-grid">
                    {[
                        { name: 'Sales', calls: 845, percentage: 34, color: 'var(--primary)' },
                        { name: 'Support', calls: 687, percentage: 28, color: 'var(--success)' },
                        { name: 'Marketing', calls: 523, percentage: 21, color: 'var(--info)' },
                        { name: 'Operations', calls: 404, percentage: 17, color: 'var(--secondary)' },
                    ].map((dept, index) => (
                        <div key={index} className="department-item">
                            <div className="department-header">
                                <span className="department-name">{dept.name}</span>
                                <span className="department-percentage">{dept.percentage}%</span>
                            </div>
                            <div className="department-bar">
                                <div
                                    className="department-fill"
                                    style={{ width: `${dept.percentage}%`, background: dept.color }}
                                ></div>
                            </div>
                            <div className="department-calls">{dept.calls} calls</div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
