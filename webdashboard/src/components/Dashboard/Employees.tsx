import { useState, useEffect } from 'react';
import './Pages.css';
import api from '../../api/client';

interface EmployeesProps {
    organizationId: string;
}

interface Employee {
    id: string;
    name: string;
    phone: string;
    join_date: string;
    status: 'active' | 'inactive';
    device_id?: string;
}

export default function Employees({ organizationId }: EmployeesProps) {
    const [showAddModal, setShowAddModal] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [employees, setEmployees] = useState<Employee[]>([]);
    const [loading, setLoading] = useState(true);
    const [formData, setFormData] = useState({
        name: '',
        phone: '',
    });

    useEffect(() => {
        loadEmployees();
    }, [organizationId]);

    const loadEmployees = async () => {
        try {
            setLoading(true);
            const response = await api.getEmployees();
            if (response.status && response.data) {
                // Determine if data is wrapped or direct array
                const data = Array.isArray(response.data) ? response.data : response.data.employees || [];
                setEmployees(data);
            }
        } catch (error) {
            console.error('Failed to load employees:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleAddEmployee = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await api.createEmployee(formData);
            if (response.status) {
                await loadEmployees(); // Reload list
                setFormData({ name: '', phone: '' });
                setShowAddModal(false);
            }
        } catch (error) {
            console.error('Failed to create employee:', error);
            alert('Failed to create employee. Please try again.');
        }
    };

    const filteredEmployees = employees.filter(emp =>
        emp.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        emp.phone.toLowerCase().includes(searchQuery.toLowerCase())
    );

    if (loading && employees.length === 0) {
        return <div className="p-8 text-center">Loading employees...</div>;
    }

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
                        placeholder="Search employees..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </div>
                <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="12" y1="5" x2="12" y2="19" />
                        <line x1="5" y1="12" x2="19" y2="12" />
                    </svg>
                    Add Employee
                </button>
            </div>

            {/* Employee Statistics */}
            <div className="employee-stats">
                <div className="stat-box">
                    <div className="stat-number">{employees.length}</div>
                    <div className="stat-label">Total Employees</div>
                </div>
                <div className="stat-box">
                    <div className="stat-number">{employees.filter(e => e.status === 'active').length}</div>
                    <div className="stat-label">Active</div>
                </div>
            </div>

            {/* Employees Table */}
            <div className="card">
                <div className="table-container">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>Employee</th>
                                <th>Phone</th>
                                <th>Device ID</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredEmployees.length === 0 ? (
                                <tr>
                                    <td colSpan={5} className="text-center py-8 text-muted">
                                        No employees found
                                    </td>
                                </tr>
                            ) : (
                                filteredEmployees.map((employee) => (
                                    <tr key={employee.id}>
                                        <td>
                                            <div className="employee-cell">
                                                <div className="employee-avatar">
                                                    {employee.name.charAt(0)}
                                                </div>
                                                <div>
                                                    <div className="employee-name">{employee.name}</div>
                                                    <div className="employee-meta">Joined {new Date(employee.join_date).toLocaleDateString()}</div>
                                                </div>
                                            </div>
                                        </td>
                                        <td>{employee.phone}</td>
                                        <td>
                                            <span className="text-muted text-sm">{employee.device_id || 'Not linked'}</span>
                                        </td>
                                        <td>
                                            <span className={`badge badge-${employee.status === 'active' ? 'success' : 'error'}`}>
                                                {employee.status}
                                            </span>
                                        </td>
                                        <td>
                                            <div className="action-buttons">
                                                <button className="btn btn-ghost btn-sm">
                                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                                                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                                                    </svg>
                                                </button>
                                                <button className="btn btn-ghost btn-sm text-error">
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

            {/* Add Employee Modal */}
            {showAddModal && (
                <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>Add New Employee</h2>
                            <button className="btn btn-ghost" onClick={() => setShowAddModal(false)}>
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <line x1="18" y1="6" x2="6" y2="18" />
                                    <line x1="6" y1="6" x2="18" y2="18" />
                                </svg>
                            </button>
                        </div>
                        <form onSubmit={handleAddEmployee} className="modal-form">
                            <div className="form-group">
                                <label className="form-label">Full Name</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    value={formData.name}
                                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                    required
                                />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Phone</label>
                                <input
                                    type="tel"
                                    className="form-input"
                                    value={formData.phone}
                                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                                    required
                                />
                            </div>
                            <div className="modal-actions">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowAddModal(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="btn btn-primary">
                                    Add Employee
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
