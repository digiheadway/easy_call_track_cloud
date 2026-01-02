import { useState, useEffect } from 'react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import {
    Plus,
    Search,
    Trash2,
    User,
    Smartphone,
    Copy,
    Check,
    Mic,
    PhoneCall,
    Settings,
    ToggleLeft,
    ToggleRight,
    Calendar,
    UserX,
    CalendarClock,
    CalendarClock,
    SmartphoneNfc,
    AlertTriangle
} from 'lucide-react';
import Modal from '../components/Modal';
import { format } from 'date-fns';
import { toast } from 'sonner';

export default function EmployeesPage() {
    const { user } = useAuth();
    const [employees, setEmployees] = useState([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');

    // Modal State
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingEmployee, setEditingEmployee] = useState(null);
    const [formData, setFormData] = useState({
        name: '',
        track_calls: true,
        track_recordings: true,
        allow_personal_exclusion: false,
        allow_changing_tracking_start_date: false,
        allow_updating_tracking_sims: false,
        default_tracking_starting_date: ''
    });
    const [saving, setSaving] = useState(false);
    const [copiedCode, setCopiedCode] = useState(null);
    const [isProcessing, setIsProcessing] = useState(false);

    // Confirmation Modal State
    const [confirmModal, setConfirmModal] = useState({
        isOpen: false,
        title: '',
        message: '',
        action: null,
        isDestructive: false
    });

    useEffect(() => {
        fetchEmployees();
    }, []);

    const fetchEmployees = async () => {
        setLoading(true);
        try {
            const res = await api.get('/employees.php');
            if (res.data && Array.isArray(res.data)) {
                const formatted = res.data.map(e => ({
                    ...e,
                    track_calls: e.track_calls == 1 || e.track_calls === true,
                    track_recordings: e.track_recordings == 1 || e.track_recordings === true,
                    allow_personal_exclusion: e.allow_personal_exclusion == 1 || e.allow_personal_exclusion === true,
                    allow_changing_tracking_start_date: e.allow_changing_tracking_start_date == 1 || e.allow_changing_tracking_start_date === true,
                    allow_updating_tracking_sims: e.allow_updating_tracking_sims == 1 || e.allow_updating_tracking_sims === true,
                    plan_expiry: e.plan_expiry ? new Date(e.plan_expiry) : null,
                    tracking_started: e.created_at ? new Date(e.created_at) : null
                }));
                setEmployees(formatted);
            }
        } catch (err) {
            console.error("Failed to fetch employees", err);
            toast.error("Failed to fetch employees");
        } finally {
            setLoading(false);
        }
    };

    const handleOpenModal = (employee = null) => {
        if (employee) {
            setEditingEmployee(employee);
            setFormData({
                name: employee.name || '',
                track_calls: employee.track_calls,
                track_recordings: employee.track_recordings,
                allow_personal_exclusion: employee.allow_personal_exclusion,
                allow_changing_tracking_start_date: employee.allow_changing_tracking_start_date,
                allow_updating_tracking_sims: employee.allow_updating_tracking_sims,
                default_tracking_starting_date: employee.default_tracking_starting_date ? employee.default_tracking_starting_date.split(' ')[0] : ''
            });
        } else {
            setEditingEmployee(null);
            setFormData({
                name: '',
                track_calls: true,
                track_recordings: true,
                allow_personal_exclusion: false,
                allow_changing_tracking_start_date: false,
                allow_updating_tracking_sims: false,
                default_tracking_starting_date: ''
            });
        }
        setIsModalOpen(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            if (editingEmployee) {
                await api.put(`/employees.php?id=${editingEmployee.id}`, {
                    name: formData.name,
                    track_calls: formData.track_calls,
                    track_recordings: formData.track_recordings,
                    allow_personal_exclusion: formData.allow_personal_exclusion ? 1 : 0,
                    allow_changing_tracking_start_date: formData.allow_changing_tracking_start_date ? 1 : 0,
                    allow_updating_tracking_sims: formData.allow_updating_tracking_sims ? 1 : 0,
                    default_tracking_starting_date: formData.default_tracking_starting_date
                });
                toast.success('Employee updated successfully');
            } else {
                await api.post('/employees.php', formData);
                toast.success('Employee created successfully');
            }
            setIsModalOpen(false);
            fetchEmployees();
        } catch (err) {
            console.error(err);
            toast.error('Failed to save. ' + (err.response?.data?.message || err.message));
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = (id) => {
        setConfirmModal({
            isOpen: true,
            title: 'Delete Employee',
            message: 'Are you sure you want to delete this employee? This action cannot be undone.',
            isDestructive: true,
            onConfirm: async () => {
                try {
                    await api.delete(`/employees.php?id=${id}`);
                    toast.success('Employee deleted');
                    fetchEmployees();
                } catch (err) {
                    console.error(err);
                    toast.error('Failed to delete employee.');
                }
            }
        });
    };

    const copyToClipboard = (code) => {
        if (!code) return;
        navigator.clipboard.writeText(code);
        setCopiedCode(code);
        setTimeout(() => setCopiedCode(null), 2000);
        toast.success('Code copied to clipboard');
    };

    const filteredEmployees = employees.filter(emp =>
        emp.name?.toLowerCase().includes(search.toLowerCase()) ||
        emp.pairing_code?.toLowerCase().includes(search.toLowerCase()) ||
        emp.device_phone?.includes(search)
    );

    // Helper to get display pairing code
    const getPairingCode = (emp) => {
        // Use pairing_code if available, otherwise construct from OrgID-EmpID
        if (emp.pairing_code) return emp.pairing_code;
        if (user?.org_id) return `${user.org_id}-${emp.id}`;
        return '----';
    };

    return (
        <div className="space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight">Employees</h1>
                    <p className="text-gray-500 text-sm mt-1">Manage pairing codes and tracking configurations.</p>
                </div>

                <div className="flex items-center gap-3 w-full sm:w-auto">
                    <div className="relative flex-1 sm:w-64">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                        <input
                            type="text"
                            placeholder="Search..."
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            className="pl-9 pr-4 py-2 text-sm border border-gray-200 rounded-lg w-full focus:ring-2 focus:ring-blue-500 outline-none"
                        />
                    </div>
                    <button
                        onClick={() => handleOpenModal()}
                        className="btn btn-primary whitespace-nowrap gap-2"
                    >
                        <Plus size={18} />
                        Add Employee
                    </button>
                </div>
            </div>

            <div className="card !p-0 overflow-hidden border border-gray-200 shadow-sm">
                <div className="overflow-x-auto">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-[#f8fafc] border-b border-gray-100 text-gray-500 font-medium whitespace-nowrap">
                            <tr>
                                <th className="px-6 py-4">Employee</th>
                                <th className="px-6 py-4">Pairing Code</th>
                                <th className="px-6 py-4">Plan Expiry</th>
                                <th className="px-6 py-4">Tracking Started</th>
                                <th className="px-6 py-4">Device Info</th>
                                <th className="px-6 py-4">Configuration</th>
                                <th className="px-6 py-4 text-right">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {filteredEmployees.map((emp) => {
                                const pairingCode = getPairingCode(emp);
                                return (
                                    <tr key={emp.id} className="hover:bg-gray-50 transition-colors group">
                                        <td className="px-6 py-4">
                                            <div className="flex items-center gap-3">
                                                <div className="w-10 h-10 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm">
                                                    {emp.name?.substring(0, 2).toUpperCase()}
                                                </div>
                                                <div>
                                                    <div className="font-medium text-gray-900">{emp.name}</div>
                                                </div>
                                            </div>
                                        </td>
                                        <td className="px-6 py-4">
                                            <div className="flex items-center gap-2">
                                                <code className="bg-gray-100 px-2.5 py-1 rounded-md text-sm font-mono font-bold text-gray-700 tracking-wide border border-gray-200">
                                                    {pairingCode}
                                                </code>
                                                <button
                                                    onClick={() => copyToClipboard(pairingCode)}
                                                    className="p-1.5 hover:bg-gray-200 rounded-md text-gray-400 hover:text-blue-600 transition-colors"
                                                    title="Copy Code"
                                                >
                                                    {copiedCode === pairingCode ? <Check size={14} className="text-green-600" /> : <Copy size={14} />}
                                                </button>
                                            </div>
                                        </td>
                                        <td className="px-6 py-4">
                                            {emp.plan_expiry ? (
                                                <div className="flex items-center gap-2 text-gray-600">
                                                    <Calendar size={14} className="text-gray-400" />
                                                    <span>{format(emp.plan_expiry, 'MMM d, yyyy')}</span>
                                                </div>
                                            ) : (
                                                <span className="text-xs text-gray-400 italic">No plan active</span>
                                            )}
                                        </td>
                                        <td className="px-6 py-4">
                                            {emp.tracking_started ? (
                                                <div className="text-gray-600 text-xs">
                                                    {format(emp.tracking_started, 'MMM d, yyyy')}
                                                </div>
                                            ) : (
                                                <span className="text-xs text-gray-400">-</span>
                                            )}
                                        </td>
                                        <td className="px-6 py-4">
                                            <div className="flex flex-col gap-1">
                                                <div className="flex items-center gap-2 text-gray-700">
                                                    <Smartphone size={14} className="text-gray-400" />
                                                    <span>{emp.device_phone || 'No phone'}</span>
                                                </div>
                                                {emp.device_model && (
                                                    <span className="text-xs text-gray-400 pl-6">{emp.device_model}</span>
                                                )}
                                            </div>
                                        </td>
                                        <td className="px-6 py-4">
                                            <div className="flex items-center gap-2">
                                                <div title="Call Tracking" className={`w-8 h-8 rounded-full flex items-center justify-center border ${emp.track_calls
                                                    ? 'bg-blue-50 text-blue-600 border-blue-200'
                                                    : 'bg-gray-50 text-gray-400 border-gray-200'
                                                    }`}>
                                                    <PhoneCall size={14} />
                                                </div>
                                                <div title="Recording Tracking" className={`w-8 h-8 rounded-full flex items-center justify-center border ${emp.track_recordings
                                                    ? 'bg-purple-50 text-purple-600 border-purple-200'
                                                    : 'bg-gray-50 text-gray-400 border-gray-200'
                                                    }`}>
                                                    <Mic size={14} />
                                                </div>
                                            </div>
                                        </td>
                                        <td className="px-6 py-4 text-right">
                                            <div className="flex items-center justify-end gap-2 text-gray-400 opacity-0 group-hover:opacity-100 transition-opacity">
                                                <button
                                                    onClick={() => handleOpenModal(emp)}
                                                    className="p-2 hover:bg-gray-100 rounded-lg hover:text-blue-600 transition-colors"
                                                    title="Configure"
                                                >
                                                    <Settings size={18} />
                                                </button>
                                                <button
                                                    onClick={() => handleDelete(emp.id)}
                                                    className="p-2 hover:bg-gray-100 rounded-lg hover:text-red-600 transition-colors"
                                                    title="Delete"
                                                >
                                                    <Trash2 size={18} />
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                )
                            })}
                            {!loading && filteredEmployees.length === 0 && (
                                <tr>
                                    <td colSpan="7" className="p-12 text-center">
                                        <div className="flex flex-col items-center justify-center text-gray-400">
                                            <User size={48} className="mb-4 text-gray-300" />
                                            <p className="text-lg font-medium text-gray-500">No employees found</p>
                                            <p className="text-sm">Add an employee to generate a pairing code.</p>
                                        </div>
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            <Modal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                title={editingEmployee ? 'Configure Employee' : 'Add New Employee'}
            >
                <form onSubmit={handleSubmit} className="flex flex-col max-h-[80vh]">
                    <div className="overflow-y-auto px-1 -mx-1 mb-4 space-y-6">
                        <div>
                            <label className="block text-sm font-medium mb-1.5 text-gray-700">Display Name</label>
                            <input
                                type="text"
                                required
                                className="input"
                                value={formData.name}
                                onChange={e => setFormData({ ...formData, name: e.target.value })}
                                placeholder="e.g. Sales Representative 1"
                            />
                        </div>

                        {editingEmployee && (
                            <div className="space-y-4 pt-2 border-t border-gray-100">
                                <h4 className="text-sm font-medium text-gray-900">App Configuration</h4>

                                <label className="flex items-center justify-between cursor-pointer p-3 rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors">
                                    <div className="flex items-center gap-3">
                                        <div className={`p-2 rounded-md ${formData.track_calls ? 'bg-blue-100 text-blue-600' : 'bg-gray-100 text-gray-500'}`}>
                                            <PhoneCall size={20} />
                                        </div>
                                        <div>
                                            <div className="text-sm font-medium text-gray-900">Track Calls</div>
                                            <div className="text-xs text-gray-500">Sync call logs from this device</div>
                                        </div>
                                    </div>
                                    <div onClick={() => setFormData(p => ({ ...p, track_calls: !p.track_calls }))}>
                                        {formData.track_calls ?
                                            <ToggleRight size={28} className="text-blue-600" /> :
                                            <ToggleLeft size={28} className="text-gray-400" />
                                        }
                                    </div>
                                </label>

                                <label className="flex items-center justify-between cursor-pointer p-3 rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors">
                                    <div className="flex items-center gap-3">
                                        <div className={`p-2 rounded-md ${formData.track_recordings ? 'bg-purple-100 text-purple-600' : 'bg-gray-100 text-gray-500'}`}>
                                            <Mic size={20} />
                                        </div>
                                        <div>
                                            <div className="text-sm font-medium text-gray-900">Track Recordings</div>
                                            <div className="text-xs text-gray-500">Upload audio recordings (if available)</div>
                                        </div>
                                    </div>
                                    <div onClick={() => setFormData(p => ({ ...p, track_recordings: !p.track_recordings }))}>
                                        {formData.track_recordings ?
                                            <ToggleRight size={28} className="text-purple-600" /> :
                                            <ToggleLeft size={28} className="text-gray-400" />
                                        }
                                    </div>
                                </label>

                                <div className="pt-4 border-t border-gray-100">
                                    <h4 className="text-sm font-medium text-gray-900 mb-4">Enterprise Controls</h4>

                                    <div className="space-y-3">
                                        <label className="flex items-center justify-between cursor-pointer p-3 rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors">
                                            <div className="flex items-center gap-3">
                                                <div className={`p-2 rounded-md ${formData.allow_personal_exclusion ? 'bg-orange-100 text-orange-600' : 'bg-gray-100 text-gray-500'}`}>
                                                    <UserX size={18} />
                                                </div>
                                                <div>
                                                    <div className="text-sm font-medium text-gray-900">Personal Exclusion</div>
                                                    <div className="text-xs text-gray-500">Allow employee to exclude personal contacts</div>
                                                </div>
                                            </div>
                                            <div onClick={() => setFormData(p => ({ ...p, allow_personal_exclusion: !p.allow_personal_exclusion }))}>
                                                {formData.allow_personal_exclusion ?
                                                    <ToggleRight size={24} className="text-orange-600" /> :
                                                    <ToggleLeft size={24} className="text-gray-400" />
                                                }
                                            </div>
                                        </label>

                                        <label className="flex items-center justify-between cursor-pointer p-3 rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors">
                                            <div className="flex items-center gap-3">
                                                <div className={`p-2 rounded-md ${formData.allow_changing_tracking_start_date ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-500'}`}>
                                                    <CalendarClock size={18} />
                                                </div>
                                                <div>
                                                    <div className="text-sm font-medium text-gray-900">Change Start Date</div>
                                                    <div className="text-xs text-gray-500">Allow employee to change tracking start date</div>
                                                </div>
                                            </div>
                                            <div onClick={() => setFormData(p => ({ ...p, allow_changing_tracking_start_date: !p.allow_changing_tracking_start_date }))}>
                                                {formData.allow_changing_tracking_start_date ?
                                                    <ToggleRight size={24} className="text-green-600" /> :
                                                    <ToggleLeft size={24} className="text-gray-400" />
                                                }
                                            </div>
                                        </label>

                                        <label className="flex items-center justify-between cursor-pointer p-3 rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors">
                                            <div className="flex items-center gap-3">
                                                <div className={`p-2 rounded-md ${formData.allow_updating_tracking_sims ? 'bg-indigo-100 text-indigo-600' : 'bg-gray-100 text-gray-500'}`}>
                                                    <SmartphoneNfc size={18} />
                                                </div>
                                                <div>
                                                    <div className="text-sm font-medium text-gray-900">Update Tracked SIMs</div>
                                                    <div className="text-xs text-gray-500">Allow employee to change which SIMs are tracked</div>
                                                </div>
                                            </div>
                                            <div onClick={() => setFormData(p => ({ ...p, allow_updating_tracking_sims: !p.allow_updating_tracking_sims }))}>
                                                {formData.allow_updating_tracking_sims ?
                                                    <ToggleRight size={24} className="text-indigo-600" /> :
                                                    <ToggleLeft size={24} className="text-gray-400" />
                                                }
                                            </div>
                                        </label>

                                        <div className="p-3 rounded-lg border border-gray-200 bg-gray-50">
                                            <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Default Tracking Start Date</label>
                                            <div className="flex items-center gap-3">
                                                <div className="p-2 bg-white rounded border border-gray-200 text-gray-400">
                                                    <Calendar size={18} />
                                                </div>
                                                <input
                                                    type="date"
                                                    className="bg-transparent border-none text-sm font-medium focus:ring-0 outline-none w-full"
                                                    value={formData.default_tracking_starting_date}
                                                    onChange={e => setFormData(p => ({ ...p, default_tracking_starting_date: e.target.value }))}
                                                />
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}

                        {!editingEmployee && (
                            <div className="bg-blue-50 text-blue-700 p-4 rounded-lg text-sm">
                                A pairing code will be generated automatically after you create this employee.
                            </div>
                        )}
                    </div>

                    <div className="pt-2 flex gap-3 border-t border-gray-100 mt-auto">
                        <button
                            type="button"
                            onClick={() => setIsModalOpen(false)}
                            className="flex-1 btn bg-gray-100 text-gray-700 hover:bg-gray-200"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={saving}
                            className="flex-1 btn btn-primary"
                        >
                            {saving ? 'Saving...' : (editingEmployee ? 'Update Setup' : 'Create & Generate Code')}
                        </button>
                    </div>
                </form>
            </Modal>

            {/* Confirmation Modal */}
            <Modal
                isOpen={confirmModal.isOpen}
                onClose={() => setConfirmModal({ ...confirmModal, isOpen: false })}
                title={confirmModal.title}
            >
                <div className="space-y-4">
                    <div className="flex items-start gap-3 p-3 bg-orange-50 rounded-lg text-orange-800 border border-orange-100">
                        <AlertTriangle className="shrink-0 mt-0.5" size={18} />
                        <p className="text-sm font-medium">{confirmModal.message}</p>
                    </div>
                    <div className="flex gap-3 pt-2">
                        <button
                            type="button"
                            onClick={() => setConfirmModal({ ...confirmModal, isOpen: false })}
                            className="flex-1 btn bg-gray-100 text-gray-700 hover:bg-gray-200"
                        >
                            Cancel
                        </button>
                        <button
                            type="button"
                            onClick={async () => {
                                setIsProcessing(true);
                                const onConfirm = confirmModal.onConfirm;
                                setConfirmModal({ ...confirmModal, isOpen: false });
                                await onConfirm();
                                setIsProcessing(false);
                            }}
                            disabled={isProcessing}
                            className={`flex-1 btn ${confirmModal.isDestructive ? 'bg-red-600 hover:bg-red-700 text-white' : 'btn-primary'}`}
                        >
                            {isProcessing ? 'Processing...' : 'Confirm'}
                        </button>
                    </div>
                </div>
            </Modal>
        </div>
    );
}
