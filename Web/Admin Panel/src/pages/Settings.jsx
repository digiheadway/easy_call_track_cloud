import { useState, useEffect } from 'react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Save, ShieldCheck, Lock, Building, AlertCircle, Download, FileSpreadsheet, Users, Phone, Loader2 } from 'lucide-react';

export default function Settings() {
    const { user } = useAuth();
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
            // Update Organization Name
            // Using /settings.php or /organization.php. Assuming /settings.php handles profile updates.
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

    return (
        <div className="max-w-4xl mx-auto space-y-8">
            <div>
                <h1 className="text-2xl font-bold tracking-tight">Settings</h1>
                <p className="text-gray-500 text-sm mt-1.5">Manage your organization details and security preferences.</p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Navigation / Sidebar for Settings could go here if more complex, keeping simple for now */}

                <div className="lg:col-span-2 space-y-8">

                    {/* Organization Profile */}
                    <div className="card">
                        <section className="space-y-6">
                            <div className="flex items-center gap-3 border-b border-gray-100 pb-4">
                                <div className="p-2 bg-blue-50 text-blue-600 rounded-lg">
                                    <Building size={20} />
                                </div>
                                <h2 className="text-lg font-semibold">Organization Profile</h2>
                            </div>

                            <form onSubmit={handleSaveProfile} className="space-y-5">
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                                    <div>
                                        <label className="block text-sm font-medium mb-1.5 text-gray-700">Organization ID</label>
                                        <div className="relative">
                                            <input
                                                type="text"
                                                value={user?.org_id || ''}
                                                disabled
                                                className="input bg-gray-50 text-gray-500 font-mono cursor-not-allowed pr-10"
                                            />
                                            <ShieldCheck size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400" />
                                        </div>
                                    </div>

                                    <div>
                                        <label className="block text-sm font-medium mb-1.5 text-gray-700">Admin Email</label>
                                        <input
                                            type="email"
                                            value={user?.email || ''}
                                            disabled
                                            className="input bg-gray-50 text-gray-500 cursor-not-allowed"
                                        />
                                    </div>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium mb-1.5 text-gray-700">Organization Name</label>
                                    <input
                                        type="text"
                                        className="input"
                                        value={orgName}
                                        onChange={(e) => setOrgName(e.target.value)}
                                        placeholder="Enter organization name"
                                    />
                                    <p className="text-[11px] text-gray-400 mt-1.5">This name will be displayed in reports and invoices.</p>
                                </div>

                                <div className="pt-2">
                                    <button
                                        type="submit"
                                        className="btn btn-primary h-10 px-5 gap-2"
                                        disabled={loading}
                                    >
                                        Save Profile
                                    </button>
                                </div>
                            </form>
                        </section>
                    </div>

                    {/* Security Settings */}
                    <div className="card">
                        <section className="space-y-6">
                            <div className="flex items-center gap-3 border-b border-gray-100 pb-4">
                                <div className="p-2 bg-purple-50 text-purple-600 rounded-lg">
                                    <Lock size={20} />
                                </div>
                                <h2 className="text-lg font-semibold">Security</h2>
                            </div>

                            <form onSubmit={handleChangePassword} className="space-y-5">
                                <div>
                                    <label className="block text-sm font-medium mb-1.5 text-gray-700">Current Password</label>
                                    <input
                                        type="password"
                                        className="input"
                                        value={passwords.current}
                                        onChange={(e) => setPasswords({ ...passwords, current: e.target.value })}
                                        placeholder="••••••••"
                                    />
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                                    <div>
                                        <label className="block text-sm font-medium mb-1.5 text-gray-700">New Password</label>
                                        <input
                                            type="password"
                                            className="input"
                                            value={passwords.new}
                                            onChange={(e) => setPasswords({ ...passwords, new: e.target.value })}
                                            placeholder="••••••••"
                                            minLength={6}
                                        />
                                    </div>

                                    <div>
                                        <label className="block text-sm font-medium mb-1.5 text-gray-700">Confirm Password</label>
                                        <input
                                            type="password"
                                            className="input"
                                            value={passwords.confirm}
                                            onChange={(e) => setPasswords({ ...passwords, confirm: e.target.value })}
                                            placeholder="••••••••"
                                        />
                                    </div>
                                </div>

                                <div className="pt-2">
                                    <button
                                        type="submit"
                                        className="btn btn-outline h-10 px-5 gap-2"
                                        disabled={loading || !passwords.current || !passwords.new}
                                    >
                                        Change Password
                                    </button>
                                </div>
                            </form>
                        </section>
                    </div>

                    {/* Data Export */}
                    <div className="card">
                        <section className="space-y-6">
                            <div className="flex items-center gap-3 border-b border-gray-100 pb-4">
                                <div className="p-2 bg-emerald-50 text-emerald-600 rounded-lg">
                                    <Download size={20} />
                                </div>
                                <div>
                                    <h2 className="text-lg font-semibold">Data Export</h2>
                                    <p className="text-xs text-gray-500 mt-0.5">Export your calls and callers data to CSV format</p>
                                </div>
                            </div>

                            <div className="space-y-5">
                                {/* Calls Export */}
                                <div className="p-4 bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl border border-blue-100">
                                    <div className="flex items-start justify-between gap-4">
                                        <div className="flex items-start gap-3">
                                            <div className="p-2.5 bg-white rounded-lg shadow-sm border border-blue-100">
                                                <Phone size={18} className="text-blue-600" />
                                            </div>
                                            <div>
                                                <h3 className="font-semibold text-gray-800">Export Calls</h3>
                                                <p className="text-xs text-gray-500 mt-0.5">
                                                    Download all call records including contact name, phone, duration, type, notes, labels, and more.
                                                </p>
                                            </div>
                                        </div>
                                    </div>

                                    <div className="mt-4 flex flex-wrap items-center gap-3">
                                        <div className="flex items-center gap-2">
                                            <label className="text-xs font-medium text-gray-600">Date Range:</label>
                                            <select
                                                value={exportDateRange}
                                                onChange={(e) => setExportDateRange(e.target.value)}
                                                className="text-sm bg-white border border-gray-200 rounded-lg py-1.5 px-3 outline-none focus:ring-2 focus:ring-blue-200"
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
                                            className="btn btn-primary h-9 px-4 gap-2 text-sm shadow-md shadow-blue-100"
                                        >
                                            {exportLoading.calls ? (
                                                <>
                                                    <Loader2 size={14} className="animate-spin" />
                                                    Exporting...
                                                </>
                                            ) : (
                                                <>
                                                    <FileSpreadsheet size={14} />
                                                    Export to CSV
                                                </>
                                            )}
                                        </button>
                                    </div>
                                </div>

                                {/* Callers/Contacts Export */}
                                <div className="p-4 bg-gradient-to-br from-purple-50 to-pink-50 rounded-xl border border-purple-100">
                                    <div className="flex items-start justify-between gap-4">
                                        <div className="flex items-start gap-3">
                                            <div className="p-2.5 bg-white rounded-lg shadow-sm border border-purple-100">
                                                <Users size={18} className="text-purple-600" />
                                            </div>
                                            <div>
                                                <h3 className="font-semibold text-gray-800">Export Callers</h3>
                                                <p className="text-xs text-gray-500 mt-0.5">
                                                    Download all caller/contact records including name, phone, labels, notes, call stats, and more.
                                                </p>
                                            </div>
                                        </div>
                                    </div>

                                    <div className="mt-4">
                                        <button
                                            onClick={handleExportCallers}
                                            disabled={exportLoading.callers}
                                            className="btn h-9 px-4 gap-2 text-sm bg-purple-600 hover:bg-purple-700 text-white shadow-md shadow-purple-100"
                                        >
                                            {exportLoading.callers ? (
                                                <>
                                                    <Loader2 size={14} className="animate-spin" />
                                                    Exporting...
                                                </>
                                            ) : (
                                                <>
                                                    <FileSpreadsheet size={14} />
                                                    Export to CSV
                                                </>
                                            )}
                                        </button>
                                    </div>
                                </div>

                                <p className="text-[11px] text-gray-400 flex items-center gap-1.5">
                                    <AlertCircle size={12} />
                                    CSV files can be opened in Excel, Google Sheets, or any spreadsheet application.
                                </p>
                            </div>
                        </section>
                    </div>

                </div>

                {/* Notifications Area (Feedback) */}
                <div className="fixed bottom-8 right-8 z-50 flex flex-col gap-2">
                    {success && (
                        <div className="bg-green-600 text-white px-4 py-3 rounded-xl shadow-lg flex items-center gap-3 animate-in slide-in-from-bottom-5 fade-in duration-300">
                            <CheckCircle size={20} />
                            <span className="font-medium text-sm">{success}</span>
                        </div>
                    )}
                    {error && (
                        <div className="bg-red-600 text-white px-4 py-3 rounded-xl shadow-lg flex items-center gap-3 animate-in slide-in-from-bottom-5 fade-in duration-300">
                            <AlertCircle size={20} />
                            <span className="font-medium text-sm">{error}</span>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

// Helper icon component since CheckCircle was reused
import { CheckCircle as CheckCircleIcon } from 'lucide-react';
function CheckCircle({ size, className }) { return <CheckCircleIcon size={size} className={className} /> }
