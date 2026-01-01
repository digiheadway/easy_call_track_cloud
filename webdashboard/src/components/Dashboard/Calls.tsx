import { useState, useEffect } from 'react';
import './Pages.css';
import api from '../../api/client';

interface CallsProps {
    organizationId: string;
}

interface Call {
    id: string;
    employee_name: string;
    contact_name: string;
    phone_number: string;
    direction: 'inbound' | 'outbound';
    duration: string;
    call_timestamp: string;
    status: 'completed' | 'missed' | 'rejected';
    has_recording: number | boolean;
    contact_label?: string; // New field
}

interface CallStats {
    total: number;
    inbound: number;
    outbound: number;
    missed: number;
}

export default function Calls({ organizationId }: CallsProps) {
    const [filterType, setFilterType] = useState<'all' | 'inbound' | 'outbound'>('all');
    const [searchQuery, setSearchQuery] = useState('');
    const [dateFilter, setDateFilter] = useState('today');
    const [labelFilter, setLabelFilter] = useState('');
    const [calls, setCalls] = useState<Call[]>([]);
    const [loading, setLoading] = useState(true);
    const [stats, setStats] = useState<CallStats>({ total: 0, inbound: 0, outbound: 0, missed: 0 });

    // Label Modal State
    const [showLabelModal, setShowLabelModal] = useState(false);
    const [selectedCall, setSelectedCall] = useState<Call | null>(null);
    const [selectedLabel, setSelectedLabel] = useState('');

    useEffect(() => {
        loadCalls();
        loadStats();
    }, [organizationId, filterType, dateFilter, labelFilter]);

    // Debounce search
    useEffect(() => {
        const timer = setTimeout(() => {
            loadCalls();
        }, 500);
        return () => clearTimeout(timer);
    }, [searchQuery]);

    const loadCalls = async () => {
        try {
            setLoading(true);
            const filters: any = {
                dateFilter
            };
            if (filterType !== 'all') {
                filters.direction = filterType;
            }
            if (searchQuery) {
                filters.search = searchQuery;
            }
            if (labelFilter) {
                filters.contactLabel = labelFilter;
            }

            const response = await api.getCalls(filters);
            if (response.status && response.data) {
                const data = Array.isArray(response.data) ? response.data : response.data.calls || [];
                setCalls(data);
            }
        } catch (error) {
            console.error('Failed to load calls:', error);
        } finally {
            setLoading(false);
        }
    };

    const loadStats = async () => {
        try {
            const response = await api.getCallStats();
            if (response.status && response.data) {
                setStats(response.data);
            }
        } catch (error) {
            console.error('Failed to load stats:', error);
        }
    };

    const openLabelModal = (call: Call) => {
        setSelectedCall(call);
        setSelectedLabel(call.contact_label || '');
        setShowLabelModal(true);
    };

    const handleSaveLabel = async () => {
        if (!selectedCall) return;

        try {
            await api.updateContactLabel(selectedCall.phone_number, selectedLabel);
            setShowLabelModal(false);
            loadCalls(); // Reload to update UI
        } catch (error) {
            console.error('Failed to save label:', error);
            alert('Failed to save label');
        }
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
                    <select className="form-select" value={labelFilter} onChange={(e) => setLabelFilter(e.target.value)}>
                        <option value="">All Labels</option>
                        <option value="VIP">VIP</option>
                        <option value="Lead">Lead</option>
                        <option value="Spam">Spam</option>
                        <option value="Customer">Customer</option>
                        <option value="uncategorized">Uncategorized</option>
                    </select>
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
                            {loading && calls.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="text-center py-8 text-muted">
                                        Loading calls...
                                    </td>
                                </tr>
                            ) : calls.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="text-center py-8 text-muted">
                                        No calls found
                                    </td>
                                </tr>
                            ) : (
                                calls.map((call) => (
                                    <tr key={call.id}>
                                        <td>
                                            <div>
                                                <div className="flex items-center gap-2">
                                                    <span className="font-medium">{call.contact_name}</span>
                                                    {call.contact_label && (
                                                        <span className="badge badge-primary text-xs" style={{ fontSize: '0.6rem', padding: '0.1rem 0.4rem' }}>
                                                            {call.contact_label}
                                                        </span>
                                                    )}
                                                </div>
                                                <div className="text-muted text-sm">{call.phone_number}</div>
                                            </div>
                                        </td>
                                        <td>{call.employee_name}</td>
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
                                            {new Date(call.call_timestamp).toLocaleString()}
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
                                                {Boolean(call.has_recording) && (
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
                                                <button className="btn btn-ghost btn-sm" title="Label Person" onClick={() => openLabelModal(call)}>
                                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                        <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" />
                                                        <line x1="7" y1="7" x2="7.01" y2="7" />
                                                    </svg>
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Label Modal */}
            {showLabelModal && selectedCall && (
                <div className="modal-overlay" onClick={() => setShowLabelModal(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>Label Person</h2>
                            <button className="btn btn-ghost" onClick={() => setShowLabelModal(false)}>
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <line x1="18" y1="6" x2="6" y2="18" />
                                    <line x1="6" y1="6" x2="18" y2="18" />
                                </svg>
                            </button>
                        </div>
                        <div className="p-4">
                            <p className="mb-4">
                                Assign a label to <strong>{selectedCall.contact_name}</strong> ({selectedCall.phone_number}).
                                <br />
                                <span className="text-sm text-muted">This will apply to all calls from this person.</span>
                            </p>

                            <div className="form-group">
                                <label className="form-label">Label</label>
                                <select
                                    className="form-select"
                                    value={selectedLabel}
                                    onChange={(e) => setSelectedLabel(e.target.value)}
                                >
                                    <option value="">No Label</option>
                                    <option value="VIP">VIP</option>
                                    <option value="Lead">Lead</option>
                                    <option value="Spam">Spam</option>
                                    <option value="Customer">Customer</option>
                                    <option value="Other">Other</option>
                                </select>
                            </div>

                            <div className="modal-actions mt-4 flex justify-end gap-2">
                                <button className="btn btn-secondary" onClick={() => setShowLabelModal(false)}>Cancel</button>
                                <button className="btn btn-primary" onClick={handleSaveLabel}>Save Label</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
