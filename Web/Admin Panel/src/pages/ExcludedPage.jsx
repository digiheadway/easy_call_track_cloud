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
    AlertTriangle
} from 'lucide-react';
import { toast } from 'sonner';
import Modal from '../components/Modal';

export default function ExcludedPage() {
    const [contacts, setContacts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [formData, setFormData] = useState({ phone: '', name: '', is_active: true });
    const [saving, setSaving] = useState(false);
    const [deleting, setDeleting] = useState(false);

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
            await api.post('/excluded_contacts.php', {
                ...formData,
                is_active: formData.is_active ? 1 : 0
            });
            setIsModalOpen(false);
            setFormData({ phone: '', name: '', is_active: true });
            toast.success('Contact added to exclusion list');
            fetchContacts();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to add contact');
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (e, id) => {
        e.preventDefault();
        e.stopPropagation();
        if (!window.confirm('Remove this exclusion?')) return;
        try {
            await api.delete(`/excluded_contacts.php?id=${id}`);
            toast.success('Exclusion removed');
            fetchContacts();
        } catch (err) {
            toast.error('Failed to remove exclusion');
        }
    };

    const handleDeleteAllData = async (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (!window.confirm('WARNING: This will permanently delete ALL call history, recordings, and contact details for EVERY contact in this exclusion list. This action cannot be undone. Are you sure?')) return;

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
    };

    const handleDeleteContactData = async (e, phone) => {
        e.preventDefault();
        e.stopPropagation();
        if (!window.confirm(`Permanently delete all call history, recordings, and contact data for ${phone}?`)) return;

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
    };

    const toggleStatus = async (e, contact) => {
        e.preventDefault();
        e.stopPropagation();
        try {
            await api.put(`/excluded_contacts.php?id=${contact.id}`, {
                is_active: contact.is_active == 1 ? 0 : 1
            });
            fetchContacts();
        } catch (err) {
            toast.error('Failed to update status');
        }
    };

    const filtered = contacts.filter(c =>
        c.phone.includes(search) ||
        (c.name && c.name.toLowerCase().includes(search.toLowerCase()))
    );

    return (
        <div className="space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-gray-900">Excluded Contacts</h1>
                    <p className="text-gray-500 text-sm mt-1">Numbers in this list will not be synced or tracked across the organization.</p>
                </div>

                <div className="flex items-center gap-3 w-full sm:w-auto">
                    <div className="relative flex-1 sm:w-64">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                        <input
                            type="text"
                            placeholder="Search numbers..."
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            className="pl-9 pr-4 py-2 text-sm border border-gray-200 rounded-lg w-full focus:ring-2 focus:ring-blue-500 outline-none"
                        />
                    </div>
                    {contacts.length > 0 && (
                        <button
                            type="button"
                            onClick={(e) => handleDeleteAllData(e)}
                            disabled={deleting}
                            className="btn bg-red-50 text-red-600 border-red-100 hover:bg-red-100 whitespace-nowrap gap-2"
                            title="Erase all history for all excluded numbers"
                        >
                            <Database size={18} />
                            Erase All History
                        </button>
                    )}
                    <button
                        type="button"
                        onClick={() => setIsModalOpen(true)}
                        className="btn btn-primary whitespace-nowrap gap-2"
                    >
                        <Plus size={18} />
                        Add Number
                    </button>
                </div>
            </div>

            <div className="card !p-0 overflow-hidden border border-gray-200 shadow-sm">
                <div className="overflow-x-auto">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-gray-50 border-b border-gray-100 text-gray-500 font-medium">
                            <tr>
                                <th className="px-6 py-4">Contact</th>
                                <th className="px-6 py-4">Phone Number</th>
                                <th className="px-6 py-4">Status</th>
                                <th className="px-6 py-4">Added On</th>
                                <th className="px-6 py-4 text-right">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {filtered.map((c) => (
                                <tr key={c.id} className="hover:bg-gray-50 transition-colors">
                                    <td className="px-6 py-4 font-medium text-gray-900">
                                        <div className="flex items-center gap-3">
                                            <div className="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center text-gray-500">
                                                <User size={14} />
                                            </div>
                                            {c.name || <span className="text-gray-400 italic">No name</span>}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-2 text-gray-600">
                                            <Phone size={14} className="text-gray-400" />
                                            {c.phone}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <button
                                            type="button"
                                            onClick={(e) => toggleStatus(e, c)}
                                            className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${c.is_active == 1
                                                ? 'bg-green-50 text-green-700 border border-green-100'
                                                : 'bg-gray-50 text-gray-500 border border-gray-100'
                                                }`}
                                        >
                                            {c.is_active == 1 ? <CheckCircle size={12} /> : <XCircle size={12} />}
                                            {c.is_active == 1 ? 'Active' : 'Disabled'}
                                        </button>
                                    </td>
                                    <td className="px-6 py-4 text-gray-500">
                                        {new Date(c.created_at).toLocaleDateString()}
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                        <div className="flex items-center justify-end gap-1">
                                            <button
                                                type="button"
                                                onClick={(e) => handleDeleteContactData(e, c.phone)}
                                                disabled={deleting}
                                                className="p-2 hover:bg-orange-50 text-gray-400 hover:text-orange-600 rounded-lg transition-colors"
                                                title="Erase all history for this contact"
                                            >
                                                <RotateCcw size={18} />
                                            </button>
                                            <button
                                                type="button"
                                                onClick={(e) => handleDelete(e, c.id)}
                                                className="p-2 hover:bg-red-50 text-gray-400 hover:text-red-600 rounded-lg transition-colors"
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
                onClose={() => setIsModalOpen(false)}
                title="Exclude New Contact"
            >
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Phone Number</label>
                        <input
                            type="text"
                            required
                            placeholder="e.g. +919876543210"
                            className="input"
                            value={formData.phone}
                            onChange={e => setFormData({ ...formData, phone: e.target.value })}
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Display Name (Optional)</label>
                        <input
                            type="text"
                            placeholder="e.g. Personal Phone"
                            className="input"
                            value={formData.name}
                            onChange={e => setFormData({ ...formData, name: e.target.value })}
                        />
                    </div>
                    <div className="flex items-center gap-2 pt-2">
                        <input
                            type="checkbox"
                            id="is_active"
                            checked={formData.is_active}
                            onChange={e => setFormData({ ...formData, is_active: e.target.checked })}
                            className="w-4 h-4 text-blue-600 rounded border-gray-300 focus:ring-blue-500"
                        />
                        <label htmlFor="is_active" className="text-sm text-gray-700">Enable this exclusion immediately</label>
                    </div>
                    <div className="pt-4 flex gap-3">
                        <button type="button" onClick={() => setIsModalOpen(false)} className="flex-1 btn bg-gray-100 text-gray-700">Cancel</button>
                        <button type="submit" disabled={saving} className="flex-1 btn btn-primary">
                            {saving ? 'Saving...' : 'Add Exclusion'}
                        </button>
                    </div>
                </form>
            </Modal>
        </div>
    );
}
