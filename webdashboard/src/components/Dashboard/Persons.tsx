import { useState, useEffect } from 'react';
import './Pages.css';
import api from '../../api/client';

interface PersonsProps {
    organizationId: string;
}

interface Person {
    id: number;
    name: string;
    phone: string;
    label: string;
    email: string;
    notes: string;
    employee_id?: number | null;
    employee_name?: string;
    incomings?: number;
    incoming_connected?: number;
    outgoings?: number;
    outgoing_connected?: number;
    last_call_type?: string;
    last_call_duration?: number;
    last_call_time?: string;
}

interface Employee {
    id: number;
    name: string;
}

export default function Persons({ organizationId }: PersonsProps) {
    const [showAddModal, setShowAddModal] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [labelFilter, setLabelFilter] = useState('');
    const [persons, setPersons] = useState<Person[]>([]);
    const [employees, setEmployees] = useState<Employee[]>([]);
    const [loading, setLoading] = useState(true);

    // Add/Edit Form State
    const [formData, setFormData] = useState({
        name: '',
        phone: '',
        label: '',
        email: '',
        notes: '',
        employee_id: '' as any
    });

    useEffect(() => {
        loadPersons();
        loadEmployees();
    }, [organizationId, labelFilter]);

    // Debounce search
    useEffect(() => {
        const timer = setTimeout(() => {
            loadPersons();
        }, 500);
        return () => clearTimeout(timer);
    }, [searchQuery]);

    const loadPersons = async () => {
        try {
            setLoading(true);
            const filters: any = {};
            if (searchQuery) filters.search = searchQuery;
            if (labelFilter) filters.label = labelFilter;

            const response = await api.getContacts(filters);
            if (response.status && response.data) {
                setPersons(Array.isArray(response.data) ? response.data : []);
            }
        } catch (error) {
            console.error('Failed to load persons:', error);
        } finally {
            setLoading(false);
        }
    };

    const loadEmployees = async () => {
        try {
            const response = await api.getEmployees();
            if (response.status && response.data) {
                setEmployees(response.data);
            }
        } catch (error) {
            console.error('Failed to load employees:', error);
        }
    };

    const handleSavePerson = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await api.createContact(formData);
            if (response.status) {
                await loadPersons();
                setFormData({ name: '', phone: '', label: '', email: '', notes: '', employee_id: '' as any });
                setShowAddModal(false);
            }
        } catch (error) {
            console.error('Failed to save person:', error);
            alert('Failed to save person');
        }
    };

    const handleDeletePerson = async (id: number) => {
        if (!confirm('Are you sure you want to delete this contact?')) return;
        try {
            await api.deleteContact(id);
            loadPersons();
        } catch (error) {
            console.error('Failed to delete person:', error);
        }
    };

    return (
        <div className="page-container">
            <div className="page-header">
                <div className="search-bar">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="11" cy="11" r="8" />
                        <path d="m21 21-4.35-4.35" />
                    </svg>
                    <input
                        type="text"
                        placeholder="Search persons..."
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
                        <option value="Other">Other</option>
                    </select>
                </div>
                <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="12" y1="5" x2="12" y2="19" />
                        <line x1="5" y1="12" x2="19" y2="12" />
                    </svg>
                    Add Person
                </button>
            </div>

            {/* Persons Table */}
            <div className="card">
                <div className="table-container">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Phone</th>
                                <th>Label</th>
                                <th>Email</th>
                                <th>Assigned To</th>
                                <th>Call Stats</th>
                                <th>Last Call</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading && persons.length === 0 ? (
                                <tr>
                                    <td colSpan={6} className="text-center py-8 text-muted">Loading persons...</td>
                                </tr>
                            ) : persons.length === 0 ? (
                                <tr>
                                    <td colSpan={6} className="text-center py-8 text-muted">No persons found</td>
                                </tr>
                            ) : (
                                persons.map((person) => (
                                    <tr key={person.id}>
                                        <td>
                                            <div>
                                                <div className="flex items-center gap-2">
                                                    <span className="font-medium">{person.name || 'Unknown'}</span>
                                                    {person.label && (
                                                        <span className="badge badge-primary text-xs" style={{ fontSize: '0.6rem', padding: '0.1rem 0.4rem' }}>
                                                            {person.label}
                                                        </span>
                                                    )}
                                                </div>
                                                <div className="text-muted text-xs">{person.phone}</div>
                                                {person.notes && (
                                                    <div className="text-muted text-xs italic mt-1 truncate max-w-xs" title={person.notes}>
                                                        "{person.notes}"
                                                    </div>
                                                )}
                                            </div>
                                        </td>
                                        <td>
                                            {person.label ? (
                                                <span className="badge badge-primary">{person.label}</span>
                                            ) : (
                                                <span className="text-muted text-sm">-</span>
                                            )}
                                        </td>
                                        <td>{person.email || '-'}</td>
                                        <td>
                                            {person.employee_name ? (
                                                <span className="text-sm font-medium">{person.employee_name}</span>
                                            ) : (
                                                <span className="text-muted text-xs">Unassigned</span>
                                            )}
                                        </td>
                                        <td>
                                            <div className="flex flex-col text-xs">
                                                <span className="text-success">In: {person.incomings || 0} ({person.incoming_connected || 0})</span>
                                                <span className="text-info">Out: {person.outgoings || 0} ({person.outgoing_connected || 0})</span>
                                            </div>
                                        </td>
                                        <td>
                                            {person.last_call_time ? (
                                                <div className="flex flex-col text-xs">
                                                    <span className="font-medium">{person.last_call_type}</span>
                                                    <span className="text-muted">{new Date(person.last_call_time).toLocaleDateString()}</span>
                                                    <span className="text-muted">{person.last_call_duration}s</span>
                                                </div>
                                            ) : (
                                                <span className="text-muted text-xs">No calls</span>
                                            )}
                                        </td>
                                        <td>
                                            <div className="action-buttons">
                                                <button className="btn btn-ghost btn-sm" onClick={() => {
                                                    setFormData({
                                                        name: person.name,
                                                        phone: person.phone,
                                                        label: person.label,
                                                        email: person.email,
                                                        notes: person.notes,
                                                        employee_id: person.employee_id || ''
                                                    });
                                                    setShowAddModal(true);
                                                }}>
                                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                                                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                                                    </svg>
                                                </button>
                                                <button className="btn btn-ghost btn-sm text-error" onClick={() => handleDeletePerson(person.id)}>
                                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                        <polyline points="3 6 5 6 21 6" />
                                                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
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

            {/* Add Person Modal */}
            {showAddModal && (
                <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>{formData.phone ? 'Edit Person' : 'Add New Person'}</h2>
                            <button className="btn btn-ghost" onClick={() => setShowAddModal(false)}>
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <line x1="18" y1="6" x2="6" y2="18" />
                                    <line x1="6" y1="6" x2="18" y2="18" />
                                </svg>
                            </button>
                        </div>
                        <form onSubmit={handleSavePerson} className="modal-form">
                            <div className="form-group">
                                <label className="form-label">Phone Number *</label>
                                <input
                                    type="tel"
                                    className="form-input"
                                    value={formData.phone}
                                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                                    required
                                    placeholder="+1234567890"
                                />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Name</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    value={formData.name}
                                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                    placeholder="John Doe"
                                />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Label</label>
                                <select
                                    className="form-select"
                                    value={formData.label}
                                    onChange={(e) => setFormData({ ...formData, label: e.target.value })}
                                >
                                    <option value="">No Label</option>
                                    <option value="VIP">VIP</option>
                                    <option value="Lead">Lead</option>
                                    <option value="Spam">Spam</option>
                                    <option value="Customer">Customer</option>
                                    <option value="Other">Other</option>
                                </select>
                            </div>
                            <div className="form-group">
                                <label className="form-label">Email</label>
                                <input
                                    type="email"
                                    className="form-input"
                                    value={formData.email}
                                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                                />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Notes</label>
                                <textarea
                                    className="form-textarea"
                                    value={formData.notes}
                                    onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                                />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Assigned Employee</label>
                                <select
                                    className="form-select"
                                    value={formData.employee_id}
                                    onChange={(e) => setFormData({ ...formData, employee_id: e.target.value })}
                                >
                                    <option value="">No Employee</option>
                                    {employees.map(emp => (
                                        <option key={emp.id} value={emp.id}>{emp.name}</option>
                                    ))}
                                </select>
                            </div>
                            <div className="modal-actions">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowAddModal(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="btn btn-primary">
                                    Save Person
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
