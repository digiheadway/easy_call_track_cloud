import { useState, useEffect } from 'react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import {
    Save,
    ShieldCheck,
    Lock,
    Building,
    AlertCircle,
    Download,
    FileSpreadsheet,
    Users,
    Phone,
    Loader2,
    CheckCircle
} from 'lucide-react';

export default function Settings() {
    const { user } = useAuth();
    const [activeTab, setActiveTab] = useState('profile');
    const [orgName, setOrgName] = useState(user?.org_name || '');
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState('');
    const [error, setError] = useState('');

    // Password Change State
    const [passwords, setPasswords] = useState({
        current: '',
        new: '',
        confirm: ''
    });

    // Export State
    const [exportLoading, setExportLoading] = useState({ calls: false, callers: false });
    const [exportDateRange, setExportDateRange] = useState('all');

    useEffect(() => {
        if (user?.org_name) setOrgName(user.org_name);
    }, [user]);

    const handleExportCalls = async () => {
        setExportLoading(prev => ({ ...prev, calls: true }));
        setError('');
        try {
            const params = new URLSearchParams({
                action: 'export',
                type: 'calls',
                dateRange: exportDateRange
            });

            const response = await fetch(`https://calltrack.mylistings.in/api/export.php?${params.toString()}`, {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('cc_token')}`
                }
            });

            if (!response.ok) throw new Error('Export failed');

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `calls_export_${new Date().toISOString().split('T')[0]}.csv`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);

            setSuccess('Calls exported successfully!');
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            console.error('Export error:', err);
            setError('Failed to export calls. Please try again.');
        } finally {
            setExportLoading(prev => ({ ...prev, calls: false }));
        }
    };

    const handleExportCallers = async () => {
        setExportLoading(prev => ({ ...prev, callers: true }));
        setError('');
        try {
            const params = new URLSearchParams({
                action: 'export',
                type: 'callers'
            });

            const response = await fetch(`https://calltrack.mylistings.in/api/export.php?${params.toString()}`, {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('cc_token')}`
                }
            });

            if (!response.ok) throw new Error('Export failed');

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `callers_export_${new Date().toISOString().split('T')[0]}.csv`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);

            setSuccess('Callers exported successfully!');
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            console.error('Export error:', err);
            setError('Failed to export callers. Please try again.');
        } finally {
            setExportLoading(prev => ({ ...prev, callers: false }));
        }
    };

    const handleSaveProfile = async (e) => {
        e.preventDefault();
        setLoading(true);
        setSuccess('');
        setError('');
        try {
            await api.put('/settings.php', {
                action: 'update_profile',
                org_name: orgName
            });
            setSuccess('Organization profile updated successfully!');
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            console.error(err);
            setError(err.response?.data?.message || 'Failed to update profile');
        } finally {
            setLoading(false);
        }
    };

    const handleChangePassword = async (e) => {
        e.preventDefault();
        if (passwords.new !== passwords.confirm) {
            setError('New passwords do not match');
            return;
        }

        setLoading(true);
        setSuccess('');
        setError('');
        try {
            await api.put('/settings.php', {
                action: 'change_password',
                current_password: passwords.current,
                new_password: passwords.new
            });
            setSuccess('Password changed successfully!');
            setPasswords({ current: '', new: '', confirm: '' });
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            console.error(err);
            setError(err.response?.data?.message || 'Failed to change password');
        } finally {
            setLoading(false);
        }
    };

    const tabs = [
        { id: 'profile', label: 'Organization Profile', icon: Building, description: 'Manage your organization details' },
        { id: 'security', label: 'Security', icon: Lock, description: 'Update your password and security settings' },
        { id: 'exports', label: 'Data Export', icon: Download, description: 'Download your calls and callers data' },
    ];

    return (
        <div className="max-w-6xl mx-auto space-y-8">
            <div>
                <h1 className="text-2xl font-bold tracking-tight text-gray-900">Settings</h1>
                <p className="text-gray-500 text-sm mt-1">Manage your team and preferences here.</p>
            </div>

            <div className="flex flex-col lg:flex-row gap-8 items-start">

                {/* Sidebar Navigation */}
                <aside className="w-full lg:w-64 flex-shrink-0">
                    <nav className="flex lg:flex-col gap-1.5 bg-white p-2 rounded-xl border border-gray-200/50 shadow-sm lg:bg-transparent lg:p-0 lg:border-none lg:shadow-none overflow-x-auto">
                        {tabs.map((tab) => {
                            const Icon = tab.icon;
                            // Active tab styling
                            const isActive = activeTab === tab.id;

                            return (
                                <button
                                    key={tab.id}
                                    onClick={() => setActiveTab(tab.id)}
                                    className={`
                                        flex items-center gap-3 px-4 py-2.5 text-sm font-medium rounded-lg transition-all duration-200 whitespace-nowrap
                                        ${isActive
                                            ? 'bg-white text-blue-600 shadow-sm border border-gray-200 lg:bg-white lg:shadow-sm lg:border-gray-200'
                                            : 'text-gray-600 hover:bg-gray-100/50 hover:text-gray-900'
                                        }
                                        relative group
                                    `}
                                >
                                    <Icon size={18} className={`transition-colors duration-200 ${isActive ? 'text-blue-600' : 'text-gray-400 group-hover:text-gray-500'}`} />
                                    {tab.label}

                                    {isActive && (
                                        <div className="hidden lg:block absolute right-0 inset-y-0 w-1 bg-blue-600 rounded-l-full opacity-0 lg:opacity-0" />
                                    )}
                                </button>
                            );
                        })}
                    </nav>
                </aside>

                {/* Main Content */}
                <main className="flex-1 w-full min-w-0">
                    <div className="bg-white rounded-2xl border border-gray-200 shadow-sm">

                        {/* Profile Section */}
                        {activeTab === 'profile' && (
                            <div className="animate-in fade-in slide-in-from-right duration-300">
                                <div className="p-6 border-b border-gray-100">
                                    <h2 className="text-lg font-semibold text-gray-900">Organization Profile</h2>
                                    <p className="text-sm text-gray-500 mt-1">Update your organization's detailed information.</p>
                                </div>

                                <div className="p-6">
                                    <form onSubmit={handleSaveProfile} className="max-w-xl space-y-6">
                                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                            <div>
                                                <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-2">Organization ID</label>
                                                <div className="relative group">
                                                    <input
                                                        type="text"
                                                        value={user?.org_id || ''}
                                                        disabled
                                                        className="input bg-gray-50 text-gray-500 font-mono text-sm pl-4 pr-10 hover:bg-gray-100 transition-colors cursor-not-allowed border-gray-200"
                                                    />
                                                    <ShieldCheck size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400" />
                                                </div>
                                            </div>

                                            <div>
                                                <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-2">Admin Email</label>
                                                <input
                                                    type="email"
                                                    value={user?.email || ''}
                                                    disabled
                                                    className="input bg-gray-50 text-gray-500 text-sm pl-4 hover:bg-gray-100 transition-colors cursor-not-allowed border-gray-200"
                                                />
                                            </div>
                                        </div>

                                        <div>
                                            <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-2">Organization Name</label>
                                            <input
                                                type="text"
                                                className="input text-sm pl-4 focus:ring-2 focus:ring-blue-100 transition-all border-gray-200"
                                                value={orgName}
                                                onChange={(e) => setOrgName(e.target.value)}
                                                placeholder="Enter organization name"
                                            />
                                            <p className="text-[11px] text-gray-400 mt-2">Visible on all generated reports and invoices.</p>
                                        </div>

                                        <div className="pt-4 border-t border-gray-50 mt-8">
                                            <button
                                                type="submit"
                                                className="btn btn-primary h-10 px-6 gap-2 shadow-lg shadow-blue-500/20 active:scale-95 transition-transform"
                                                disabled={loading}
                                            >
                                                {loading ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />}
                                                Save Changes
                                            </button>
                                        </div>
                                    </form>
                                </div>
                            </div>
                        )}

                        {/* Security Section */}
                        {activeTab === 'security' && (
                            <div className="animate-in fade-in slide-in-from-right duration-300">
                                <div className="p-6 border-b border-gray-100">
                                    <h2 className="text-lg font-semibold text-gray-900">Security Settings</h2>
                                    <p className="text-sm text-gray-500 mt-1">Manage your password and account security.</p>
                                </div>

                                <div className="p-6">
                                    <form onSubmit={handleChangePassword} className="max-w-xl space-y-6">
                                        <div>
                                            <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-2">Current Password</label>
                                            <input
                                                type="password"
                                                className="input text-sm pl-4 border-gray-200"
                                                value={passwords.current}
                                                onChange={(e) => setPasswords({ ...passwords, current: e.target.value })}
                                                placeholder="Enter current password"
                                            />
                                        </div>

                                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                            <div>
                                                <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-2">New Password</label>
                                                <input
                                                    type="password"
                                                    className="input text-sm pl-4 border-gray-200"
                                                    value={passwords.new}
                                                    onChange={(e) => setPasswords({ ...passwords, new: e.target.value })}
                                                    placeholder="Minimum 6 characters"
                                                    minLength={6}
                                                />
                                            </div>

                                            <div>
                                                <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-2">Confirm Password</label>
                                                <input
                                                    type="password"
                                                    className="input text-sm pl-4 border-gray-200"
                                                    value={passwords.confirm}
                                                    onChange={(e) => setPasswords({ ...passwords, confirm: e.target.value })}
                                                    placeholder="Confirm new password"
                                                />
                                            </div>
                                        </div>

                                        <div className="pt-4 border-t border-gray-50 mt-8">
                                            <button
                                                type="submit"
                                                className="btn btn-primary h-10 px-6 gap-2 shadow-lg shadow-blue-500/20 active:scale-95 transition-transform"
                                                disabled={loading || !passwords.current || !passwords.new}
                                            >
                                                {loading ? <Loader2 size={16} className="animate-spin" /> : <ShieldCheck size={16} />}
                                                Update Password
                                            </button>
                                        </div>
                                    </form>
                                </div>
                            </div>
                        )}

                        {/* Export Section */}
                        {activeTab === 'exports' && (
                            <div className="animate-in fade-in slide-in-from-right duration-300">
                                <div className="p-6 border-b border-gray-100">
                                    <h2 className="text-lg font-semibold text-gray-900">Data Export</h2>
                                    <p className="text-sm text-gray-500 mt-1">Download your data in CSV format for external analysis.</p>
                                </div>

                                <div className="p-6">
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                        {/* Calls Export */}
                                        <div className="p-5 bg-white rounded-xl border border-gray-200 hover:border-blue-300 hover:shadow-md transition-all group relative overflow-hidden">
                                            <div className="absolute top-0 right-0 w-24 h-24 bg-blue-50/50 rounded-full -mr-12 -mt-12 group-hover:bg-blue-50 transition-colors" />

                                            <div className="flex items-start gap-4 mb-5 relative">
                                                <div className="p-3 bg-blue-50 text-blue-600 rounded-lg group-hover:scale-105 transition-transform flex-shrink-0">
                                                    <Phone size={20} />
                                                </div>
                                                <div>
                                                    <h3 className="font-semibold text-gray-900">Export Calls</h3>
                                                    <p className="text-xs text-gray-500 mt-1 leading-relaxed">
                                                        Export call logs including duration, type, and notes.
                                                    </p>
                                                </div>
                                            </div>

                                            <div className="space-y-4 relative">
                                                <div className="space-y-1.5">
                                                    <label className="text-[10px] uppercase font-bold text-gray-400 tracking-wider">Date Range</label>
                                                    <select
                                                        value={exportDateRange}
                                                        onChange={(e) => setExportDateRange(e.target.value)}
                                                        className="w-full text-sm bg-gray-50 border border-gray-200 rounded-lg py-2 px-3 outline-none focus:ring-2 focus:ring-blue-500/10 focus:border-blue-500 transition-all cursor-pointer hover:bg-white"
                                                    >
                                                        <option value="all">All Time</option>
                                                        <option value="today">Today</option>
                                                        <option value="7days">Last 7 Days</option>
                                                        <option value="30days">Last 30 Days</option>
                                                        <option value="90days">Last 90 Days</option>
                                                    </select>
                                                </div>

                                                <button
                                                    onClick={handleExportCalls}
                                                    disabled={exportLoading.calls}
                                                    className="btn w-full btn-primary h-10 gap-2 shadow-lg shadow-blue-500/10 active:scale-95 transition-transform"
                                                >
                                                    {exportLoading.calls ? (
                                                        <>
                                                            <Loader2 size={16} className="animate-spin" />
                                                            Processing...
                                                        </>
                                                    ) : (
                                                        <>
                                                            <FileSpreadsheet size={16} />
                                                            Download CSV
                                                        </>
                                                    )}
                                                </button>
                                            </div>
                                        </div>

                                        {/* Callers Export */}
                                        <div className="p-5 bg-white rounded-xl border border-gray-200 hover:border-purple-300 hover:shadow-md transition-all group relative overflow-hidden">
                                            <div className="absolute top-0 right-0 w-24 h-24 bg-purple-50/50 rounded-full -mr-12 -mt-12 group-hover:bg-purple-50 transition-colors" />

                                            <div className="flex items-start gap-4 mb-5 relative">
                                                <div className="p-3 bg-purple-50 text-purple-600 rounded-lg group-hover:scale-105 transition-transform flex-shrink-0">
                                                    <Users size={20} />
                                                </div>
                                                <div>
                                                    <h3 className="font-semibold text-gray-900">Export Contacts</h3>
                                                    <p className="text-xs text-gray-500 mt-1 leading-relaxed">
                                                        Export unique contacts and their statistics.
                                                    </p>
                                                </div>
                                            </div>

                                            <div className="mt-auto pt-4 space-y-4 relative flex flex-col justify-end h-full">
                                                {/* Spacer to push button down if needed, but flex-col handles it */}
                                                <div className="flex-1" />
                                                <button
                                                    onClick={handleExportCallers}
                                                    disabled={exportLoading.callers}
                                                    className="btn w-full h-10 gap-2 bg-white border border-purple-200 text-purple-700 hover:bg-purple-50 hover:border-purple-300 transition-all active:scale-95"
                                                >
                                                    {exportLoading.callers ? (
                                                        <>
                                                            <Loader2 size={16} className="animate-spin text-purple-600" />
                                                            Processing...
                                                        </>
                                                    ) : (
                                                        <>
                                                            <FileSpreadsheet size={16} />
                                                            Download CSV
                                                        </>
                                                    )}
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}

                    </div>
                </main>

                {/* Notifications Area (Feedback) */}
                <div className="fixed bottom-8 right-8 z-50 flex flex-col gap-2 pointer-events-none">
                    {success && (
                        <div className="bg-emerald-600 text-white px-4 py-3 rounded-xl shadow-xl shadow-emerald-500/20 flex items-center gap-3 animate-in slide-in-from-bottom-5 fade-in duration-300 pointer-events-auto">
                            <CheckCircle size={20} className="text-emerald-100" />
                            <span className="font-medium text-sm">{success}</span>
                        </div>
                    )}
                    {error && (
                        <div className="bg-red-600 text-white px-4 py-3 rounded-xl shadow-xl shadow-red-500/20 flex items-center gap-3 animate-in slide-in-from-bottom-5 fade-in duration-300 pointer-events-auto">
                            <AlertCircle size={20} className="text-red-100" />
                            <span className="font-medium text-sm">{error}</span>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
