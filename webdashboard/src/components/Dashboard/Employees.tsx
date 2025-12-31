import { useState } from 'react';
import './Pages.css';

interface EmployeesProps {
    organizationId: string;
}

interface Employee {
    id: string;
    name: string;
    email: string;
    phone: string;
    role: string;
    department: string;
    joinDate: string;
    status: 'active' | 'inactive';
    callsToday: number;
}

export default function Employees({ organizationId }: EmployeesProps) {
    const [showAddModal, setShowAddModal] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [formData, setFormData] = useState({
        name: '',
        email: '',
        phone: '',
        role: '',
        department: '',
    });

    // Mock data
    const [employees, setEmployees] = useState<Employee[]>([
        {
            id: '1',
            name: 'John Smith',
            email: 'john.smith@company.com',
            phone: '+1 234 567 8900',
            role: 'Sales Manager',
            department: 'Sales',
            joinDate: '2024-01-15',
            status: 'active',
            callsToday: 12
        },
        {
            id: '2',
            name: 'Sarah Johnson',
            email: 'sarah.j@company.com',
            phone: '+1 234 567 8901',
            role: 'Marketing Lead',
            department: 'Marketing',
            joinDate: '2024-02-20',
            status: 'active',
            callsToday: 8
        },
        {
            id: '3',
            name: 'David Lee',
            email: 'david.lee@company.com',
            phone: '+1 234 567 8902',
            role: 'Support Agent',
            department: 'Support',
            joinDate: '2024-03-10',
            status: 'active',
            callsToday: 15
        },
        {
            id: '4',
            name: 'Emma Davis',
            email: 'emma.d@company.com',
            phone: '+1 234 567 8903',
            role: 'Sales Representative',
            department: 'Sales',
            joinDate: '2024-01-25',
            status: 'active',
            callsToday: 10
        },
    ]);

    const handleAddEmployee = (e: React.FormEvent) => {
        e.preventDefault();
        const newEmployee: Employee = {
            id: (employees.length + 1).toString(),
            ...formData,
            joinDate: new Date().toISOString().split('T')[0],
            status: 'active',
            callsToday: 0
        };
        setEmployees([...employees, newEmployee]);
        setFormData({ name: '', email: '', phone: '', role: '', department: '' });
        setShowAddModal(false);
    };

    const filteredEmployees = employees.filter(emp =>
        emp.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        emp.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
        emp.department.toLowerCase().includes(searchQuery.toLowerCase())
    );

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
                <div className="stat-box">
                    <div className="stat-number">{employees.reduce((sum, e) => sum + e.callsToday, 0)}</div>
                    <div className="stat-label">Calls Today</div>
                </div>
            </div>

            {/* Employees Table */}
            <div className="card">
                <div className="table-container">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>Employee</th>
                                <th>Contact</th>
                                <th>Department</th>
                                <th>Role</th>
                                <th>Calls Today</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredEmployees.map((employee) => (
                                <tr key={employee.id}>
                                    <td>
                                        <div className="employee-cell">
                                            <div className="employee-avatar">
                                                {employee.name.charAt(0)}
                                            </div>
                                            <div>
                                                <div className="employee-name">{employee.name}</div>
                                                <div className="employee-meta">Joined {new Date(employee.joinDate).toLocaleDateString()}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <div>{employee.email}</div>
                                        <div className="text-muted text-sm">{employee.phone}</div>
                                    </td>
                                    <td>{employee.department}</td>
                                    <td>{employee.role}</td>
                                    <td>
                                        <span className="badge badge-info">{employee.callsToday}</span>
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
                            ))}
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
                                <label className="form-label">Email</label>
                                <input
                                    type="email"
                                    className="form-input"
                                    value={formData.email}
                                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
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
                            <div className="form-group">
                                <label className="form-label">Department</label>
                                <select
                                    className="form-select"
                                    value={formData.department}
                                    onChange={(e) => setFormData({ ...formData, department: e.target.value })}
                                    required
                                >
                                    <option value="">Select Department</option>
                                    <option value="Sales">Sales</option>
                                    <option value="Marketing">Marketing</option>
                                    <option value="Support">Support</option>
                                    <option value="Engineering">Engineering</option>
                                    <option value="Operations">Operations</option>
                                </select>
                            </div>
                            <div className="form-group">
                                <label className="form-label">Role</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    value={formData.role}
                                    onChange={(e) => setFormData({ ...formData, role: e.target.value })}
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
