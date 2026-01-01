import { useState, useEffect } from 'react';
import './Pages.css';
import api from '../../api/client';

interface ReportsProps {
    organizationId: string;
}

export default function Reports({ organizationId }: ReportsProps) {
    const [reportType, setReportType] = useState('overview');
    const [dateRange, setDateRange] = useState('week');
    const [loading, setLoading] = useState(true);
    const [data, setData] = useState<any>(null);

    useEffect(() => {
        loadReport();
    }, [organizationId, reportType, dateRange]);

    const loadReport = async () => {
        try {
            setLoading(true);
            let response;
            switch (reportType) {
                case 'overview':
                    response = await api.getOverviewReport(dateRange);
                    break;
                case 'employee':
                    response = await api.getEmployeePerformance(dateRange);
                    break;
                case 'calls':
                    response = await api.getCallAnalytics(dateRange);
                    break;
                default:
                    response = await api.getOverviewReport(dateRange);
            }

            if (response.status) {
                setData(response.data);
            }
        } catch (error) {
            console.error('Failed to load report:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleExport = (format: string) => {
        alert(`Exporting ${reportType} report as ${format.toUpperCase()}...`);
    };

    if (loading && !data) {
        return (
            <div className="page-container flex items-center justify-center py-20">
                <div className="text-xl text-muted">Generating report...</div>
            </div>
        );
    }

    const metrics = reportType === 'overview' ? data?.metrics : null;
    const trends = reportType === 'overview' ? data?.trends : [];
    const employees = reportType === 'employee' ? data : [];
    const analytics = reportType === 'calls' ? data : null;

    return (
        <div className="page-container">
            {/* Header */}
            <div className="page-header">
                <div className="filter-group">
                    <select className="form-select" value={reportType} onChange={(e) => setReportType(e.target.value)}>
                        <option value="overview">Overview Report</option>
                        <option value="employee">Employee Performance</option>
                        <option value="calls">Call Analytics</option>
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

            {/* Content for Overview */}
            {reportType === 'overview' && metrics && (
                <>
                    <div className="metrics-grid">
                        <div className="metric-card card">
                            <div className="metric-header">
                                <span className="metric-label">Total Calls</span>
                                <span className="metric-change positive">{metrics.success_rate}% Success</span>
                            </div>
                            <div className="metric-value">{(metrics.total_calls || 0).toLocaleString()}</div>
                        </div>

                        <div className="metric-card card">
                            <div className="metric-header">
                                <span className="metric-label">Call Duration</span>
                                <span className="metric-change">Average</span>
                            </div>
                            <div className="metric-value">{metrics.avg_duration_formatted}</div>
                        </div>

                        <div className="metric-card card">
                            <div className="metric-header">
                                <span className="metric-label">Inbound Calls</span>
                                <span className="metric-change">{metrics.inbound_calls && metrics.total_calls ? Math.round((metrics.inbound_calls / metrics.total_calls) * 100) : 0}% of Total</span>
                            </div>
                            <div className="metric-value">{(metrics.inbound_calls || 0).toLocaleString()}</div>
                        </div>

                        <div className="metric-card card">
                            <div className="metric-header">
                                <span className="metric-label">Recordings</span>
                                <span className="metric-change">Stored Clips</span>
                            </div>
                            <div className="metric-value">{(metrics.recordings_count || 0).toLocaleString()}</div>
                        </div>
                    </div>

                    <div className="reports-grid">
                        <div className="card chart-card">
                            <div className="card-header">
                                <h3 className="card-title">Call Volume (Recent Trends)</h3>
                            </div>
                            <div className="chart-container">
                                {trends && trends.length > 0 ? (
                                    <div className="flex items-end justify-between h-full gap-2 p-4">
                                        {trends.map((t: any, i: number) => (
                                            <div key={i} className="flex flex-col items-center flex-1 gap-2">
                                                <div
                                                    className="w-full bg-[var(--primary)] rounded-t-sm opacity-80"
                                                    style={{ height: `${Math.max(10, (t.call_count / Math.max(...trends.map((x: any) => x.call_count))) * 200)}px` }}
                                                ></div>
                                                <span className="text-xs text-muted truncate">{new Date(t.date).toLocaleDateString(undefined, { weekday: 'short' })}</span>
                                            </div>
                                        ))}
                                    </div>
                                ) : (
                                    <div className="flex items-center justify-center h-full text-muted">No trend data available</div>
                                )}
                            </div>
                        </div>
                    </div>
                </>
            )}

            {/* Content for Employee Performance */}
            {reportType === 'employee' && employees && (
                <div className="card">
                    <div className="card-header">
                        <h3 className="card-title">Employee Ranking</h3>
                    </div>
                    <div className="performance-list">
                        {employees.length > 0 ? employees.map((performer: any, index: number) => (
                            <div key={index} className="performance-item">
                                <div className="rank-badge">{index + 1}</div>
                                <div className="performer-avatar">{performer.name.charAt(0)}</div>
                                <div className="performer-info">
                                    <div className="performer-name">{performer.name}</div>
                                    <div className="performer-stats">{performer.total_calls} calls • {performer.avg_duration_formatted} avg</div>
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
                        )) : (
                            <div className="p-10 text-center text-muted">No employee data found for this period</div>
                        )}
                    </div>
                </div>
            )}



            {/* Content for Call Analytics */}
            {reportType === 'calls' && analytics && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="card">
                        <div className="card-header">
                            <h3 className="card-title">Hourly Distribution</h3>
                        </div>
                        <div className="p-4 h-64 flex items-end justify-between gap-1">
                            {analytics.hourly?.map((h: any, i: number) => (
                                <div key={i} className="flex-1 bg-[var(--primary)] opacity-70" title={`${h.hour}:00 - ${h.call_count} calls`} style={{ height: `${(h.call_count / Math.max(...analytics.hourly.map((x: any) => x.call_count))) * 100}%` }}></div>
                            ))}
                        </div>
                        <div className="text-center text-xs text-muted mb-4">Hour of Day (0-23)</div>
                    </div>
                    <div className="card">
                        <div className="card-header">
                            <h3 className="card-title">Call Direction</h3>
                        </div>
                        <div className="p-8 flex items-center justify-center">
                            <div className="flex flex-col items-center gap-4">
                                <div className="flex gap-10">
                                    <div className="text-center">
                                        <div className="text-success text-2xl font-bold">{analytics.direction?.inbound || 0}</div>
                                        <div className="text-muted text-sm">Inbound</div>
                                    </div>
                                    <div className="text-center">
                                        <div className="text-info text-2xl font-bold">{analytics.direction?.outbound || 0}</div>
                                        <div className="text-muted text-sm">Outbound</div>
                                    </div>
                                </div>
                                <div className="w-64 h-4 bg-[var(--bg-tertiary)] rounded-full overflow-hidden flex">
                                    <div className="bg-success" style={{ width: `${(analytics.direction?.inbound / (analytics.direction?.inbound + analytics.direction?.outbound)) * 100 || 50}%` }}></div>
                                    <div className="bg-info" style={{ width: `${(analytics.direction?.outbound / (analytics.direction?.inbound + analytics.direction?.outbound)) * 100 || 50}%` }}></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
