import { useState, useEffect } from 'react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import {
    HardDrive,
    RefreshCw,
    Trash2,
    Clock,
    AlertCircle,
    CheckCircle,
    Loader2,
    ChevronRight,
    PieChart,
    Database,
    Download,
    Calendar,
    Users,
    ArrowLeft,
    AlertTriangle
} from 'lucide-react';
import Modal from '../components/Modal';
import { toast } from 'sonner';

export default function StoragePage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [usage, setUsage] = useState(null);
    const [breakdown, setBreakdown] = useState([]);
    const [employees, setEmployees] = useState([]);

    // UI State
    const [viewType, setViewType] = useState('time'); // 'time' or 'employee'
    const [drillDownMonth, setDrillDownMonth] = useState(null); // YYYY-MM
    const [drillDownEmployee, setDrillDownEmployee] = useState(null); // empId

    const [loading, setLoading] = useState(true);
    const [breakdownLoading, setBreakdownLoading] = useState(false);
    const [refreshing, setRefreshing] = useState(false);
    const [cleanupLoading, setCleanupLoading] = useState(null);
    const [downloadingKey, setDownloadingKey] = useState(null); // Tracks which item is downloading

    const [confirmModal, setConfirmModal] = useState({
        isOpen: false,
        title: '',
        message: '',
        onConfirm: null,
        isDestructive: false
    });

    useEffect(() => {
        fetchUsage();
        fetchEmployees();
    }, []);

    useEffect(() => {
        fetchBreakdown();
    }, [viewType, drillDownMonth, drillDownEmployee]);

    const fetchUsage = async () => {
        try {
            const res = await api.get('/billing.php?action=get_usage');
            setUsage(res.data);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const fetchEmployees = async () => {
        try {
            const res = await api.get('/employees.php?action=get_all');
            if (res.status) setEmployees(res.data);
        } catch (err) {
            console.error(err);
        }
    };

    const fetchBreakdown = async () => {
        setBreakdownLoading(true);
        try {
            let type = 'month';
            let employee_id = null;

            if (viewType === 'employee') {
                if (drillDownEmployee) {
                    type = 'month';
                    employee_id = drillDownEmployee;
                } else {
                    type = 'employee';
                }
            } else if (drillDownMonth) {
                type = 'date'; // If a month is selected, we show dates
            }

            const params = new URLSearchParams({
                action: 'get_usage_breakdown',
                type: type
            });
            if (employee_id) params.append('employee_id', employee_id);

            const res = await api.get(`/billing.php?${params.toString()}`);
            if (res.status) {
                let data = res.data;
                if (drillDownMonth && type === 'date') {
                    data = data.filter(item => item.label.startsWith(drillDownMonth));
                }
                setBreakdown(data);
            }
        } catch (err) {
            console.error(err);
        } finally {
            setBreakdownLoading(false);
        }
    };

    const handleRefresh = async () => {
        setRefreshing(true);
        try {
            const res = await api.post('/billing.php?action=refresh_storage');
            setUsage(prev => ({
                ...prev,
                storage_used_bytes: res.data.storage_used_bytes,
                last_storage_check: res.data.last_storage_check
            }));
            fetchBreakdown();
            toast.success('Storage usage updated successfully');
        } catch (err) {
            toast.error('Failed to recalculate storage');
        } finally {
            setRefreshing(false);
        }
    };

    const handleFilteredCleanup = async (type, value) => {
        const displayLabel = type === 'employee'
            ? employees.find(e => String(e.id) === String(value))?.name || value
            : formatDateLabel(value);

        setConfirmModal({
            isOpen: true,
            title: 'Delete Recordings',
            message: `Permanently delete recordings for ${displayLabel}? This cannot be undone.`,
            isDestructive: true,
            onConfirm: async () => {
                setCleanupLoading(`${type}-${value}`);
                try {
                    const formData = new FormData();
                    formData.append('action', 'cleanup_filtered');

                    if (type === 'employee') {
                        formData.append('employee_id', value);
                    } else if (type === 'month') {
                        formData.append('month', value);
                    } else if (type === 'date') {
                        formData.append('date', value);
                    }

                    const res = await api.post('/billing.php', formData);
                    if (res.status) {
                        toast.success(res.message || 'Cleanup successful');
                        handleRefresh();
                    }
                } catch (err) {
                    toast.error('Cleanup failed');
                } finally {
                    setCleanupLoading(null);
                }
            }
        });
    };

    const handleDownload = async (type, value) => {
        const downloadKey = `${type}-${value}`;

        // Prevent multiple simultaneous downloads of the same item
        if (downloadingKey) {
            toast.error('A download is already in progress');
            return;
        }

        setDownloadingKey(downloadKey);
        toast.info('Preparing download...', { id: 'download-toast' });

        const params = new URLSearchParams({ action: 'download_storage' });
        if (type === 'employee') params.append('employee_id', value);
        else params.append(type, value);

        const url = `https://calltrack.mylistings.in/api/billing.php?${params.toString()}`;
        const token = localStorage.getItem('cc_token');

        try {
            const response = await fetch(url, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) {
                const text = await response.text();
                throw new Error(text || 'Download failed');
            }

            const blob = await response.blob();
            const downloadUrl = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = downloadUrl;
            a.download = `recordings_${value}.zip`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(downloadUrl);

            toast.success('Download complete!', { id: 'download-toast' });
        } catch (err) {
            toast.error('Download failed: ' + err.message, { id: 'download-toast' });
        } finally {
            setDownloadingKey(null);
        }
    };

    const formatBytes = (bytes, decimals = 2) => {
        if (!+bytes) return '0 Bytes';
        const k = 1024, dm = decimals, sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    };

    const formatDateLabel = (label) => {
        if (viewType === 'employee' && !drillDownEmployee) {
            return employees.find(e => String(e.id) === String(label))?.name || `Employee #${label}`;
        }
        // If it's YYYY-MM
        if (label.length === 7) {
            return new Date(label + '-01').toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
        }
        // If it's YYYY-MM-DD
        return new Date(label).toLocaleDateString(undefined, { day: 'numeric', month: 'long', year: 'numeric' });
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <Loader2 className="animate-spin text-blue-600" size={40} />
            </div>
        );
    }

    const usagePercent = usage ? Math.min(100, (usage.storage_used_bytes / (usage.allowed_storage_gb * 1024 * 1024 * 1024)) * 100) : 0;
    const isCritical = usagePercent > 90;

    return (
        <div className="max-w-7xl mx-auto space-y-6 animate-in fade-in duration-500 pb-12">
            <header className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-black tracking-tight text-gray-900">Storage Management</h1>
                    <p className="text-gray-500 mt-1 font-medium text-sm">Monitor and optimize your cloud recording storage.</p>
                </div>
                <div className="flex items-center gap-3">
                    <button
                        onClick={handleRefresh}
                        disabled={refreshing}
                        className="btn bg-white border border-gray-200 text-gray-700 h-10 px-4 rounded-xl font-bold gap-2 hover:border-blue-500 hover:text-blue-600 transition-all shadow-sm active:scale-95"
                    >
                        <RefreshCw size={16} className={refreshing ? 'animate-spin' : ''} />
                        Recalculate
                    </button>
                </div>
            </header>

            <div className="space-y-6">
                <div className="space-y-6">
                    {/* Main Stats Card */}
                    <div className="bg-white rounded-[2rem] border border-gray-100 shadow-xl overflow-hidden p-8">
                        <div className="flex flex-col md:flex-row items-center gap-8">
                            <div className="relative w-36 h-36 flex-shrink-0">
                                <svg className="w-full h-full transform -rotate-90">
                                    <circle cx="72" cy="72" r="64" fill="transparent" stroke="#f3f4f6" strokeWidth="12" />
                                    <circle
                                        cx="72" cy="72" r="64" fill="transparent"
                                        stroke={isCritical ? "#ef4444" : "#2563eb"}
                                        strokeWidth="12"
                                        strokeDasharray={2 * Math.PI * 64}
                                        strokeDashoffset={(2 * Math.PI * 64) * (1 - usagePercent / 100)}
                                        strokeLinecap="round"
                                        className="transition-all duration-1000 ease-out"
                                    />
                                </svg>
                                <div className="absolute inset-0 flex flex-col items-center justify-center">
                                    <span className="text-2xl font-black text-gray-900">{Math.round(usagePercent)}%</span>
                                    <span className="text-[8px] uppercase font-black text-gray-400 tracking-widest">Used</span>
                                </div>
                            </div>

                            <div className="flex-1 space-y-4 text-center md:text-left">
                                <div className="space-y-0.5">
                                    <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">Storage Status</p>
                                    <h2 className="text-3xl font-black text-gray-900">{usage ? formatBytes(usage.storage_used_bytes) : '0 Bytes'}</h2>
                                    <p className="text-sm text-gray-500 font-bold uppercase tracking-tight">of {usage?.allowed_storage_gb || 0} GB Total Capacity</p>
                                </div>

                                <div className="flex flex-wrap gap-2 justify-center md:justify-start">
                                    <div className="px-3 py-1.5 bg-gray-50 rounded-lg border border-gray-100 flex items-center gap-2">
                                        <Clock size={12} className="text-gray-400" />
                                        <span className="text-[10px] font-black text-gray-500 uppercase">Last Sync: {usage?.last_storage_check ? new Date(usage.last_storage_check).toLocaleTimeString() : 'Never'}</span>
                                    </div>
                                    {isCritical && (
                                        <div className="px-3 py-1.5 bg-red-50 text-red-600 rounded-lg border border-red-100 flex items-center gap-2 animate-pulse">
                                            <AlertCircle size={12} />
                                            <span className="text-[10px] font-black uppercase">Low Space</span>
                                        </div>
                                    )}
                                </div>
                            </div>

                            <button
                                onClick={() => {
                                    const current = parseInt(localStorage.getItem('cc_cart_storage') || '0');
                                    localStorage.setItem('cc_cart_storage', (current + 2).toString());
                                    navigate('/plans');
                                }}
                                className="px-8 py-4 bg-blue-600 text-white rounded-[1.25rem] font-black text-xs uppercase tracking-wider hover:bg-blue-700 hover:shadow-blue-500/40 hover:-translate-y-0.5 transition-all shadow-xl shadow-blue-500/20 active:scale-95 flex items-center gap-3 whitespace-nowrap"
                            >
                                <Database size={18} />
                                Get Extra Storage
                            </button>
                        </div>
                    </div>

                    {/* Breakdown Filter Bar */}
                    <div className="bg-white rounded-[1.5rem] border border-gray-100 shadow-sm p-4 flex flex-col md:flex-row items-center justify-between gap-4">
                        <div className="flex items-center gap-4">
                            <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest whitespace-nowrap">Breakdown By:</span>
                            <div className="flex items-center gap-2 p-1 bg-gray-50 rounded-xl w-full md:w-auto overflow-x-auto no-scrollbar">
                                <button
                                    onClick={() => { setViewType('time'); setDrillDownMonth(null); setDrillDownEmployee(null); }}
                                    className={`px-4 py-2 rounded-lg text-[10px] font-black uppercase tracking-wider transition-all whitespace-nowrap flex items-center gap-2 ${viewType === 'time'
                                        ? "bg-white text-blue-600 shadow-sm border border-gray-100"
                                        : "text-gray-400 hover:text-gray-600"
                                        }`}
                                >
                                    <Calendar size={14} />
                                    Time Based
                                </button>
                                <button
                                    onClick={() => { setViewType('employee'); setDrillDownMonth(null); setDrillDownEmployee(null); }}
                                    className={`px-4 py-2 rounded-lg text-[10px] font-black uppercase tracking-wider transition-all whitespace-nowrap flex items-center gap-2 ${viewType === 'employee'
                                        ? "bg-white text-blue-600 shadow-sm border border-gray-100"
                                        : "text-gray-400 hover:text-gray-600"
                                        }`}
                                >
                                    <Users size={14} />
                                    Employees
                                </button>
                            </div>
                        </div>

                        <div className="flex items-center gap-4">
                            {(drillDownMonth || drillDownEmployee) && (
                                <button
                                    onClick={() => { setDrillDownMonth(null); setDrillDownEmployee(null); }}
                                    className="flex items-center gap-2 text-[10px] font-black uppercase text-gray-500 hover:text-blue-600 transition-colors"
                                >
                                    <ArrowLeft size={14} />
                                    Back to {drillDownMonth ? 'Months' : 'Employees'}
                                </button>
                            )}

                            <div className="flex items-center gap-2">
                                <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest bg-gray-50 px-3 py-1.5 rounded-lg border border-gray-100">
                                    Total Items: {breakdown.length}
                                </span>
                            </div>
                        </div>
                    </div>

                    {/* Breakdown List */}
                    <div className="bg-white rounded-[2rem] border border-gray-100 shadow-sm overflow-hidden min-h-[400px]">
                        <div className="p-6 border-b border-gray-50 bg-gray-50/20 flex items-center justify-between">
                            <h3 className="font-black text-gray-900 uppercase tracking-widest text-xs flex items-center gap-2">
                                <PieChart size={16} className="text-blue-600" />
                                {drillDownMonth ? `Daily for ${formatDateLabel(drillDownMonth)}` :
                                    drillDownEmployee ? `Monthly for ${employees.find(e => String(e.id) === String(drillDownEmployee))?.name || 'Employee'}` :
                                        (viewType === 'time' ? 'Monthly Breakdown' : 'Employee Breakdown')}
                            </h3>
                        </div>

                        <div className="divide-y divide-gray-50">
                            {breakdownLoading ? (
                                <div className="flex items-center justify-center py-20">
                                    <Loader2 className="animate-spin text-blue-600" size={32} />
                                </div>
                            ) : breakdown.length === 0 ? (
                                <div className="flex flex-col items-center justify-center py-20 text-center">
                                    <div className="w-16 h-16 bg-gray-50 rounded-2xl flex items-center justify-center text-gray-300 mb-4">
                                        <HardDrive size={32} />
                                    </div>
                                    <p className="text-xs font-black text-gray-400 uppercase tracking-widest whitespace-nowrap">No recordings found</p>
                                </div>
                            ) : breakdown.map((item) => (
                                <div
                                    key={item.label}
                                    className={`p-5 flex items-center justify-between hover:bg-gray-50/50 transition-colors group ${((!drillDownMonth && viewType === 'time') || (!drillDownEmployee && viewType === 'employee')) ? 'cursor-pointer' : ''}`}
                                    onClick={() => {
                                        if (!drillDownMonth && viewType === 'time') {
                                            setDrillDownMonth(item.label);
                                        } else if (!drillDownEmployee && viewType === 'employee') {
                                            setDrillDownEmployee(item.label);
                                        }
                                    }}
                                >
                                    <div className="flex items-center gap-4">
                                        <div className="w-10 h-10 bg-gray-50 rounded-xl flex items-center justify-center text-gray-400 group-hover:bg-white group-hover:text-blue-500 transition-all shadow-sm border border-gray-100">
                                            {(viewType === 'employee' && !drillDownEmployee) ? <Users size={18} /> : (item.label.length === 7 ? <Calendar size={18} /> : <Clock size={18} />)}
                                        </div>
                                        <div>
                                            <p className="font-black text-gray-900 text-sm">
                                                {formatDateLabel(item.label)}
                                                {((!drillDownMonth && viewType === 'time') || (!drillDownEmployee && viewType === 'employee')) && (
                                                    <span className="ml-2 text-[10px] text-blue-500 opacity-0 group-hover:opacity-100 transition-opacity">Drill Down ↘</span>
                                                )}
                                            </p>
                                            <p className="text-[10px] text-gray-500 font-bold uppercase tracking-tighter">
                                                {formatBytes(item.size)} <span className="mx-1.5">•</span> {item.count} Files
                                            </p>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
                                        <button
                                            onClick={() => handleDownload(drillDownEmployee ? 'month' : (viewType === 'time' ? (drillDownMonth ? 'date' : 'month') : 'employee'), item.label)}
                                            disabled={downloadingKey !== null}
                                            className={`h-10 px-4 border rounded-xl text-[10px] font-black uppercase transition-all shadow-sm flex items-center gap-2 active:scale-95 ${downloadingKey === `${drillDownEmployee ? 'month' : (viewType === 'time' ? (drillDownMonth ? 'date' : 'month') : 'employee')}-${item.label}`
                                                ? 'bg-blue-50 border-blue-300 text-blue-600'
                                                : downloadingKey
                                                    ? 'bg-gray-100 border-gray-200 text-gray-400 cursor-not-allowed'
                                                    : 'bg-white border-gray-200 text-gray-600 hover:border-blue-500 hover:text-blue-600'
                                                }`}
                                        >
                                            {downloadingKey === `${drillDownEmployee ? 'month' : (viewType === 'time' ? (drillDownMonth ? 'date' : 'month') : 'employee')}-${item.label}`
                                                ? <><Loader2 size={14} className="animate-spin" /> Preparing...</>
                                                : <><Download size={14} /> Download</>}
                                        </button>
                                        <button
                                            onClick={() => handleFilteredCleanup(drillDownEmployee ? 'month' : (viewType === 'time' ? (drillDownMonth ? 'date' : 'month') : 'employee'), item.label)}
                                            disabled={cleanupLoading === `${drillDownEmployee ? 'month' : (viewType === 'time' ? (drillDownMonth ? 'date' : 'month') : 'employee')}-${item.label}`}
                                            className="h-10 w-10 bg-white border border-gray-200 text-gray-400 rounded-xl hover:border-red-500 hover:text-red-500 transition-all shadow-sm flex items-center justify-center active:scale-95"
                                        >
                                            {cleanupLoading === `${viewType === 'time' ? (drillDownMonth ? 'date' : 'month') : 'employee'}-${item.label}`
                                                ? <Loader2 size={14} className="animate-spin" />
                                                : <Trash2 size={14} />}
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

            </div>

            {/* Confirmation Modal */}
            <Modal
                isOpen={confirmModal.isOpen}
                onClose={() => setConfirmModal({ ...confirmModal, isOpen: false })}
                title={confirmModal.title}
            >
                <div className="space-y-4">
                    <div className="flex items-start gap-3 p-3 bg-red-50 rounded-lg text-red-800 border border-red-100">
                        <AlertTriangle className="shrink-0 mt-0.5" size={18} />
                        <p className="text-sm font-medium">{confirmModal.message}</p>
                    </div>
                    <div className="flex gap-3 pt-2">
                        <button
                            type="button"
                            onClick={() => setConfirmModal({ ...confirmModal, isOpen: false })}
                            className="flex-1 px-4 py-2 border border-gray-200 text-gray-600 rounded-lg hover:bg-gray-50 font-medium"
                        >
                            Cancel
                        </button>
                        <button
                            type="button"
                            onClick={async () => {
                                const onConfirm = confirmModal.onConfirm;
                                setConfirmModal({ ...confirmModal, isOpen: false });
                                await onConfirm();
                            }}
                            className={`flex-1 btn ${confirmModal.isDestructive ? 'bg-red-600 hover:bg-red-700 text-white' : 'btn-primary'}`}
                        >
                            Confirm Delete
                        </button>
                    </div>
                </div>
            </Modal>
        </div>
    );
}
