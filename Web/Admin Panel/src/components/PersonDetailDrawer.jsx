import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { usePersonModal } from '../context/PersonModalContext';
import api from '../api/client';
import { format } from 'date-fns';
import {
    X, Phone, Clock, Smartphone,
    PhoneIncoming, PhoneOutgoing, PhoneMissed, User,
    MessageSquare, Edit3, FileText, CheckCircle2, Circle, Tag, UserX, Plus,
    Archive, Trash2, ShieldAlert, MoreVertical
} from 'lucide-react';
import { toast } from 'sonner';
import { useAudioPlayer } from '../context/AudioPlayerContext';
import { Play, Pause } from 'lucide-react';

export default function PersonDetailDrawer() {
    const { isOpen, personData, closePersonModal } = usePersonModal();
    const { playRecording, currentCall, isPlaying, currentTime, duration: activeDuration } = useAudioPlayer();
    const [calls, setCalls] = useState([]);
    const [loading, setLoading] = useState(false);

    // Person editing states
    const [contactName, setContactName] = useState('');
    const [personNote, setPersonNote] = useState('');
    const [editingField, setEditingField] = useState(null); // 'name' | 'personNote' | null
    const [editValue, setEditValue] = useState('');
    const [saving, setSaving] = useState(false);

    // Call note editing
    const [editingCallNote, setEditingCallNote] = useState(null);
    const [callNoteValue, setCallNoteValue] = useState('');

    // Person labels
    const [personLabels, setPersonLabels] = useState([]);
    const [showLabelModal, setShowLabelModal] = useState(false);
    const [labelSearch, setLabelSearch] = useState('');
    const [labelSuggestions, setLabelSuggestions] = useState([]);
    const [pendingLabels, setPendingLabels] = useState([]); // Labels being selected in modal

    // Exclude list
    const [isExcluded, setIsExcluded] = useState(false);
    const [excludeLoading, setExcludeLoading] = useState(false);

    // Actions loading inputs
    const [isArchiving, setIsArchiving] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);

    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [showManagement, setShowManagement] = useState(false);

    // Common label suggestions (can be fetched from API)
    const defaultLabelSuggestions = [
        'VIP', 'Customer', 'Lead', 'Spam', 'Important', 'Follow Up',
        'Resolved', 'Pending', 'Sales', 'Support', 'Partner', 'Vendor'
    ];

    useEffect(() => {
        if (isOpen && personData?.phone_number) {
            setContactName(personData.contact_name || '');
            setPersonNote(personData.person_note || '');
            const labels = personData.person_labels ? personData.person_labels.split(',').filter(Boolean) : [];
            setPersonLabels(labels);
            setPendingLabels(labels);
            setIsExcluded(Boolean(Number(personData.is_excluded)));
            fetchPersonHistory(personData.phone_number);
            fetchLabelSuggestions();
        } else {
            setCalls([]);
            setLoading(false);
            setEditingCallNote(null);
            setEditingField(null);
            setPersonLabels([]);
            setPendingLabels([]);
            setShowLabelModal(false);
            setIsExcluded(false);
        }
    }, [isOpen, personData]);

    const fetchLabelSuggestions = async () => {
        try {
            // Try to get used labels from API
            const res = await api.get('/contacts.php?action=labels');
            if (res.data?.data && Array.isArray(res.data.data)) {
                const apiLabels = res.data.data.map(l => l.label).filter(Boolean);
                const allLabels = [...new Set([...defaultLabelSuggestions, ...apiLabels])];
                setLabelSuggestions(allLabels);
            } else {
                setLabelSuggestions(defaultLabelSuggestions);
            }
        } catch {
            setLabelSuggestions(defaultLabelSuggestions);
        }
    };

    const fetchPersonHistory = async (phone) => {
        setLoading(true);
        try {
            const params = new URLSearchParams({
                search: phone,
                limit: 50,
                page: 1,
                archiveFilter: 'all'
            });

            const res = await api.get(`/calls.php?${params.toString()}`);

            if (res.data && Array.isArray(res.data.data)) {
                const exactMatches = res.data.data.filter(c => c.phone_number.includes(phone) || phone.includes(c.phone_number));
                setCalls(exactMatches);

                if (exactMatches.length > 0) {
                    const latest = exactMatches[0];
                    if (latest.contact_name) setContactName(latest.contact_name);
                    if (latest.person_note) setPersonNote(latest.person_note);
                }
            } else if (Array.isArray(res.data)) {
                const exactMatches = res.data.filter(c => c.phone_number.includes(phone) || phone.includes(c.phone_number));
                setCalls(exactMatches);
            }
        } catch (err) {
            console.error("Failed to fetch history", err);
        } finally {
            setLoading(false);
        }
    };

    const startEditingPerson = (field) => {
        setEditingField(field);
        setEditValue(field === 'name' ? contactName : personNote);
    };

    const cancelEditingPerson = () => {
        setEditingField(null);
        setEditValue('');
    };

    const savePersonField = async () => {
        if (!personData?.phone_number) return;
        setSaving(true);
        try {
            const callId = personData.id || (calls.length > 0 ? calls[0].id : null);

            if (callId) {
                const updates = editingField === 'name'
                    ? { contact_name: editValue }
                    : { person_note: editValue };

                await api.post(`/calls.php?action=update&id=${callId}`, updates);

                // Update local state
                if (editingField === 'name') {
                    setContactName(editValue);
                } else {
                    setPersonNote(editValue);
                }

                setEditingField(null);
                setEditValue('');
            }
        } catch (err) {
            console.error("Failed to save", err);
        } finally {
            setSaving(false);
        }
    };

    const handleSaveCallNote = async (callId) => {
        try {
            await api.post(`/calls.php?action=update&id=${callId}`, {
                note: callNoteValue
            });

            setCalls(prev => prev.map(c =>
                c.id === callId ? { ...c, note: callNoteValue } : c
            ));
            setEditingCallNote(null);
            setCallNoteValue('');
        } catch (err) {
            console.error("Failed to save call note", err);
        }
    };

    const startEditingCallNote = (call) => {
        setEditingCallNote(call.id);
        setCallNoteValue(call.note || '');
    };

    const handleToggleReviewed = async (callId, currentReviewed) => {
        const newReviewed = !currentReviewed;
        // Optimistic update
        setCalls(prev => prev.map(c =>
            c.id === callId ? { ...c, reviewed: newReviewed } : c
        ));
        try {
            await api.post(`/calls.php?action=update&id=${callId}`, { reviewed: newReviewed });
        } catch (err) {
            console.error("Failed to update reviewed status", err);
            // Revert
            setCalls(prev => prev.map(c =>
                c.id === callId ? { ...c, reviewed: currentReviewed } : c
            ));
        }
    };

    const handleMarkAllReviewed = async () => {
        // Optimistic update all calls
        const previousCalls = [...calls];
        setCalls(prev => prev.map(c => ({ ...c, reviewed: true })));

        try {
            // Update all calls
            await Promise.all(
                calls.filter(c => !Number(c.reviewed)).map(call =>
                    api.post(`/calls.php?action=update&id=${call.id}`, { reviewed: true })
                )
            );
        } catch (err) {
            console.error("Failed to mark all as reviewed", err);
            // Revert on error
            setCalls(previousCalls);
        }
    };

    const handleAddLabel = async () => {
        if (!labelSearch.trim()) return;
        const label = labelSearch.trim();
        if (!pendingLabels.includes(label)) {
            setPendingLabels([...pendingLabels, label]);
        }
        setLabelSearch('');
    };

    const toggleLabelSelection = (label) => {
        if (pendingLabels.includes(label)) {
            setPendingLabels(pendingLabels.filter(l => l !== label));
        } else {
            setPendingLabels([...pendingLabels, label]);
        }
    };

    const handleSaveLabels = async () => {
        if (!personData?.phone_number) return;
        setPersonLabels(pendingLabels);
        setShowLabelModal(false);

        try {
            const callId = personData.id || (calls.length > 0 ? calls[0].id : null);
            if (callId) {
                await api.post(`/calls.php?action=update&id=${callId}`, {
                    person_labels: pendingLabels.join(',')
                });
                toast.success('Labels updated');
            }
        } catch (err) {
            console.error('Failed to save labels', err);
            setPersonLabels(personLabels); // revert
            toast.error('Failed to save labels');
        }
    };

    const handleRemoveLabel = async (labelToRemove) => {
        const updatedLabels = personLabels.filter(l => l !== labelToRemove);
        setPersonLabels(updatedLabels);
        setPendingLabels(updatedLabels);

        try {
            const callId = personData.id || (calls.length > 0 ? calls[0].id : null);
            if (callId) {
                await api.post(`/calls.php?action=update&id=${callId}`, {
                    person_labels: updatedLabels.join(',')
                });
            }
        } catch (err) {
            console.error('Failed to remove label', err);
            setPersonLabels([...personLabels, labelToRemove]); // revert
        }
    };

    const openLabelModal = () => {
        setPendingLabels([...personLabels]);
        setLabelSearch('');
        setShowLabelModal(true);
    };

    const filteredSuggestions = labelSuggestions.filter(label =>
        label.toLowerCase().includes(labelSearch.toLowerCase())
    );

    const handleToggleExclude = async () => {
        if (!personData?.phone_number) return;
        setExcludeLoading(true);
        const newState = !isExcluded;

        try {
            // Call API to add/remove from exclude list
            await api.post('/contacts.php?action=exclude', {
                phone_number: personData.phone_number,
                excluded: newState
            });
            setIsExcluded(newState);
            toast.success(newState ? 'Added to exclude list' : 'Removed from exclude list');
        } catch (err) {
            console.error('Failed to update exclude status', err);
            toast.error('Failed to update exclude status');
        } finally {
            setExcludeLoading(false);
        }
    };

    const handleArchiveData = async () => {
        if (!personData?.phone_number) return;
        setIsArchiving(true);
        try {
            await api.post('/calls.php?action=archive_calls', {
                phone_number: personData.phone_number
            });
            toast.success('Calls archived');
            closePersonModal();
        } catch (err) {
            console.error('Failed to archive', err);
            toast.error('Failed to archive calls');
        } finally {
            setIsArchiving(false);
        }
    };

    const handleArchiveAndExclude = async () => {
        if (!personData?.phone_number) return;
        setIsArchiving(true);
        try {
            // 1. Archive
            await api.post('/calls.php?action=archive_calls', {
                phone_number: personData.phone_number
            });

            // 2. Exclude if not already
            if (!isExcluded) {
                await api.post('/contacts.php?action=exclude', {
                    phone_number: personData.phone_number,
                    excluded: true
                });
            }

            toast.success('Archived and Excluded');
            closePersonModal();
        } catch (err) {
            console.error('Failed to archive & exclude', err);
            toast.error('Failed to archive & exclude');
        } finally {
            setIsArchiving(false);
        }
    };

    const handleDeletePerson = async () => {
        if (!personData?.phone_number) return;
        setIsDeleting(true);
        try {
            await api.post('/calls.php?action=delete_person', {
                phone_number: personData.phone_number
            });
            toast.success('Person and data deleted');
            closePersonModal();
        } catch (err) {
            console.error('Failed to delete', err);
            toast.error('Failed to delete person');
        } finally {
            setIsDeleting(false);
        }
    };

    if (!isOpen) return null;

    const getTypeIcon = (type) => {
        if (type?.toLowerCase().includes('in')) return <PhoneIncoming size={14} className="text-blue-500" />;
        if (type?.toLowerCase().includes('out')) return <PhoneOutgoing size={14} className="text-green-500" />;
        return <PhoneMissed size={14} className="text-red-500" />;
    };

    return createPortal(
        <div className="fixed inset-0 z-[100] flex justify-end isolate">
            {/* Backdrop */}
            <div
                className="absolute inset-0 bg-black/30 backdrop-blur-[2px] transition-opacity duration-300"
                onClick={closePersonModal}
            />

            {/* Drawer */}
            <div className="relative w-full max-w-lg h-full bg-white shadow-2xl flex flex-col animate-in slide-in-from-right duration-300">

                {/* Header */}
                <div className="flex-none p-5 bg-gradient-to-b from-gray-50 to-white border-b border-gray-100">
                    <div className="flex justify-between items-start">
                        <div className="flex items-center gap-3">
                            <div className="w-12 h-12 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-lg font-bold">
                                {contactName ? contactName.charAt(0).toUpperCase() : <User size={24} />}
                            </div>
                            <div>
                                {/* Editable Name */}
                                {editingField === 'name' ? (
                                    <div className="flex items-center gap-2">
                                        <input
                                            type="text"
                                            value={editValue}
                                            onChange={(e) => setEditValue(e.target.value)}
                                            className="text-lg font-bold text-gray-900 border border-blue-300 rounded px-2 py-0.5 outline-none focus:ring-2 focus:ring-blue-500 w-40"
                                            autoFocus
                                            onKeyDown={(e) => {
                                                if (e.key === 'Enter') savePersonField();
                                                if (e.key === 'Escape') cancelEditingPerson();
                                            }}
                                        />
                                        <button onClick={savePersonField} disabled={saving} className="text-xs bg-blue-600 text-white px-2 py-1 rounded font-medium">
                                            {saving ? '...' : 'Save'}
                                        </button>
                                        <button onClick={cancelEditingPerson} className="text-xs text-gray-500 hover:text-gray-700">
                                            Cancel
                                        </button>
                                    </div>
                                ) : (
                                    <h2
                                        onClick={() => startEditingPerson('name')}
                                        className="text-xl font-bold text-gray-900 cursor-pointer hover:text-blue-600 transition-colors flex items-center gap-1 group"
                                    >
                                        {contactName || 'Unknown Contact'}
                                        <Edit3 size={14} className="opacity-0 group-hover:opacity-100 text-blue-500 transition-opacity" />
                                    </h2>
                                )}
                                <p className="text-gray-500 font-mono text-sm">{personData?.phone_number}</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-1">
                            {/* Three-dot menu for management actions */}
                            <div className="relative">
                                <button
                                    onClick={() => setShowManagement(!showManagement)}
                                    className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition-colors"
                                    title="More options"
                                >
                                    <MoreVertical size={20} />
                                </button>
                                {showManagement && (
                                    <>
                                        <div className="fixed inset-0 z-10" onClick={() => setShowManagement(false)} />
                                        <div className="absolute right-0 top-full mt-1 w-56 bg-white rounded-xl shadow-xl border border-gray-100 py-2 z-20 animate-in fade-in slide-in-from-top-2 duration-150">
                                            <button
                                                onClick={() => { handleToggleExclude(); setShowManagement(false); }}
                                                disabled={excludeLoading}
                                                className="w-full px-4 py-2.5 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-3 transition-colors"
                                            >
                                                <UserX size={16} className={isExcluded ? 'text-green-500' : 'text-gray-400'} />
                                                {excludeLoading ? 'Updating...' : isExcluded ? 'Remove from Exclude List' : 'Add to Exclude List'}
                                            </button>
                                            <div className="border-t border-gray-100 my-1" />
                                            <button
                                                onClick={() => { handleArchiveData(); setShowManagement(false); }}
                                                disabled={isArchiving}
                                                className="w-full px-4 py-2.5 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-3 transition-colors"
                                            >
                                                <Archive size={16} className="text-gray-400" />
                                                Archive All Calls
                                            </button>
                                            <button
                                                onClick={() => { handleArchiveAndExclude(); setShowManagement(false); }}
                                                disabled={isArchiving}
                                                className="w-full px-4 py-2.5 text-left text-sm text-orange-600 hover:bg-orange-50 flex items-center gap-3 transition-colors"
                                            >
                                                <ShieldAlert size={16} className="text-orange-500" />
                                                Archive & Exclude
                                            </button>
                                            <div className="border-t border-gray-100 my-1" />
                                            <button
                                                onClick={() => { setShowDeleteConfirm(true); setShowManagement(false); }}
                                                className="w-full px-4 py-2.5 text-left text-sm text-red-600 hover:bg-red-50 flex items-center gap-3 transition-colors"
                                            >
                                                <Trash2 size={16} className="text-red-500" />
                                                Delete Person & Data
                                            </button>
                                        </div>
                                    </>
                                )}
                            </div>
                            <button
                                onClick={closePersonModal}
                                className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition-colors"
                            >
                                <X size={24} />
                            </button>
                        </div>
                    </div>

                    {/* Labels - Quick Access in Header */}
                    <div className="mt-3 flex items-center gap-2 flex-wrap">
                        {personLabels.length === 0 ? (
                            <button
                                onClick={openLabelModal}
                                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-purple-50 text-purple-600 text-xs font-medium border border-purple-200 hover:bg-purple-100 transition-colors"
                            >
                                <Plus size={12} />
                                Add Label
                            </button>
                        ) : (
                            <>
                                {personLabels.map(label => (
                                    <span key={label} className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-purple-100 text-purple-700 text-xs font-medium border border-purple-200">
                                        {label}
                                        <button
                                            onClick={() => handleRemoveLabel(label)}
                                            className="hover:text-red-500 transition-colors"
                                        >
                                            <X size={12} />
                                        </button>
                                    </span>
                                ))}
                                <button
                                    onClick={openLabelModal}
                                    className="p-1.5 rounded-full text-purple-500 hover:bg-purple-50 transition-colors"
                                    title="Add more labels"
                                >
                                    <Plus size={14} />
                                </button>
                            </>
                        )}
                    </div>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-4 space-y-5">

                    {/* Person Note - Click to Edit */}
                    <div className="bg-amber-50 rounded-xl border border-amber-200 p-3">
                        <div className="flex items-center gap-2 mb-2">
                            <FileText size={14} className="text-amber-600" />
                            <span className="text-xs font-semibold text-amber-700 uppercase tracking-wide">Person Note</span>
                        </div>

                        {editingField === 'personNote' ? (
                            <div className="space-y-2">
                                <textarea
                                    value={editValue}
                                    onChange={(e) => setEditValue(e.target.value)}
                                    className="w-full text-sm p-2 border border-amber-300 rounded-lg focus:ring-2 focus:ring-amber-500 outline-none resize-none bg-white"
                                    rows={3}
                                    autoFocus
                                    placeholder="Add notes about this person..."
                                />
                                <div className="flex justify-end gap-2">
                                    <button onClick={cancelEditingPerson} className="text-xs text-gray-500 hover:text-gray-700 px-2 py-1">
                                        Cancel
                                    </button>
                                    <button
                                        onClick={savePersonField}
                                        disabled={saving}
                                        className="text-xs bg-amber-600 text-white px-3 py-1 rounded font-medium hover:bg-amber-700"
                                    >
                                        {saving ? 'Saving...' : 'Save'}
                                    </button>
                                </div>
                            </div>
                        ) : (
                            <div
                                onClick={() => startEditingPerson('personNote')}
                                className="cursor-pointer group flex items-start gap-2"
                            >
                                {personNote ? (
                                    <p className="text-sm text-amber-900 flex-1">{personNote}</p>
                                ) : (
                                    <p className="text-sm text-amber-600/60 italic flex-1">Click to add person note...</p>
                                )}
                                <Edit3 size={12} className="text-amber-400 group-hover:text-amber-600 transition-colors flex-shrink-0 mt-0.5" />
                            </div>
                        )}
                    </div>



                    {/* Label Picker Modal */}
                    {showLabelModal && createPortal(
                        <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-black/30 backdrop-blur-[2px]" onClick={() => setShowLabelModal(false)}>
                            <div
                                className="bg-white rounded-xl shadow-2xl w-full max-w-sm overflow-hidden animate-in fade-in zoom-in-95 duration-150"
                                onClick={e => e.stopPropagation()}
                            >
                                <div className="px-4 py-3 bg-purple-50 border-b border-purple-100 flex justify-between items-center">
                                    <h3 className="font-semibold text-purple-900 text-sm">Select Labels</h3>
                                    <button onClick={() => setShowLabelModal(false)} className="text-purple-400 hover:text-purple-600">
                                        <X size={18} />
                                    </button>
                                </div>

                                <div className="p-4">
                                    {/* Search Input */}
                                    <div className="relative mb-3">
                                        <input
                                            type="text"
                                            value={labelSearch}
                                            onChange={(e) => setLabelSearch(e.target.value)}
                                            placeholder="Search or add new label..."
                                            className="w-full text-sm px-3 py-2 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-purple-500 focus:border-purple-300"
                                            autoFocus
                                            onKeyDown={(e) => {
                                                if (e.key === 'Enter' && labelSearch.trim()) {
                                                    handleAddLabel();
                                                }
                                            }}
                                        />
                                        {labelSearch.trim() && !filteredSuggestions.includes(labelSearch.trim()) && (
                                            <button
                                                onClick={handleAddLabel}
                                                className="absolute right-2 top-1/2 -translate-y-1/2 text-xs bg-purple-600 text-white px-2 py-1 rounded font-medium hover:bg-purple-700"
                                            >
                                                Add "{labelSearch.trim()}"
                                            </button>
                                        )}
                                    </div>

                                    {/* Selected Labels */}
                                    {pendingLabels.length > 0 && (
                                        <div className="mb-3">
                                            <p className="text-[10px] font-semibold text-gray-400 uppercase mb-1.5">Selected</p>
                                            <div className="flex flex-wrap gap-1.5">
                                                {pendingLabels.map(label => (
                                                    <span
                                                        key={label}
                                                        onClick={() => toggleLabelSelection(label)}
                                                        className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-purple-600 text-white text-xs font-medium cursor-pointer hover:bg-purple-700 transition-colors"
                                                    >
                                                        {label}
                                                        <X size={12} />
                                                    </span>
                                                ))}
                                            </div>
                                        </div>
                                    )}

                                    {/* Suggestions */}
                                    <div>
                                        <p className="text-[10px] font-semibold text-gray-400 uppercase mb-1.5">Suggestions</p>
                                        <div className="flex flex-wrap gap-1.5 max-h-40 overflow-y-auto">
                                            {filteredSuggestions.length === 0 ? (
                                                <span className="text-xs text-gray-400 italic">No suggestions found</span>
                                            ) : (
                                                filteredSuggestions.map(label => (
                                                    <button
                                                        key={label}
                                                        onClick={() => toggleLabelSelection(label)}
                                                        className={`px-2 py-1 rounded-full text-xs font-medium border transition-all ${pendingLabels.includes(label)
                                                            ? 'bg-purple-100 text-purple-700 border-purple-300'
                                                            : 'bg-gray-50 text-gray-600 border-gray-200 hover:border-purple-300 hover:text-purple-600'
                                                            }`}
                                                    >
                                                        {label}
                                                    </button>
                                                ))
                                            )}
                                        </div>
                                    </div>
                                </div>

                                <div className="px-4 py-3 bg-gray-50 border-t border-gray-100 flex justify-end gap-2">
                                    <button
                                        onClick={() => setShowLabelModal(false)}
                                        className="px-4 py-2 text-sm font-medium text-gray-600 hover:text-gray-800"
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        onClick={handleSaveLabels}
                                        className="px-4 py-2 text-sm font-medium text-white bg-purple-600 rounded-lg hover:bg-purple-700 transition-colors"
                                    >
                                        Done
                                    </button>
                                </div>
                            </div>
                        </div>,
                        document.body
                    )}

                    {/* Call History */}
                    <div>
                        <h3 className="text-sm font-bold text-gray-900 mb-3 flex items-center gap-2">
                            <Phone size={16} /> Call History
                            <span className="text-xs font-normal text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">{calls.length}</span>
                        </h3>

                        <div className="space-y-3">
                            {loading ? (
                                <div className="text-center py-10 text-gray-400">Loading history...</div>
                            ) : calls.length === 0 ? (
                                <div className="text-center py-10 text-gray-400">No calls found</div>
                            ) : (
                                calls.map((call) => (
                                    <div key={call.id} className="bg-white p-3 rounded-xl border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
                                        {/* Call Header */}
                                        <div className="flex justify-between items-start mb-2">
                                            <div className="flex items-center gap-2">
                                                {/* Reviewed Toggle */}
                                                <button
                                                    onClick={() => handleToggleReviewed(call.id, call.reviewed)}
                                                    className={`p-0.5 rounded-full transition-all duration-200 hover:scale-110 ${Number(call.reviewed)
                                                        ? 'text-green-500 hover:text-green-600'
                                                        : 'text-gray-300 hover:text-gray-400'
                                                        }`}
                                                    title={Number(call.reviewed) ? 'Mark as unreviewed' : 'Mark as reviewed'}
                                                >
                                                    {Number(call.reviewed) ? (
                                                        <CheckCircle2 size={18} className="fill-green-100" />
                                                    ) : (
                                                        <Circle size={18} />
                                                    )}
                                                </button>
                                                {getTypeIcon(call.type)}
                                                <span className="text-sm font-medium text-gray-900 capitalize">
                                                    {call.type}
                                                </span>
                                                {call.duration > 0 && (
                                                    <span className="text-xs text-gray-500">
                                                        {call.duration >= 60 ? `${Math.floor(call.duration / 60)}m ${call.duration % 60}s` : `${call.duration}s`}
                                                    </span>
                                                )}
                                            </div>
                                            <span className="text-xs text-gray-500">
                                                {format(new Date(call.call_time + (call.call_time.endsWith('Z') ? '' : 'Z')), 'MMM d, h:mm a')}
                                            </span>
                                        </div>

                                        {/* Call Details */}
                                        <div className="flex items-center gap-3 text-xs text-gray-500 mb-2">
                                            <div className="flex items-center gap-1">
                                                <User size={11} className="text-gray-400" />
                                                {call.employee_name || 'System'} {call.device_phone ? `(${call.device_phone})` : ''}
                                            </div>
                                        </div>

                                        {/* Call Note - Improved UI */}
                                        {editingCallNote === call.id ? (
                                            <div className="bg-blue-50 rounded-lg p-3 border border-blue-200">
                                                <textarea
                                                    className="w-full text-sm p-2.5 border border-blue-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none resize-none bg-white"
                                                    rows={3}
                                                    value={callNoteValue}
                                                    onChange={(e) => setCallNoteValue(e.target.value)}
                                                    placeholder="Add a note about this call..."
                                                    autoFocus
                                                />
                                                <div className="flex justify-end gap-2 mt-2">
                                                    <button
                                                        onClick={() => setEditingCallNote(null)}
                                                        className="text-xs text-gray-500 hover:text-gray-700 px-3 py-1.5 rounded-md hover:bg-white transition-colors"
                                                    >
                                                        Cancel
                                                    </button>
                                                    <button
                                                        onClick={() => handleSaveCallNote(call.id)}
                                                        className="text-xs bg-blue-600 text-white px-4 py-1.5 rounded-md font-medium hover:bg-blue-700 transition-colors shadow-sm"
                                                    >
                                                        Save Note
                                                    </button>
                                                </div>
                                            </div>
                                        ) : call.note ? (
                                            <div
                                                onClick={() => startEditingCallNote(call)}
                                                className="bg-gray-50 rounded-lg p-2.5 border border-gray-100 cursor-pointer group hover:border-blue-200 hover:bg-blue-50/50 transition-all"
                                            >
                                                <div className="flex items-start gap-2">
                                                    <MessageSquare size={14} className="text-blue-500 mt-0.5 flex-shrink-0" />
                                                    <p className="text-sm text-gray-700 flex-1 leading-relaxed">{call.note}</p>
                                                    <Edit3 size={12} className="text-gray-300 group-hover:text-blue-500 transition-colors flex-shrink-0 mt-0.5" />
                                                </div>
                                            </div>
                                        ) : (
                                            <button
                                                onClick={() => startEditingCallNote(call)}
                                                className="w-full text-left px-3 py-2 text-xs text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg border border-dashed border-gray-200 hover:border-blue-300 transition-all flex items-center gap-2"
                                            >
                                                <Plus size={12} />
                                                Add note
                                            </button>
                                        )}

                                        {/* Labels */}
                                        {call.labels && (
                                            <div className="flex flex-wrap gap-1 mt-2">
                                                {call.labels.split(',').filter(Boolean).map(label => (
                                                    <span key={label} className="inline-flex items-center px-1.5 py-0.5 rounded bg-blue-50 text-blue-700 text-[10px] font-medium border border-blue-100">
                                                        <Tag size={10} className="mr-1" />
                                                        {label}
                                                    </span>
                                                ))}
                                            </div>
                                        )}

                                        {/* Recording */}
                                        {call.recording_url && (
                                            <div className="mt-3 pt-2 border-t border-gray-100 flex items-center gap-3">
                                                <button
                                                    onClick={() => playRecording(call)}
                                                    className="flex items-center gap-2 bg-white hover:bg-blue-50 border border-gray-200 hover:border-blue-200 px-3 py-1.5 rounded-full shadow-sm transition-all group/player"
                                                    title="Play Recording"
                                                >
                                                    <div className={`w-6 h-6 rounded-full flex items-center justify-center transition-colors ${currentCall?.id === call.id ? 'bg-blue-600 text-white' : 'bg-blue-100 text-blue-600 group-hover/player:bg-blue-600 group-hover/player:text-white'}`}>
                                                        {currentCall?.id === call.id && isPlaying ? <Pause size={10} fill="currentColor" /> : <Play size={10} fill="currentColor" className={currentCall?.id === call.id ? '' : 'ml-0.5'} />}
                                                    </div>
                                                    <span className={`text-xs font-mono group-hover/player:text-blue-700 ${currentCall?.id === call.id ? 'text-blue-700 font-semibold' : 'text-gray-600'}`}>
                                                        {currentCall?.id === call.id ? (
                                                            `${Math.floor(currentTime / 60)}:${String(Math.floor(currentTime % 60)).padStart(2, '0')}`
                                                        ) : (
                                                            call.duration > 0 ? (
                                                                call.duration >= 60 ? `${Math.floor(call.duration / 60)}:${String(call.duration % 60).padStart(2, '0')}` : `0:${String(call.duration).padStart(2, '0')}`
                                                            ) : '0:00'
                                                        )}
                                                    </span>
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                ))
                            )}
                        </div>

                        {/* Mark All as Reviewed Button */}
                        {calls.length > 0 && calls.some(c => !Number(c.reviewed)) && (
                            <button
                                onClick={handleMarkAllReviewed}
                                className="w-full mt-3 py-2.5 px-4 bg-green-50 hover:bg-green-100 text-green-700 border border-green-200 rounded-lg text-sm font-medium transition-colors flex items-center justify-center gap-2"
                            >
                                <CheckCircle2 size={16} />
                                Mark All as Reviewed
                            </button>
                        )}
                    </div>

                    {/* Delete Confirmation Modal */}
                    {showDeleteConfirm && createPortal(
                        <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-black/30 backdrop-blur-[2px]" onClick={() => setShowDeleteConfirm(false)}>
                            <div
                                className="bg-white rounded-xl shadow-2xl w-full max-w-sm overflow-hidden animate-in fade-in zoom-in-95 duration-150"
                                onClick={e => e.stopPropagation()}
                            >
                                <div className="p-6 text-center">
                                    <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                        <Trash2 size={24} className="text-red-600" />
                                    </div>
                                    <h3 className="text-lg font-bold text-gray-900 mb-2">Delete Person & Data?</h3>
                                    <p className="text-sm text-gray-500 mb-6">This will permanently delete all call history, excluded status, and contact details. This action cannot be undone.</p>
                                    <div className="flex gap-3">
                                        <button
                                            onClick={() => setShowDeleteConfirm(false)}
                                            className="flex-1 px-4 py-2.5 bg-gray-100 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-200 transition-colors"
                                        >
                                            Cancel
                                        </button>
                                        <button
                                            onClick={handleDeletePerson}
                                            disabled={isDeleting}
                                            className="flex-1 px-4 py-2.5 bg-red-600 text-white rounded-lg text-sm font-medium hover:bg-red-700 transition-colors shadow-sm"
                                        >
                                            {isDeleting ? 'Deleting...' : 'Delete'}
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>,
                        document.body
                    )}

                    {/* Bottom Spacing */}
                    <div className="h-6"></div>
                </div>
            </div>
        </div>,
        document.body
    );
}
