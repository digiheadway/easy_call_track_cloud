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
    RefreshCw,
    ToggleLeft,
    ToggleRight,
    Calendar,
    UserX,
    CalendarClock,
    SmartphoneNfc,
    AlertTriangle,
    Archive,
    Database,
    HardDrive,
    Loader2,
    CheckCircle2
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
    const [isControlsModalOpen, setIsControlsModalOpen] = useState(false);
    const [isTrackingModalOpen, setIsTrackingModalOpen] = useState(false);
    const [isDeviceModalOpen, setIsDeviceModalOpen] = useState(false);
    const [selectedEmployee, setSelectedEmployee] = useState(null);
    const [isProcessing, setIsProcessing] = useState(false);

    // Confirmation Modal State
    const [confirmModal, setConfirmModal] = useState({
        isOpen: false,
        title: '',
        message: '',
        action: null,
        isDestructive: false
    });

    // Deletion Modal State
    const [isDeletionModalOpen, setIsDeletionModalOpen] = useState(false);
    const [deletionEmployee, setDeletionEmployee] = useState(null);
    const [deletionStats, setDeletionStats] = useState(null);
    const [loadingStats, setLoadingStats] = useState(false);
    const [deletingCalls, setDeletingCalls] = useState(false);
    const [deletingRecordings, setDeletingRecordings] = useState(false);
    const [callsDeleted, setCallsDeleted] = useState(false);
    const [recordingsDeleted, setRecordingsDeleted] = useState(false);

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
                    last_sync: e.last_sync ? new Date(e.last_sync.replace(' ', 'T') + 'Z') : null,
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

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingEmployee, setEditingEmployee] = useState(null);
    const [formData, setFormData] = useState({
        name: '',
        phone: '',
        track_calls: true,
        track_recordings: true,
        allow_personal_exclusion: false,
        allow_changing_tracking_start_date: false,
        allow_updating_tracking_sims: false,
        default_tracking_starting_date: ''
    });
    const [saving, setSaving] = useState(false);
    const [copiedCode, setCopiedCode] = useState(null);

    const handleOpenModal = (employee = null) => {
        if (employee) {
            setEditingEmployee(employee);
            setFormData({
                name: employee.name || '',
                phone: employee.phone || '',
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
                phone: '',
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

    const toggleTracking = async (employee, type) => {
        try {
            const key = type === 'calls' ? 'track_calls' : 'track_recordings';
            const newValue = !employee[key];

            // Update local state first for instant feedback
            setEmployees(prev => prev.map(e => e.id === employee.id ? { ...e, [key]: newValue } : e));

            await api.put(`/employees.php?id=${employee.id}`, {
                [key]: newValue
            });
            toast.success(`${type === 'calls' ? 'Call' : 'Recording'} tracking updated`);
        } catch (err) {
            console.error(err);
            toast.error('Failed to update tracking');
            fetchEmployees(); // Rollback
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            if (editingEmployee) {
                await api.put(`/employees.php?id=${editingEmployee.id}`, {
                    name: formData.name,
                    phone: formData.phone,
                    track_calls: formData.track_calls,
                    track_recordings: formData.track_recordings,
                    allow_personal_exclusion: formData.allow_personal_exclusion ? 1 : 0,
                    allow_changing_tracking_start_date: formData.allow_changing_tracking_start_date ? 1 : 0,
                    allow_updating_tracking_sims: formData.allow_updating_tracking_sims ? 1 : 0,
                    default_tracking_starting_date: formData.default_tracking_starting_date
                });
                toast.success('Employee updated successfully');
            } else {
                await api.post('/employees.php', {
                    ...formData,
                    phone: formData.phone
                });
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

    // Format bytes for display
    const formatBytes = (bytes, decimals = 2) => {
        if (!bytes || bytes === 0) return '0 Bytes';
        const k = 1024;
        const dm = decimals < 0 ? 0 : decimals;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    };

    const openDeletionModal = async (employee) => {
        setDeletionEmployee(employee);
        setDeletionStats(null);
        setCallsDeleted(false);
        setRecordingsDeleted(false);
        setIsDeletionModalOpen(true);

        // Fetch stats
        setLoadingStats(true);
        try {
            const res = await api.get(`/employees.php?action=data_stats&id=${employee.id}`);
            console.log('Stats Response:', res);
            if (res.data) {
                setDeletionStats(res.data);
            } else {
                console.error('No data in stats response');
            }
        } catch (err) {
            console.error('Failed to fetch deletion stats', err);
            toast.error('Failed to fetch employee data stats');
        } finally {
            setLoadingStats(false);
        }
    };

    const handleDeleteCalls = async () => {
        if (!deletionEmployee) return;
        setDeletingCalls(true);
        try {
            await api.get(`/employees.php?action=delete_calls&id=${deletionEmployee.id}`);
            setCallsDeleted(true);
            setDeletionStats(prev => ({ ...prev, calls_count: 0, contacts_count: 0 }));
            toast.success('Calls and contacts deleted');
        } catch (err) {
            console.error('Failed to delete calls', err);
            toast.error('Failed to delete calls');
        } finally {
            setDeletingCalls(false);
        }
    };

    const handleDeleteRecordings = async () => {
        if (!deletionEmployee) return;
        setDeletingRecordings(true);
        try {
            const res = await api.get(`/employees.php?action=delete_recordings&id=${deletionEmployee.id}`);
            setRecordingsDeleted(true);
            setDeletionStats(prev => ({ ...prev, recordings_count: 0, recordings_size_bytes: 0 }));
            toast.success(`Recordings deleted, freed ${formatBytes(res.data?.bytes_freed || 0)}`);
        } catch (err) {
            console.error('Failed to delete recordings', err);
            toast.error('Failed to delete recordings');
        } finally {
            setDeletingRecordings(false);
        }
    };

    const handleDeleteEmployee = async () => {
        if (!deletionEmployee) return;

        setConfirmModal({
            isOpen: true,
            title: 'Delete Employee Permanently?',
            message: `Are you sure you want to delete ${deletionEmployee.name}? This action cannot be undone.`,
            isDestructive: true,
            onConfirm: async () => {
                try {
                    await api.delete(`/employees.php?id=${deletionEmployee.id}`);
                    toast.success('Employee deleted permanently');
                    setIsDeletionModalOpen(false);
                    fetchEmployees();
                } catch (err) {
                    console.error('Failed to delete employee', err);
                    toast.error('Failed to delete employee');
                }
            }
        });
    };

    const handleArchiveEmployee = async () => {
        if (!deletionEmployee) return;
        try {
            await api.get(`/employees.php?action=archive&id=${deletionEmployee.id}`);
            toast.success('Employee archived');
            setIsDeletionModalOpen(false);
            fetchEmployees();
        } catch (err) {
            console.error('Failed to archive employee', err);
            toast.error('Failed to archive employee');
        }
    };

    const handleDelete = (employee) => {
        openDeletionModal(employee);
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
                    <h1 className="text-2xl font-bold tracking-tight flex items-center gap-3">
                        Employees
                        <span className="px-2 py-0.5 bg-gray-100 text-gray-500 text-[10px] font-black rounded-full uppercase tracking-tighter">
                            {employees.length} / {user?.allowed_users_count || 0} Slots
                        </span>
                    </h1>
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
                            className="pl-9 pr-4 py-2 text-sm border border-gray-200 dark:border-gray-700 rounded-lg w-full focus:ring-2 focus:ring-blue-500 outline-none bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 placeholder-gray-400"
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

            <div className="card !p-0 overflow-hidden border border-gray-200 dark:border-gray-700 shadow-sm">
                <div className="overflow-x-auto">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-[#f8fafc] dark:bg-gray-900/50 border-b border-gray-100 dark:border-gray-800 text-gray-500 dark:text-gray-400 font-medium whitespace-nowrap">
                            <tr>
                                <th className="px-6 py-4">Employee</th>
                                <th className="px-6 py-4">Pairing Code</th>
                                <th className="px-6 py-4">Last Sync</th>
                                <th className="px-6 py-4">Tracking</th>
                                <th className="px-6 py-4">Other</th>
                                <th className="px-6 py-4 text-right">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                            {filteredEmployees.map((emp) => {
                                const pairingCode = getPairingCode(emp);
                                return (
                                    <tr key={emp.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors group">
                                        <td className="px-6 py-4">
                                            <div
                                                className="flex items-center gap-3 cursor-pointer hover:opacity-70 transition-opacity"
                                                onClick={() => handleOpenModal(emp)}
                                            >
                                                <div className="w-10 h-10 rounded-full bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 flex items-center justify-center font-bold text-sm shadow-sm">
                                                    {emp.name?.substring(0, 2).toUpperCase()}
                                                </div>
                                                <div>
                                                    <div className="font-bold text-gray-900 dark:text-gray-100 flex items-center gap-1.5">
                                                        {emp.name}
                                                        <Settings size={12} className="text-gray-300 dark:text-gray-600 group-hover:text-blue-400" />
                                                    </div>
                                                    <div className="text-[10px] text-gray-400 font-black uppercase tracking-widest">Click to Edit</div>
                                                </div>
                                            </div>
                                        </td>
                                        <td className="px-6 py-4">
                                            <div className="flex items-center gap-2">
                                                <code className="bg-gray-50 dark:bg-gray-800 px-2 py-1 rounded-md text-sm font-mono font-black text-gray-600 dark:text-gray-300 tracking-wider border border-gray-100 dark:border-gray-700 shadow-sm">
                                                    {pairingCode}
                                                </code>
                                                <button
                                                    onClick={() => copyToClipboard(pairingCode)}
                                                    className="p-1.5 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-md text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
                                                >
                                                    {copiedCode === pairingCode ? <Check size={14} className="text-green-600" /> : <Copy size={14} />}
                                                </button>
                                            </div>
                                        </td>
                                        <td className="px-6 py-4">
                                            {emp.last_sync ? (
                                                <div className="flex flex-col">
                                                    <div className="flex items-center gap-2 text-gray-900 dark:text-gray-100 font-bold whitespace-nowrap">
                                                        <RefreshCw size={12} className="text-blue-500" />
                                                        <span>{format(emp.last_sync, 'h:mm a')}</span>
                                                    </div>
                                                    <div className="text-[10px] text-gray-400 font-medium whitespace-nowrap">
                                                        {format(emp.last_sync, 'MMM d, yyyy')}
                                                    </div>
                                                </div>
                                            ) : (
                                                <span className="text-xs text-gray-400 italic">Never synced</span>
                                            )}
                                        </td>
                                        <td className="px-6 py-4">
                                            <button
                                                onClick={(e) => { e.stopPropagation(); setSelectedEmployee(emp); setIsTrackingModalOpen(true); }}
                                                className="group/track flex items-center gap-2 p-2 hover:bg-gray-50 dark:hover:bg-gray-800 rounded-xl transition-all border border-transparent hover:border-gray-100 dark:hover:border-gray-700"
                                            >
                                                <div className={`w-8 h-8 rounded-lg flex items-center justify-center transition-all ${emp.track_calls ? 'bg-blue-600 text-white shadow-sm' : 'bg-gray-100 dark:bg-gray-800 text-gray-400'}`}>
                                                    <PhoneCall size={14} />
                                                </div>
                                                <div className={`w-8 h-8 rounded-lg flex items-center justify-center transition-all ${emp.track_recordings ? 'bg-purple-600 text-white shadow-sm' : 'bg-gray-100 dark:bg-gray-800 text-gray-400'}`}>
                                                    <Mic size={14} />
                                                </div>
                                                <Settings size={12} className="text-gray-300 dark:text-gray-600 group-hover/track:text-blue-500 transition-colors ml-1" />
                                            </button>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            <div className="flex items-center gap-2">
                                                <button
                                                    onClick={(e) => { e.stopPropagation(); setSelectedEmployee(emp); setIsDeviceModalOpen(true); }}
                                                    className="px-3 py-1.5 bg-gray-50 dark:bg-gray-800 border border-gray-100 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-lg text-[10px] font-black uppercase tracking-wider hover:bg-blue-50 dark:hover:bg-blue-900/20 hover:text-blue-600 dark:hover:text-blue-400 hover:border-blue-100 dark:hover:border-blue-900/30 transition-all shadow-sm active:scale-95 flex items-center gap-1.5"
                                                >
                                                    <Smartphone size={14} />
                                                    Device
                                                </button>
                                                <button
                                                    onClick={(e) => { e.stopPropagation(); setSelectedEmployee(emp); setIsControlsModalOpen(true); }}
                                                    className="px-3 py-1.5 bg-gray-50 dark:bg-gray-800 border border-gray-100 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-lg text-[10px] font-black uppercase tracking-wider hover:bg-orange-50 dark:hover:bg-orange-900/20 hover:text-orange-600 dark:hover:text-orange-400 hover:border-orange-100 dark:hover:border-orange-900/30 transition-all shadow-sm active:scale-95 flex items-center gap-1.5"
                                                >
                                                    <Settings size={14} />
                                                    Controls
                                                </button>
                                            </div>
                                        </td>
                                        <td className="px-6 py-4 text-right">
                                            <button
                                                onClick={(e) => { e.stopPropagation(); handleDelete(emp); }}
                                                className="p-2 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg text-gray-300 dark:text-gray-600 hover:text-red-500 dark:hover:text-red-400 transition-all active:scale-95"
                                                title="Delete"
                                            >
                                                <Trash2 size={18} />
                                            </button>
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



                        {!editingEmployee && (
                            <div className="bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300 p-4 rounded-lg text-sm">
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
                    <div className="flex items-start gap-3 p-3 bg-orange-50 dark:bg-orange-900/20 rounded-lg text-orange-800 dark:text-orange-300 border border-orange-100 dark:border-orange-900/30">
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

            {/* Device Info Modal */}
            <Modal
                isOpen={isDeviceModalOpen}
                onClose={() => setIsDeviceModalOpen(false)}
                title="Device Information"
            >
                <div className="space-y-6">
                    <div className="flex items-center gap-4 p-5 bg-blue-50 dark:bg-blue-900/20 rounded-[2rem] border border-blue-100 dark:border-blue-900/30">
                        <div className="w-14 h-14 bg-white dark:bg-blue-900 rounded-2xl flex items-center justify-center text-blue-600 dark:text-blue-400 shadow-sm border border-blue-100 dark:border-blue-800">
                            <Smartphone size={32} />
                        </div>
                        <div>
                            <h4 className="font-black text-gray-900 dark:text-white">{selectedEmployee?.device_model || 'Unknown Device'}</h4>
                            <p className="text-xs text-blue-600 dark:text-blue-400 font-bold uppercase tracking-wider">Device Linked</p>
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="p-4 bg-gray-50 dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700">
                            <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Phone Number</p>
                            <p className="text-sm font-bold text-gray-900 dark:text-gray-100">{selectedEmployee?.device_phone || 'N/A'}</p>
                        </div>
                        <div className="p-4 bg-gray-50 dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700">
                            <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">OS Version</p>
                            <p className="text-sm font-bold text-gray-900 dark:text-gray-100">Android {selectedEmployee?.os_version || 'N/A'}</p>
                        </div>
                        <div className="p-4 bg-gray-50 dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700">
                            <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Battery Level</p>
                            <div className="flex items-center gap-2">
                                <p className="text-sm font-bold text-gray-900 dark:text-gray-100">{selectedEmployee?.battery_level != null ? `${selectedEmployee.battery_level}%` : 'N/A'}</p>
                                {selectedEmployee?.battery_level != null && (
                                    <div className="w-8 h-4 bg-gray-200 dark:bg-gray-700 rounded-sm p-0.5 relative">
                                        <div
                                            className={`h-full rounded-sm ${selectedEmployee.battery_level > 20 ? 'bg-green-500' : 'bg-red-500'}`}
                                            style={{ width: `${selectedEmployee.battery_level}%` }}
                                        />
                                    </div>
                                )}
                            </div>
                        </div>
                        <div className="p-4 bg-gray-50 dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700 col-span-2">
                            <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Device ID</p>
                            <p className="text-[10px] font-mono font-bold text-gray-900 dark:text-gray-100 break-all">{selectedEmployee?.device_id || 'N/A'}</p>
                        </div>
                        <div className="p-4 bg-gray-50 dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700 col-span-2">
                            <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Last Synchronization</p>
                            <p className="text-sm font-bold text-gray-900 dark:text-gray-100">
                                {selectedEmployee?.last_sync ? format(selectedEmployee.last_sync, 'MMM d, yyyy h:mm a') : 'Never'}
                            </p>
                        </div>
                    </div>

                    <button
                        onClick={() => setIsDeviceModalOpen(false)}
                        className="w-full btn btn-primary mt-4"
                    >
                        Close Details
                    </button>
                </div>
            </Modal>

            {/* App Controls Modal */}
            <Modal
                isOpen={isControlsModalOpen}
                onClose={() => setIsControlsModalOpen(false)}
                title="App & Enterprise Controls"
            >
                <div className="space-y-6">
                    <p className="text-xs text-gray-500 dark:text-gray-400 font-medium bg-orange-50 dark:bg-orange-900/20 p-3 rounded-xl border border-orange-100 dark:border-orange-900/30 text-orange-700 dark:text-orange-300">
                        Configure advanced tracking permissions for <strong>{selectedEmployee?.name || 'Employee'}</strong>.
                    </p>

                    <div className="space-y-3">
                        <label className="flex items-center justify-between cursor-pointer p-4 rounded-2xl border border-gray-100 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800 transition-all group">
                            <div className="flex items-center gap-3">
                                <div className={`p-2.5 rounded-xl ${selectedEmployee?.allow_personal_exclusion ? 'bg-orange-100 dark:bg-orange-900/30 text-orange-600 dark:text-orange-400' : 'bg-gray-100 dark:bg-gray-800 text-gray-400'} group-hover:scale-110 transition-transform`}>
                                    <UserX size={20} />
                                </div>
                                <div>
                                    <div className="text-sm font-black text-gray-900 dark:text-gray-100">Personal Exclusion</div>
                                    <div className="text-[10px] text-gray-500 dark:text-gray-400 font-medium">Allow manual contact exclusion</div>
                                </div>
                            </div>
                            <button
                                onClick={async () => {
                                    const val = !selectedEmployee?.allow_personal_exclusion;
                                    setEmployees(prev => prev.map(e => e.id === selectedEmployee?.id ? { ...e, allow_personal_exclusion: val } : e));
                                    setSelectedEmployee(selectedEmployee ? { ...selectedEmployee, allow_personal_exclusion: val } : null);
                                    await api.put(`/employees.php?id=${selectedEmployee?.id}`, { allow_personal_exclusion: val ? 1 : 0 });
                                    toast.success('Exclusion permission updated');
                                }}
                            >
                                {selectedEmployee?.allow_personal_exclusion ?
                                    <ToggleRight size={32} className="text-orange-600" /> :
                                    <ToggleLeft size={32} className="text-gray-400" />
                                }
                            </button>
                        </label>

                        <label className="flex items-center justify-between cursor-pointer p-4 rounded-2xl border border-gray-100 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800 transition-all group">
                            <div className="flex items-center gap-3">
                                <div className={`p-2.5 rounded-xl ${selectedEmployee?.allow_changing_tracking_start_date ? 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400' : 'bg-gray-100 dark:bg-gray-800 text-gray-400'} group-hover:scale-110 transition-transform`}>
                                    <CalendarClock size={20} />
                                </div>
                                <div>
                                    <div className="text-sm font-black text-gray-900 dark:text-gray-100">Change Start Date</div>
                                    <div className="text-[10px] text-gray-500 dark:text-gray-400 font-medium">Manual tracking start control</div>
                                </div>
                            </div>
                            <button
                                onClick={async () => {
                                    const val = !selectedEmployee?.allow_changing_tracking_start_date;
                                    setEmployees(prev => prev.map(e => e.id === selectedEmployee?.id ? { ...e, allow_changing_tracking_start_date: val } : e));
                                    setSelectedEmployee(selectedEmployee ? { ...selectedEmployee, allow_changing_tracking_start_date: val } : null);
                                    await api.put(`/employees.php?id=${selectedEmployee?.id}`, { allow_changing_tracking_start_date: val ? 1 : 0 });
                                    toast.success('Start date permission updated');
                                }}
                            >
                                {selectedEmployee?.allow_changing_tracking_start_date ?
                                    <ToggleRight size={32} className="text-green-600" /> :
                                    <ToggleLeft size={32} className="text-gray-400" />
                                }
                            </button>
                        </label>

                        <label className="flex items-center justify-between cursor-pointer p-4 rounded-2xl border border-gray-100 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800 transition-all group">
                            <div className="flex items-center gap-3">
                                <div className={`p-2.5 rounded-xl ${selectedEmployee?.allow_updating_tracking_sims ? 'bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400' : 'bg-gray-100 dark:bg-gray-800 text-gray-400'} group-hover:scale-110 transition-transform`}>
                                    <SmartphoneNfc size={20} />
                                </div>
                                <div>
                                    <div className="text-sm font-black text-gray-900 dark:text-gray-100">Update Tracked SIMs</div>
                                    <div className="text-[10px] text-gray-500 dark:text-gray-400 font-medium">SIM management from device</div>
                                </div>
                            </div>
                            <button
                                onClick={async () => {
                                    const val = !selectedEmployee?.allow_updating_tracking_sims;
                                    setEmployees(prev => prev.map(e => e.id === selectedEmployee?.id ? { ...e, allow_updating_tracking_sims: val } : e));
                                    setSelectedEmployee(selectedEmployee ? { ...selectedEmployee, allow_updating_tracking_sims: val } : null);
                                    await api.put(`/employees.php?id=${selectedEmployee?.id}`, { allow_updating_tracking_sims: val ? 1 : 0 });
                                    toast.success('SIM update permission updated');
                                }}
                            >
                                {selectedEmployee?.allow_updating_tracking_sims ?
                                    <ToggleRight size={32} className="text-indigo-600" /> :
                                    <ToggleLeft size={32} className="text-gray-400" />
                                }
                            </button>
                        </label>
                    </div>

                    <button
                        onClick={() => setIsControlsModalOpen(false)}
                        className="w-full btn btn-primary mt-4"
                    >
                        Save & Close
                    </button>
                </div>
            </Modal>

            {/* Tracking Configuration Modal */}
            <Modal
                isOpen={isTrackingModalOpen}
                onClose={() => setIsTrackingModalOpen(false)}
                title="Tracking Configuration"
            >
                <div className="space-y-6">
                    <p className="text-xs text-gray-500 dark:text-gray-400 font-medium bg-blue-50 dark:bg-blue-900/20 p-3 rounded-xl border border-blue-100 dark:border-blue-900/30 text-blue-700 dark:text-blue-300">
                        Select which activities to track for <strong>{selectedEmployee?.name || 'Employee'}</strong>.
                    </p>

                    <div className="space-y-3">
                        <label className="flex items-center justify-between cursor-pointer p-4 rounded-2xl border border-gray-100 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800 transition-all group">
                            <div className="flex items-center gap-3">
                                <div className={`p-2.5 rounded-xl ${selectedEmployee?.track_calls ? 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400' : 'bg-gray-100 dark:bg-gray-800 text-gray-400'} group-hover:scale-110 transition-transform`}>
                                    <PhoneCall size={20} />
                                </div>
                                <div>
                                    <div className="text-sm font-black text-gray-900 dark:text-gray-100">Track Calls</div>
                                    <div className="text-[10px] text-gray-500 dark:text-gray-400 font-medium">Sync call logs from this device</div>
                                </div>
                            </div>
                            <button
                                onClick={async (e) => {
                                    e.stopPropagation();
                                    const val = !selectedEmployee?.track_calls;
                                    setEmployees(prev => prev.map(e => e.id === selectedEmployee?.id ? { ...e, track_calls: val } : e));
                                    setSelectedEmployee(selectedEmployee ? { ...selectedEmployee, track_calls: val } : null);
                                    await api.put(`/employees.php?id=${selectedEmployee?.id}`, { track_calls: val ? 1 : 0 });
                                    toast.success('Call tracking updated');
                                }}
                            >
                                {selectedEmployee?.track_calls ?
                                    <ToggleRight size={32} className="text-blue-600" /> :
                                    <ToggleLeft size={32} className="text-gray-400" />
                                }
                            </button>
                        </label>

                        <label className="flex items-center justify-between cursor-pointer p-4 rounded-2xl border border-gray-100 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800 transition-all group">
                            <div className="flex items-center gap-3">
                                <div className={`p-2.5 rounded-xl ${selectedEmployee?.track_recordings ? 'bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400' : 'bg-gray-100 dark:bg-gray-800 text-gray-400'} group-hover:scale-110 transition-transform`}>
                                    <Mic size={20} />
                                </div>
                                <div>
                                    <div className="text-sm font-black text-gray-900 dark:text-gray-100">Track Recordings</div>
                                    <div className="text-[10px] text-gray-500 dark:text-gray-400 font-medium">Upload audio logs (if available)</div>
                                </div>
                            </div>
                            <button
                                onClick={async (e) => {
                                    e.stopPropagation();
                                    const val = !selectedEmployee?.track_recordings;

                                    // Check for storage if enabling
                                    if (val && (!user?.allowed_storage_gb || user.allowed_storage_gb <= 0)) {
                                        toast.error('No storage space available. Please upgrade your storage plan to enable recordings.');
                                        return;
                                    }

                                    setEmployees(prev => prev.map(e => e.id === selectedEmployee?.id ? { ...e, track_recordings: val } : e));
                                    setSelectedEmployee(selectedEmployee ? { ...selectedEmployee, track_recordings: val } : null);
                                    try {
                                        await api.put(`/employees.php?id=${selectedEmployee?.id}`, { track_recordings: val ? 1 : 0 });
                                        toast.success('Recording tracking updated');
                                    } catch (err) {
                                        // Rollback on error
                                        setEmployees(prev => prev.map(e => e.id === selectedEmployee.id ? { ...e, track_recordings: !val } : e));
                                        setSelectedEmployee({ ...selectedEmployee, track_recordings: !val });
                                        toast.error(err.response?.data?.message || 'Failed to update tracking');
                                    }
                                }}
                            >
                                {selectedEmployee?.track_recordings ?
                                    <ToggleRight size={32} className="text-purple-600" /> :
                                    <ToggleLeft size={32} className="text-gray-400" />
                                }
                            </button>
                        </label>
                    </div>

                    <button
                        onClick={() => setIsTrackingModalOpen(false)}
                        className="w-full btn btn-primary mt-4"
                    >
                        Apply Tracking
                    </button>
                </div>
            </Modal>

            {/* Employee Deletion Modal */}
            <Modal
                isOpen={isDeletionModalOpen}
                onClose={() => setIsDeletionModalOpen(false)}
                title="Manage Employee Data"
            >
                <div className="space-y-5">
                    {/* Employee Header */}
                    <div className="flex items-center gap-4 p-4 bg-red-50 dark:bg-red-900/20 rounded-2xl border border-red-100 dark:border-red-900/30">
                        <div className="w-12 h-12 bg-red-100 dark:bg-red-900/50 rounded-xl flex items-center justify-center text-red-600 dark:text-red-400">
                            <User size={24} />
                        </div>
                        <div>
                            <h4 className="font-black text-gray-900 dark:text-white">{deletionEmployee?.name}</h4>
                            <p className="text-xs text-red-600 dark:text-red-400 font-bold uppercase tracking-wider">
                                {deletionEmployee?.status === 'inactive' ? 'Archived' : 'Active Employee'}
                            </p>
                        </div>
                    </div>

                    {/* Loading State */}
                    {loadingStats && (
                        <div className="flex items-center justify-center py-8">
                            <Loader2 size={24} className="animate-spin text-gray-400" />
                            <span className="ml-2 text-sm text-gray-500">Loading data stats...</span>
                        </div>
                    )}

                    {/* Data Stats & Actions */}
                    {!loadingStats && deletionStats && (
                        <div className="space-y-3">
                            {/* Delete Calls & Data */}
                            <div className={`p-4 rounded-2xl border transition-all ${callsDeleted || (deletionStats.calls_count === 0 && deletionStats.contacts_count === 0)
                                ? 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-900/30'
                                : 'bg-gray-50 dark:bg-gray-800 border-gray-100 dark:border-gray-700'
                                }`}>
                                <div className="flex items-start justify-between gap-4">
                                    <div className="flex items-start gap-3">
                                        <div className={`p-2.5 rounded-xl ${callsDeleted || (deletionStats.calls_count === 0 && deletionStats.contacts_count === 0)
                                            ? 'bg-green-100 dark:bg-green-900/50 text-green-600 dark:text-green-400'
                                            : 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'
                                            }`}>
                                            {callsDeleted || (deletionStats.calls_count === 0 && deletionStats.contacts_count === 0)
                                                ? <CheckCircle2 size={20} />
                                                : <Database size={20} />
                                            }
                                        </div>
                                        <div>
                                            <div className="text-sm font-black text-gray-900 dark:text-gray-100">
                                                Calls & Contacts Data
                                            </div>
                                            <div className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                                                {deletionStats.calls_count === 0 && deletionStats.contacts_count === 0
                                                    ? 'No data to delete'
                                                    : `${deletionStats.calls_count - deletionStats.recordings_count} calls, ${deletionStats.contacts_count} contacts`
                                                }
                                            </div>
                                        </div>
                                    </div>
                                    {!(callsDeleted || (deletionStats.calls_count === 0 && deletionStats.contacts_count === 0)) && (
                                        <button
                                            onClick={handleDeleteCalls}
                                            disabled={deletingCalls}
                                            className="px-3 py-1.5 bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 rounded-lg text-xs font-bold hover:bg-red-200 dark:hover:bg-red-900/50 transition-all disabled:opacity-50 flex items-center gap-1.5"
                                        >
                                            {deletingCalls ? <Loader2 size={14} className="animate-spin" /> : <Trash2 size={14} />}
                                            Delete
                                        </button>
                                    )}
                                </div>
                            </div>

                            {/* Delete Recordings */}
                            <div className={`p-4 rounded-2xl border transition-all ${recordingsDeleted || deletionStats.recordings_count === 0
                                ? 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-900/30'
                                : 'bg-gray-50 dark:bg-gray-800 border-gray-100 dark:border-gray-700'
                                }`}>
                                <div className="flex items-start justify-between gap-4">
                                    <div className="flex items-start gap-3">
                                        <div className={`p-2.5 rounded-xl ${recordingsDeleted || deletionStats.recordings_count === 0
                                            ? 'bg-green-100 dark:bg-green-900/50 text-green-600 dark:text-green-400'
                                            : 'bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400'
                                            }`}>
                                            {recordingsDeleted || deletionStats.recordings_count === 0
                                                ? <CheckCircle2 size={20} />
                                                : <HardDrive size={20} />
                                            }
                                        </div>
                                        <div>
                                            <div className="text-sm font-black text-gray-900 dark:text-gray-100">
                                                Recordings & Storage
                                            </div>
                                            <div className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                                                {deletionStats.recordings_count === 0
                                                    ? 'No recordings to delete'
                                                    : `${deletionStats.recordings_count} files • ${formatBytes(deletionStats.recordings_size_bytes)}`
                                                }
                                            </div>
                                        </div>
                                    </div>
                                    {!(recordingsDeleted || deletionStats.recordings_count === 0) && (
                                        <button
                                            onClick={handleDeleteRecordings}
                                            disabled={deletingRecordings}
                                            className="px-3 py-1.5 bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 rounded-lg text-xs font-bold hover:bg-red-200 dark:hover:bg-red-900/50 transition-all disabled:opacity-50 flex items-center gap-1.5"
                                        >
                                            {deletingRecordings ? <Loader2 size={14} className="animate-spin" /> : <Trash2 size={14} />}
                                            Delete
                                        </button>
                                    )}
                                </div>
                            </div>

                            {/* Divider */}
                            <div className="relative py-2">
                                <div className="absolute inset-0 flex items-center">
                                    <div className="w-full border-t border-gray-200 dark:border-gray-700"></div>
                                </div>
                                <div className="relative flex justify-center">
                                    <span className="px-3 bg-white dark:bg-gray-900 text-[10px] font-black uppercase tracking-widest text-gray-400">Actions</span>
                                </div>
                            </div>

                            {/* Delete Employee Button - LOCKED until data is cleared */}
                            {(() => {
                                const hasData = (deletionStats.calls_count > 0 || deletionStats.recordings_count > 0) && !(callsDeleted && recordingsDeleted);
                                const canDelete = !hasData || (callsDeleted && (recordingsDeleted || deletionStats.recordings_count === 0));

                                return (
                                    <button
                                        onClick={handleDeleteEmployee}
                                        disabled={!canDelete}
                                        className={`w-full p-4 rounded-2xl border transition-all flex items-center justify-between ${canDelete
                                            ? 'bg-red-600 hover:bg-red-700 border-red-600 text-white'
                                            : 'bg-gray-100 dark:bg-gray-800 border-gray-200 dark:border-gray-700 text-gray-400 cursor-not-allowed'
                                            }`}
                                    >
                                        <div className="flex items-center gap-3">
                                            <Trash2 size={20} />
                                            <div className="text-left">
                                                <div className="font-bold">Delete Employee Permanently</div>
                                                <div className={`text-xs ${canDelete ? 'text-red-200' : 'text-gray-500'}`}>
                                                    {canDelete ? 'This action cannot be undone' : 'Delete all data above first'}
                                                </div>
                                            </div>
                                        </div>
                                        {!canDelete && (
                                            <div className="px-2 py-1 bg-orange-100 dark:bg-orange-900/30 text-orange-600 dark:text-orange-400 rounded text-[10px] font-black uppercase">
                                                Locked
                                            </div>
                                        )}
                                    </button>
                                );
                            })()}

                            {/* Archive Option */}
                            <button
                                onClick={handleArchiveEmployee}
                                disabled={deletionEmployee?.status === 'inactive'}
                                className={`w-full p-4 rounded-2xl border transition-all flex items-center gap-3 ${deletionEmployee?.status === 'inactive'
                                    ? 'bg-gray-100 dark:bg-gray-800 border-gray-200 dark:border-gray-700 text-gray-400 cursor-not-allowed'
                                    : 'bg-amber-50 dark:bg-amber-900/20 border-amber-200 dark:border-amber-900/30 text-amber-700 dark:text-amber-300 hover:bg-amber-100 dark:hover:bg-amber-900/30'
                                    }`}
                            >
                                <Archive size={20} />
                                <div className="text-left flex-1">
                                    <div className="font-bold">
                                        {deletionEmployee?.status === 'inactive' ? 'Already Archived' : 'Disable & Archive'}
                                    </div>
                                    <div className="text-xs opacity-75">
                                        Not counted towards active employees • Data preserved
                                    </div>
                                </div>
                            </button>
                        </div>
                    )}

                    {/* Cancel Button */}
                    <button
                        onClick={() => setIsDeletionModalOpen(false)}
                        className="w-full btn bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700"
                    >
                        Cancel
                    </button>
                </div>
            </Modal>
        </div>
    );
}
