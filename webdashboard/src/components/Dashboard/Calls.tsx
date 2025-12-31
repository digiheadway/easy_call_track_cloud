import { useState } from 'react';
import './Pages.css';

interface CallsProps {
    organizationId: string;
}

interface Call {
    id: string;
    employee: string;
    contact: string;
    phoneNumber: string;
    direction: 'inbound' | 'outbound';
    duration: string;
    timestamp: string;
    status: 'completed' | 'missed' | 'rejected';
    hasRecording: boolean;
}

export default function Calls({ organizationId }: CallsProps) {
    const [filterType, setFilterType] = useState<'all' | 'inbound' | 'outbound'>('all');
    const [searchQuery, setSearchQuery] = useState('');
    const [dateFilter, setDateFilter] = useState('today');

    // Mock data
    const calls: Call[] = [
        {
            id: '1',
            employee: 'John Smith',
            contact: 'ABC Corporation',
            phoneNumber: '+1 234 567 8900',
            direction: 'outbound',
            duration: '12:34',
            timestamp: '2024-12-31 14:30:00',
            status: 'completed',
            hasRecording: true
        },
        {
            id: '2',
            employee: 'Sarah Johnson',
            contact: 'XYZ Industries',
            phoneNumber: '+1 234 567 8901',
            direction: 'inbound',
            duration: '8:45',
            timestamp: '2024-12-31 13:15:00',
            status: 'completed',
            hasRecording: true
        },
        {
            id: '3',
            employee: 'David Lee',
            contact: 'Tech Solutions Ltd',
            phoneNumber: '+1 234 567 8902',
            direction: 'outbound',
            duration: '5:22',
            timestamp: '2024-12-31 12:45:00',
            status: 'completed',
            hasRecording: false
        },
        {
            id: '4',
            employee: 'Emma Davis',
            contact: 'Global Services',
            phoneNumber: '+1 234 567 8903',
            direction: 'inbound',
            duration: '0:00',
            timestamp: '2024-12-31 11:20:00',
            status: 'missed',
            hasRecording: false
        },
        {
            id: '5',
            employee: 'James Brown',
            contact: 'Innovation Hub',
            phoneNumber: '+1 234 567 8904',
            direction: 'outbound',
            duration: '15:12',
            timestamp: '2024-12-31 10:05:00',
            status: 'completed',
            hasRecording: true
        },
    ];

    const filteredCalls = calls.filter(call => {
        const matchesType = filterType === 'all' || call.direction === filterType;
        const matchesSearch =
            call.contact.toLowerCase().includes(searchQuery.toLowerCase()) ||
            call.phoneNumber.includes(searchQuery) ||
            call.employee.toLowerCase().includes(searchQuery.toLowerCase());
        return matchesType && matchesSearch;
    });

    const stats = {
        total: calls.length,
        inbound: calls.filter(c => c.direction === 'inbound').length,
        outbound: calls.filter(c => c.direction === 'outbound').length,
        missed: calls.filter(c => c.status === 'missed').length,
    };

    return (
        <div className="page-container">
            {/* Header */}
            <div className="page-header">
                <div className="search-bar">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="11" cy="11" r="8" />
                        <path d="m21 21-4.35-4.35" />
                    </svg>
                    <input
                        type="text"
                        placeholder="Search calls..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </div>
                <div className="filter-group">
                    <select className="form-select" value={dateFilter} onChange={(e) => setDateFilter(e.target.value)}>
                        <option value="today">Today</option>
                        <option value="week">This Week</option>
                        <option value="month">This Month</option>
                        <option value="all">All Time</option>
                    </select>
                </div>
            </div>

            {/* Call Stats */}
            <div className="call-stats">
                <button
                    className={`stat-filter ${filterType === 'all' ? 'active' : ''}`}
                    onClick={() => setFilterType('all')}
                >
                    <div className="stat-number">{stats.total}</div>
                    <div className="stat-label">All Calls</div>
                </button>
                <button
                    className={`stat-filter ${filterType === 'inbound' ? 'active' : ''}`}
                    onClick={() => setFilterType('inbound')}
                >
                    <div className="stat-number text-success">{stats.inbound}</div>
                    <div className="stat-label">Inbound</div>
                </button>
                <button
                    className={`stat-filter ${filterType === 'outbound' ? 'active' : ''}`}
                    onClick={() => setFilterType('outbound')}
                >
                    <div className="stat-number text-info">{stats.outbound}</div>
                    <div className="stat-label">Outbound</div>
                </button>
                <button className="stat-filter">
                    <div className="stat-number text-error">{stats.missed}</div>
                    <div className="stat-label">Missed</div>
                </button>
            </div>

            {/* Calls List */}
            <div className="card">
                <div className="table-container">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>Contact</th>
                                <th>Employee</th>
                                <th>Direction</th>
                                <th>Duration</th>
                                <th>Time</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredCalls.map((call) => (
                                <tr key={call.id}>
                                    <td>
                                        <div>
                                            <div className="font-medium">{call.contact}</div>
                                            <div className="text-muted text-sm">{call.phoneNumber}</div>
                                        </div>
                                    </td>
                                    <td>{call.employee}</td>
                                    <td>
                                        <div className="call-direction">
                                            {call.direction === 'inbound' ? (
                                                <>
                                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-success">
                                                        <polyline points="17 11 12 6 7 11" />
                                                        <polyline points="17 18 12 13 7 18" />
                                                    </svg>
                                                    <span className="text-success">Inbound</span>
                                                </>
                                            ) : (
                                                <>
                                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-info">
                                                        <polyline points="7 13 12 18 17 13" />
                                                        <polyline points="7 6 12 11 17 6" />
                                                    </svg>
                                                    <span className="text-info">Outbound</span>
                                                </>
                                            )}
                                        </div>
                                    </td>
                                    <td className="font-medium">{call.duration}</td>
                                    <td className="text-muted">
                                        {new Date(call.timestamp).toLocaleString()}
                                    </td>
                                    <td>
                                        <span className={`badge badge-${call.status === 'completed' ? 'success' :
                                                call.status === 'missed' ? 'error' :
                                                    'warning'
                                            }`}>
                                            {call.status}
                                        </span>
                                    </td>
                                    <td>
                                        <div className="action-buttons">
                                            {call.hasRecording && (
                                                <button className="btn btn-ghost btn-sm" title="Play Recording">
                                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                        <circle cx="12" cy="12" r="10" />
                                                        <polygon points="10 8 16 12 10 16 10 8" />
                                                    </svg>
                                                </button>
                                            )}
                                            <button className="btn btn-ghost btn-sm" title="View Details">
                                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                                    <circle cx="12" cy="12" r="3" />
                                                </svg>
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
