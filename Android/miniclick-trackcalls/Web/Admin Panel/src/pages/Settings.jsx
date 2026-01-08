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
    CheckCircle,
    RefreshCw,
    HardDrive,
    Trash2,
    Clock
} from 'lucide-react';

export default function Settings() {
    const { user } = useAuth();
    const [activeTab, setActiveTab] = useState('profile');
    const [formData, setFormData] = useState({
        name: '',
        org_name: '',
        billing_address: '',
        gst_number: '',
        state: ''
    });

    useEffect(() => {
        if (user) {
            setFormData({
                name: user.name || '',
                org_name: user.org_name || '',
                billing_address: user.billing_address || '',
                gst_number: user.gst_number || '',
                state: user.state || ''
            });
        }
    }, [user]);

    const handleSaveProfile = async (e) => {
        e.preventDefault();
        setLoading(true);
        setSuccess('');
        setError('');
        try {
            await api.put('/settings.php', {
                action: 'update_profile',
                ...formData
            });
            setSuccess('Profile updated successfully!');
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to update profile');
        } finally {
            setLoading(false);
        }
    };

    // ... (handleChangePassword and handleExportCalls remain same)

    const tabs = [
        { id: 'profile', label: 'Profile', icon: Users },
        { id: 'security', label: 'Security', icon: Lock },
        { id: 'exports', label: 'Data Export', icon: Download },
    ];

    // ... (formatBytes remains same)

    return (
        <div className="max-w-6xl mx-auto space-y-8">
            <div>
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Settings</h1>
                <p className="text-gray-500 dark:text-gray-400 text-sm mt-1">Manage your account and preferences.</p>
            </div>

            <div className="flex flex-col lg:flex-row gap-8 items-start">
                <aside className="w-full lg:w-64">
                    <nav className="flex lg:flex-col gap-1.5 overflow-x-auto">
                        {tabs.map((tab) => {
                            const Icon = tab.icon;
                            const isActive = activeTab === tab.id;
                            return (
                                <button
                                    key={tab.id}
                                    onClick={() => setActiveTab(tab.id)}
                                    className={`flex items-center gap-3 px-4 py-2.5 text-sm font-medium rounded-lg transition-all ${isActive ? 'bg-white dark:bg-gray-800 text-blue-600 dark:text-blue-400 shadow-sm border border-gray-200 dark:border-gray-700' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'}`}
                                >
                                    <Icon size={18} className={isActive ? 'text-blue-600 dark:text-blue-400' : 'text-gray-400 dark:text-gray-500'} />
                                    {tab.label}
                                </button>
                            );
                        })}
                    </nav>
                </aside>

                <main className="flex-1 w-full bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 shadow-sm">
                    {activeTab === 'profile' && (
                        <div className="p-6 space-y-6">
                            <h2 className="text-lg font-semibold text-gray-900 dark:text-white border-b dark:border-gray-700 pb-4">Profile</h2>
                            <form onSubmit={handleSaveProfile} className="max-w-2xl space-y-6">
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                    <div>
                                        <label className="block text-xs font-bold uppercase text-gray-500 dark:text-gray-400 mb-2">Full Name</label>
                                        <input
                                            type="text"
                                            className="input dark:bg-gray-900 dark:border-gray-700 dark:text-white"
                                            value={formData.name}
                                            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-xs font-bold uppercase text-gray-500 dark:text-gray-400 mb-2">Organization Name</label>
                                        <input
                                            type="text"
                                            className="input dark:bg-gray-900 dark:border-gray-700 dark:text-white"
                                            value={formData.org_name}
                                            onChange={(e) => setFormData({ ...formData, org_name: e.target.value })}
                                        />
                                    </div>

                                    <div>
                                        <label className="block text-xs font-bold uppercase text-gray-500 dark:text-gray-400 mb-2">Organization Unique ID</label>
                                        <div className="relative">
                                            <input type="text" value={user?.org_id || ''} disabled className="input bg-gray-50 dark:bg-gray-900 cursor-not-allowed text-gray-500 dark:text-gray-500 pr-10" />
                                            <Lock size={14} className="absolute right-3 top-3 text-gray-400" />
                                        </div>
                                    </div>

                                    <div>
                                        <label className="block text-xs font-bold uppercase text-gray-500 dark:text-gray-400 mb-2">Email Address</label>
                                        <div className="relative">
                                            <input type="email" value={user?.email || ''} disabled className="input bg-gray-50 dark:bg-gray-900 cursor-not-allowed text-gray-500 dark:text-gray-500 pr-10" />
                                            <Lock size={14} className="absolute right-3 top-3 text-gray-400" />
                                        </div>
                                    </div>
                                </div>

                                <div>
                                    <label className="block text-xs font-bold uppercase text-gray-500 dark:text-gray-400 mb-2">Billing Address</label>
                                    <textarea
                                        rows="3"
                                        className="input dark:bg-gray-900 dark:border-gray-700 dark:text-white w-full"
                                        value={formData.billing_address}
                                        onChange={(e) => setFormData({ ...formData, billing_address: e.target.value })}
                                    ></textarea>
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                    <div>
                                        <label className="block text-xs font-bold uppercase text-gray-500 dark:text-gray-400 mb-2">GST Number</label>
                                        <input
                                            type="text"
                                            className="input dark:bg-gray-900 dark:border-gray-700 dark:text-white"
                                            value={formData.gst_number}
                                            onChange={(e) => setFormData({ ...formData, gst_number: e.target.value })}
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-xs font-bold uppercase text-gray-500 dark:text-gray-400 mb-2">State</label>
                                        <input
                                            type="text"
                                            className="input dark:bg-gray-900 dark:border-gray-700 dark:text-white"
                                            value={formData.state}
                                            onChange={(e) => setFormData({ ...formData, state: e.target.value })}
                                        />
                                    </div>
                                </div>

                                <div className="pt-4 border-t dark:border-gray-700">
                                    <button type="submit" disabled={loading} className="btn btn-primary px-6">
                                        {loading ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />}
                                        Save Changes
                                    </button>
                                </div>
                            </form>
                        </div>
                    )}


                    {activeTab === 'security' && (
                        <div className="p-6 space-y-6">
                            <h2 className="text-lg font-semibold text-gray-900 dark:text-white border-b dark:border-gray-700 pb-4">Security</h2>
                            <form onSubmit={handleChangePassword} className="max-w-xl space-y-6">
                                <div>
                                    <label className="block text-xs font-bold uppercase text-gray-500 dark:text-gray-400 mb-2">Current Password</label>
                                    <input type="password" underline="none" className="input dark:bg-gray-900 dark:border-gray-700 dark:text-white" value={passwords.current} onChange={(e) => setPasswords({ ...passwords, current: e.target.value })} />
                                </div>
                                <div className="grid grid-cols-2 gap-6">
                                    <div>
                                        <label className="block text-xs font-bold uppercase text-gray-500 dark:text-gray-400 mb-2">New Password</label>
                                        <input type="password" underline="none" className="input dark:bg-gray-900 dark:border-gray-700 dark:text-white" value={passwords.new} onChange={(e) => setPasswords({ ...passwords, new: e.target.value })} />
                                    </div>
                                    <div>
                                        <label className="block text-xs font-bold uppercase text-gray-500 dark:text-gray-400 mb-2">Confirm</label>
                                        <input type="password" underline="none" className="input dark:bg-gray-900 dark:border-gray-700 dark:text-white" value={passwords.confirm} onChange={(e) => setPasswords({ ...passwords, confirm: e.target.value })} />
                                    </div>
                                </div>
                                <button type="submit" disabled={loading} className="btn btn-primary px-6">Update Password</button>
                            </form>
                        </div>
                    )}

                    {activeTab === 'exports' && (
                        <div className="p-6 space-y-6">
                            <h2 className="text-lg font-semibold text-gray-900 dark:text-white border-b dark:border-gray-700 pb-4">Data Export</h2>
                            <div className="grid grid-cols-2 gap-6">
                                <div className="p-4 border dark:border-gray-700 rounded-2xl space-y-4">
                                    <h3 className="font-bold dark:text-white">Call Logs</h3>
                                    <select value={exportDateRange} onChange={(e) => setExportDateRange(e.target.value)} className="w-full input text-sm dark:bg-gray-900 dark:border-gray-700 dark:text-white">
                                        <option value="all">All Time</option>
                                        <option value="7days">Last 7 Days</option>
                                        <option value="30days">Last 30 Days</option>
                                    </select>
                                    <button onClick={handleExportCalls} disabled={exportLoading.calls} className="btn btn-primary w-full">
                                        {exportLoading.calls ? <Loader2 className="animate-spin" /> : <FileSpreadsheet size={16} />}
                                        Export CSV
                                    </button>
                                </div>
                                <div className="p-4 border dark:border-gray-700 rounded-2xl space-y-4">
                                    <h3 className="font-bold dark:text-white">Contacts</h3>
                                    <p className="text-xs text-gray-500 dark:text-gray-400">Export all unique callers and stats.</p>
                                    <button onClick={handleExportCallers} disabled={exportLoading.callers} className="btn w-full border border-gray-200 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700">
                                        {exportLoading.callers ? <Loader2 className="animate-spin" /> : <Users size={16} />}
                                        Export CSV
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}
                </main>
            </div>

            <div className="fixed bottom-8 right-8 pointer-events-none space-y-2">
                {success && <div className="bg-emerald-600 text-white px-4 py-2 rounded-lg shadow-xl animate-in fade-in transition-all">{success}</div>}
                {error && <div className="bg-red-600 text-white px-4 py-2 rounded-lg shadow-xl animate-in fade-in transition-all">{error}</div>}
            </div>
        </div>
    );
}
