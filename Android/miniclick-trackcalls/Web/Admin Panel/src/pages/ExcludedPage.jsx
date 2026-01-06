import { useState, useEffect } from 'react';
import api from '../api/client';
import {
    Plus,
    Search,
    Trash2,
    UserX,
    Phone,
    User,
    CheckCircle,
    XCircle,
    Database,
    RotateCcw,
    AlertTriangle,
    Eye,
    EyeOff,
    Cloud,
    CloudOff,
    Settings2
} from 'lucide-react';
import { toast } from 'sonner';
import Modal from '../components/Modal';

export default function ExcludedPage() {
    const [contacts, setContacts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [activeTab, setActiveTab] = useState('full'); // 'full' or 'privacy'
    const [formData, setFormData] = useState({ phone: '', name: '', type: 'full' });
    const [editingId, setEditingId] = useState(null);
    const [saving, setSaving] = useState(false);
    const [deleting, setDeleting] = useState(false);
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
        fetchContacts();
    }, []);

    const fetchContacts = async () => {
        setLoading(true);
        try {
            const res = await api.get('/excluded_contacts.php');
            if (res.data && Array.isArray(res.data)) {
                setContacts(res.data);
            }
        } catch (err) {
            console.error("Failed to fetch excluded contacts", err);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            if (editingId) {
                await api.put(`/excluded_contacts.php?id=${editingId}`, {
                    phone: formData.phone,
                    name: formData.name,
                    exclude_from_sync: formData.type === 'full' ? 1 : 0,
                    exclude_from_list: 1
                });
                toast.success('Exclusion updated');
            } else {
                await api.post('/excluded_contacts.php', {
                    phone: formData.phone,
                    name: formData.name,
                    exclude_from_sync: formData.type === 'full' ? 1 : 0,
                    exclude_from_list: 1
                });
                toast.success('Contact added to exclusion list');
            }
            setIsModalOpen(false);
            setEditingId(null);
            setFormData({ phone: '', name: '', type: activeTab });
            fetchContacts();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to save');
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = (e, id) => {
        e.preventDefault();
        e.stopPropagation();
        setConfirmModal({
            isOpen: true,
            title: 'Remove Exclusion',
            message: 'Are you sure you want to remove this contact from the exclusion list?',
            isDestructive: true,
            onConfirm: async () => {
                try {
                    await api.delete(`/excluded_contacts.php?id=${id}`);
                    toast.success('Exclusion removed');
                    fetchContacts();
                } catch (err) {
                    toast.error('Failed to remove exclusion');
                }
            }
        });
    };

    const handleDeleteAllData = (e) => {
        e.preventDefault();
        e.stopPropagation();
        setConfirmModal({
            isOpen: true,
            title: 'ERASE ALL HISTORY',
            message: 'WARNING: This will permanently delete ALL call history, recordings, and contact details for EVERY contact in this exclusion list. This action cannot be undone. Are you sure?',
            isDestructive: true,
            onConfirm: async () => {
                setDeleting(true);
                try {
                    await api.post('/excluded_contacts.php?action=delete_all_data');
                    toast.success('Successfully erased all history for excluded contacts');
                    fetchContacts();
                } catch (err) {
                    toast.error('Failed to erase all data');
                } finally {
                    setDeleting(false);
                }
            }
        });
    };

    const handleDeleteContactData = (e, phone) => {
        e.preventDefault();
        e.stopPropagation();
        setConfirmModal({
            isOpen: true,
            title: 'Erase History',
            message: `Permanently delete all call history, recordings, and contact data for ${phone}?`,
            isDestructive: true,
            onConfirm: async () => {
                setDeleting(true);
                try {
                    await api.post('/excluded_contacts.php?action=delete_contact_data', { phone });
                    toast.success(`History for ${phone} erased`);
                    fetchContacts();
                } catch (err) {
                    toast.error('Failed to erase contact history');
                } finally {
                    setDeleting(false);
                }
            }
        });
    };

    const handleEdit = (contact) => {
        setEditingId(contact.id);
        setFormData({
            phone: contact.phone,
            name: contact.name || '',
            type: contact.exclude_from_sync == 1 ? 'full' : 'privacy'
        });
        setIsModalOpen(true);
    };

    const filtered = contacts.filter(c => {
        const matchesSearch = c.phone.includes(search) || (c.name && c.name.toLowerCase().includes(search.toLowerCase()));
        const isFull = c.exclude_from_sync == 1 && c.exclude_from_list == 1;
        const isPrivacy = c.exclude_from_sync == 0 && c.exclude_from_list == 1;

        if (activeTab === 'full') return matchesSearch && isFull;
        if (activeTab === 'privacy') return matchesSearch && isPrivacy;
        return matchesSearch;
    });

    return (
        <div className="space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-gray-900 dark:text-white">Privacy Exclusions</h1>
                    <p className="text-gray-500 dark:text-gray-400 text-sm mt-1">Manage numbers excluded from syncing or hidden from the organization's call logs.</p>
                </div>

                <div className="flex items-center gap-3 w-full sm:w-auto">
                    <div className="relative flex-1 sm:w-64">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 dark:text-gray-500" size={16} />
                        <input
                            type="text"
                            placeholder="Search numbers..."
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            className="pl-9 pr-4 py-2 text-sm border border-gray-200 dark:border-gray-700 rounded-lg w-full focus:ring-2 focus:ring-blue-500 outline-none bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 transition-colors"
                        />
                    </div>
                    {contacts.length > 0 && (
                        <button
                            type="button"
                            onClick={(e) => handleDeleteAllData(e)}
                            disabled={deleting}
                            className="btn bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 border-red-100 dark:border-red-900/30 hover:bg-red-100 dark:hover:bg-red-900/40 whitespace-nowrap gap-2"
                            title="Erase all history for all excluded numbers"
                        >
                            <Database size={18} />
                            Erase All History
                        </button>
                    )}
                    <button
                        type="button"
                        onClick={() => {
                            setEditingId(null);
                            setFormData({ phone: '', name: '', type: activeTab });
                            setIsModalOpen(true);
                        }}
                        className="btn btn-primary whitespace-nowrap gap-2"
                    >
                        <Plus size={18} />
                        Add Number
                    </button>
                </div>
            </div>

            <div className="card !p-0 overflow-hidden border border-gray-200 dark:border-gray-700 shadow-sm bg-white dark:bg-gray-800">
                <div className="flex border-b border-gray-200 dark:border-gray-700 px-6">
                    <button
                        onClick={() => setActiveTab('full')}
                        className={`py-4 px-6 text-sm font-medium border-b-2 transition-colors ${activeTab === 'full'
                            ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                            : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
                            }`}
                    >
                        No Tracking
                        <span className="ml-2 px-2 py-0.5 rounded-full bg-gray-100 dark:bg-gray-700 text-xs">
                            {contacts.filter(c => c.exclude_from_sync == 1 && c.exclude_from_list == 1).length}
                        </span>
                    </button>
                    <button
                        onClick={() => setActiveTab('privacy')}
                        className={`py-4 px-6 text-sm font-medium border-b-2 transition-colors ${activeTab === 'privacy'
                            ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                            : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
                            }`}
                    >
                        Excluded from lists
                        <span className="ml-2 px-2 py-0.5 rounded-full bg-gray-100 dark:bg-gray-700 text-xs">
                            {contacts.filter(c => c.exclude_from_sync == 0 && c.exclude_from_list == 1).length}
                        </span>
                    </button>
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-gray-50 dark:bg-gray-900/50 border-b border-gray-100 dark:border-gray-700 text-gray-500 dark:text-gray-400 font-medium">
                            <tr>
                                <th className="px-6 py-4">Contact</th>
                                <th className="px-6 py-4">Phone Number</th>
                                <th className="px-6 py-4">Mode</th>
                                <th className="px-6 py-4">Added On</th>
                                <th className="px-6 py-4 text-right">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                            {filtered.map((c) => (
                                <tr
                                    key={c.id}
                                    onClick={() => handleEdit(c)}
                                    className="hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors cursor-pointer group"
                                >
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-3">
                                            <div className="w-8 h-8 rounded-full bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 flex items-center justify-center font-bold text-xs">
                                                {(c.name || 'U').charAt(0).toUpperCase()}
                                            </div>
                                            <span className="font-medium text-gray-900 dark:text-white">{c.name || 'Unnamed Contact'}</span>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 text-gray-600 dark:text-gray-300 font-mono text-xs">
                                        {c.phone}
                                    </td>
                                    <td className="px-6 py-4">
                                        <div
                                            className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${c.exclude_from_sync == 1
                                                ? 'bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-400 border border-red-100 dark:border-red-900/30'
                                                : 'bg-indigo-50 dark:bg-indigo-900/20 text-indigo-700 dark:text-indigo-400 border border-indigo-100 dark:border-indigo-900/30'
                                                }`}
                                        >
                                            {c.exclude_from_sync == 1 ? <CloudOff size={12} /> : <Database size={12} />}
                                            {c.exclude_from_sync == 1 ? 'No Tracking' : 'Excluded from lists'}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 text-gray-500 dark:text-gray-400">
                                        {new Date(c.created_at).toLocaleDateString()}
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                        <div className="flex items-center justify-end gap-1">
                                            <button
                                                type="button"
                                                onClick={(e) => { e.stopPropagation(); handleDeleteContactData(e, c.phone); }}
                                                disabled={deleting}
                                                className="p-2 hover:bg-orange-50 dark:hover:bg-orange-900/20 text-gray-400 dark:text-gray-500 hover:text-orange-600 dark:hover:text-orange-400 rounded-lg transition-colors"
                                                title="Erase all history for this contact"
                                            >
                                                <RotateCcw size={18} />
                                            </button>
                                            <button
                                                type="button"
                                                onClick={(e) => { e.stopPropagation(); handleDelete(e, c.id); }}
                                                className="p-2 hover:bg-red-50 dark:hover:bg-red-900/20 text-gray-400 dark:text-gray-500 hover:text-red-600 dark:hover:text-red-400 rounded-lg transition-colors"
                                                title="Remove from exclusion list"
                                            >
                                                <Trash2 size={18} />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            {!loading && filtered.length === 0 && (
                                <tr>
                                    <td colSpan="5" className="p-12 text-center text-gray-400">
                                        <UserX size={48} className="mx-auto mb-4 opacity-20" />
                                        <p>No excluded contacts found.</p>
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            <Modal
                isOpen={isModalOpen}
                onClose={() => {
                    setIsModalOpen(false);
                    setEditingId(null);
                    setFormData({ phone: '', name: '', type: activeTab });
                }}
                title={editingId ? 'Edit Exclusion' : 'Add Number to Exclude'}
            >
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Phone Number</label>
                        <input
                            type="text"
                            required
                            placeholder="e.g. +919876543210"
                            className="input bg-white dark:bg-gray-800 dark:border-gray-700 dark:text-white"
                            value={formData.phone}
                            onChange={e => setFormData({ ...formData, phone: e.target.value })}
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Display Name (Optional)</label>
                        <input
                            type="text"
                            placeholder="e.g. Personal Phone"
                            className="input bg-white dark:bg-gray-800 dark:border-gray-700 dark:text-white"
                            value={formData.name}
                            onChange={e => setFormData({ ...formData, name: e.target.value })}
                        />
                    </div>
                    <div className="space-y-3 pt-2">
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Exclusion Type</label>
                        <div className="grid grid-cols-2 gap-3">
                            <button
                                type="button"
                                onClick={() => setFormData({ ...formData, type: 'full' })}
                                className={`p-3 rounded-xl border text-left transition-all ${formData.type === 'full'
                                    ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20 ring-1 ring-blue-500'
                                    : 'border-gray-200 dark:border-gray-700 hover:border-gray-300'
                                    }`}
                            >
                                <div className="flex items-center gap-2 mb-1">
                                    <CloudOff size={16} className={formData.type === 'full' ? 'text-blue-600' : 'text-gray-400'} />
                                    <span className="font-bold text-sm">No Tracking</span>
                                </div>
                                <p className="text-[11px] text-gray-500">Stop recording and hiding from logs.</p>
                            </button>

                            <button
                                type="button"
                                onClick={() => setFormData({ ...formData, type: 'privacy' })}
                                className={`p-3 rounded-xl border text-left transition-all ${formData.type === 'privacy'
                                    ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20 ring-1 ring-blue-500'
                                    : 'border-gray-200 dark:border-gray-700 hover:border-gray-300'
                                    }`}
                            >
                                <div className="flex items-center gap-2 mb-1">
                                    <EyeOff size={16} className={formData.type === 'privacy' ? 'text-blue-600' : 'text-gray-400'} />
                                    <span className="font-bold text-sm">Excluded from lists</span>
                                </div>
                                <p className="text-[11px] text-gray-500">Keep recording but hide from UI.</p>
                            </button>
                        </div>
                    </div>
                    <div className="pt-4 flex gap-3">
                        <button
                            type="button"
                            onClick={() => {
                                setIsModalOpen(false);
                                setEditingId(null);
                                setFormData({ phone: '', name: '', type: activeTab });
                            }}
                            className="flex-1 btn bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={saving || !formData.phone}
                            className="flex-1 btn btn-primary"
                        >
                            {saving ? 'Saving...' : editingId ? 'Update Settings' : 'Exclude Number'}
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
                    <div className="flex items-start gap-3 p-3 bg-orange-50 dark:bg-orange-900/20 rounded-lg text-orange-800 dark:text-orange-200 border border-orange-100 dark:border-orange-900/30">
                        <AlertTriangle className="shrink-0 mt-0.5" size={18} />
                        <p className="text-sm font-medium">{confirmModal.message}</p>
                    </div>
                    <div className="flex gap-3 pt-2">
                        <button
                            type="button"
                            onClick={() => setConfirmModal({ ...confirmModal, isOpen: false })}
                            className="flex-1 btn bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"
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
