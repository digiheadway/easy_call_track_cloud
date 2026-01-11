import { useState, useEffect, useRef, useMemo } from 'react';
import api from '../api/client';
import { format } from 'date-fns';
import { usePersonModal } from '../context/PersonModalContext';
import DateRangeFilter from '../components/DateRangeFilter';
import CustomizeViewModal from '../components/CustomizeViewModal';
import SimpleNoteModal from '../components/SimpleNoteModal';
import { toast } from 'sonner';
import {
    Search,
    Filter,
    ChevronLeft,
    ChevronRight,
    ChevronDown,
    PhoneIncoming,
    PhoneOutgoing,
    PhoneMissed,
    CheckCircle2,
    Circle,
    Tag,
    Plus,
    X,
    Settings,
    Columns,
    ArrowUpDown,
    ArrowUp,
    ArrowDown,
    Save,
    Edit3,
    Clock,
    User,
    Activity,
    Smartphone,
    MoreHorizontal,
    Calendar,
    ArrowUpRight,
    ArrowDownLeft,
    Layers,
    ListFilter,
    BarChart3,
    Hash,
    History as HistoryIcon,
    Flame,
    Zap,
    Play,
    PhoneCall,
    Volume2,
    FileText,
    Percent,
    ArrowRightLeft,
    GripVertical
} from 'lucide-react';
import { useAudioPlayer } from '../context/AudioPlayerContext';

const LabelCell = ({ labels, field, phone, onUpdate }) => {
    const [isAdding, setIsAdding] = useState(false);
    const [inputValue, setInputValue] = useState('');

    const currentLabels = labels ? labels.split(',').filter(Boolean) : [];
    const defaultSuggestions = ['Important', 'Follow Up', 'Resolved', 'Pending', 'Callback', 'Lead', 'VIP', 'Spam', 'Wrong Number', 'Inquiry', 'Complaint', 'Hot Lead'];

    const getLabelColor = (label) => {
        const l = label.toLowerCase();
        if (l.includes('vip') || l.includes('important') || l.includes('hot')) return 'bg-amber-100 text-amber-700 border-amber-200   ';
        if (l.includes('lead')) return 'bg-blue-100 text-blue-700 border-blue-200   ';
        if (l.includes('spam') || l.includes('complaint')) return 'bg-rose-100 text-rose-700 border-rose-200   ';
        if (l.includes('follow')) return 'bg-indigo-100 text-indigo-700 border-indigo-200   ';
        if (l.includes('resolved')) return 'bg-emerald-100 text-emerald-700 border-emerald-200   ';
        return 'bg-slate-100 text-slate-700 border-slate-200   ';
    };

    const handleAdd = (label) => {
        const val = label.trim();
        if (!val) return;
        const updated = [...new Set([...currentLabels, val])].join(',');
        onUpdate(phone, { [field]: updated });
        setInputValue('');
        setIsAdding(false);
    };

    const handleRemove = (label) => {
        const updated = currentLabels.filter(l => l !== label).join(',');
        onUpdate(phone, { [field]: updated });
    };

    return (
        <div className="flex flex-wrap gap-1.5" onClick={e => e.stopPropagation()}>
            {currentLabels.map(label => (
                <span key={label} className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-bold border shadow-sm transition-all ${getLabelColor(label)}`}>
                    {label}
                    <button onClick={() => handleRemove(label)} className="hover:text-red-500 transition-colors"><X size={8} /></button>
                </span>
            ))}
            {isAdding ? (
                <div className="relative">
                    <input
                        autoFocus
                        type="text"
                        value={inputValue}
                        onChange={(e) => setInputValue(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key === 'Enter') handleAdd(inputValue);
                            else if (e.key === 'Escape') setIsAdding(false);
                        }}
                        onBlur={() => setTimeout(() => setIsAdding(false), 200)}
                        className="text-[10px] px-2 py-0.5 border border-blue-400 rounded-lg outline-none w-28 bg-white  shadow-sm"
                        placeholder="Type label..."
                    />
                    <div className="absolute left-0 top-full mt-1 w-40 bg-white  border border-gray-100  rounded-lg shadow-xl z-50 p-1 flex flex-col gap-0.5 max-h-48 overflow-y-auto">
                        {defaultSuggestions
                            .filter(s => s.toLowerCase().includes(inputValue.toLowerCase()) && !currentLabels.includes(s))
                            .map(s => (
                                <button
                                    key={s}
                                    onMouseDown={(e) => { e.preventDefault(); handleAdd(s); }}
                                    className="w-full text-left px-2 py-1.5 text-[10px] hover:bg-blue-50  rounded font-bold text-gray-700 "
                                >
                                    {s}
                                </button>
                            ))}
                        {inputValue.trim() && !defaultSuggestions.some(s => s.toLowerCase() === inputValue.toLowerCase().trim()) && (
                            <button
                                onMouseDown={(e) => { e.preventDefault(); handleAdd(inputValue); }}
                                className="w-full text-left px-2 py-1.5 text-[10px] hover:bg-emerald-50  text-emerald-600  rounded font-bold border-t border-gray-50  mt-1 pt-1"
                            >
                                + Add "{inputValue.trim()}"
                            </button>
                        )}
                    </div>
                </div>
            ) : (
                <button
                    onClick={() => setIsAdding(true)}
                    className="w-5 h-5 rounded-md flex items-center justify-center border border-dashed border-gray-300 text-gray-400 hover:border-blue-400 hover:text-blue-500 bg-white  transition-all shadow-sm"
                >
                    <Plus size={12} />
                </button>
            )}
        </div>
    );
};

const MetricBadge = ({ label, value, icon: Icon, color = 'blue' }) => {
    const colorClasses = {
        blue: 'bg-blue-50 text-blue-700 border-blue-100  ',
        green: 'bg-emerald-50 text-emerald-700 border-emerald-100  ',
        rose: 'bg-rose-50 text-rose-700 border-rose-100  ',
        amber: 'bg-amber-50 text-amber-700 border-amber-100  '
    };
    return (
        <div className={`flex flex-col items-center px-3 py-1.5 rounded-xl border ${colorClasses[color]} min-w-[64px]`}>
            <div className="flex items-center gap-1 opacity-60 mb-0.5">
                {Icon && <Icon size={9} />}
                <span className="text-[8px] font-black uppercase tracking-tighter">{label}</span>
            </div>
            <span className="text-sm font-black leading-none">{value}</span>
        </div>
    );
};

export default function CallersPage() {
    const { openPersonModal } = usePersonModal();
    const { playRecording } = useAudioPlayer();
    const [callers, setCallers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [pagination, setPagination] = useState({ page: 1, limit: 50, total: 0, total_pages: 1 });

    // Filter States
    const [search, setSearch] = useState('');
    const [dateRange, setDateRange] = useState('7days');
    const [customRange, setCustomRange] = useState({ startDate: '', endDate: '' });
    const [sortConfig, setSortConfig] = useState({ key: 'last_call', direction: 'DESC' });
    const [showFilterBar, setShowFilterBar] = useState(false);

    // Filters
    const [connectStatus, setConnectStatus] = useState('all');
    const [noteStatus, setNoteStatus] = useState('all');
    const [reviewStatus, setReviewStatus] = useState('all');
    const [recordingStatus, setRecordingStatus] = useState('all');
    const [durationFilter, setDurationFilter] = useState('all');
    const [interactionFilter, setInteractionFilter] = useState('all');
    const [lastCallType, setLastCallType] = useState('all');
    const [firstCallType, setFirstCallType] = useState('all');
    const [inOutRatioFilter, setInOutRatioFilter] = useState('all');
    const [labelFilter, setLabelFilter] = useState('all');
    const [availableLabels, setAvailableLabels] = useState([]);

    const [showColumnCustomization, setShowColumnCustomization] = useState(false);

    const defaultColumnOrder = ['contact', 'last_call_time', 'person_note', 'label', 'metrics', 'last_call_type', 'first_call_type', 'in_out_ratio', 'recording'];
    const [columnOrder, setColumnOrder] = useState(() => {
        const saved = JSON.parse(localStorage.getItem('callers_column_order'));
        const filtered = saved ? saved.filter(id => defaultColumnOrder.includes(id)) : [];
        return filtered.length > 4 ? [...new Set([...filtered, ...defaultColumnOrder])] : defaultColumnOrder;
    });
    const [visibleColumns, setVisibleColumns] = useState(() => {
        const saved = JSON.parse(localStorage.getItem('callers_visible_columns'));
        const filtered = saved ? saved.filter(id => defaultColumnOrder.includes(id)) : [];
        return filtered.length > 4 ? filtered : defaultColumnOrder;
    });
    const [columnWidths, setColumnWidths] = useState(() => JSON.parse(localStorage.getItem('callers_column_widths')) || {
        contact: 260,
        last_call_time: 140,
        person_note: 160,
        label: 150,
        metrics: 280,
        last_call_type: 140,
        first_call_type: 140,
        in_out_ratio: 140,
        recording: 100
    });

    const [editingNote, setEditingNote] = useState(null);
    const resizingRef = useRef(null);
    const [draggedColumn, setDraggedColumn] = useState(null);

    useEffect(() => {
        fetchLabels();
    }, []);

    useEffect(() => {
        fetchCallers();
    }, [
        search, dateRange, customRange, sortConfig, pagination.page,
        connectStatus, noteStatus, reviewStatus, recordingStatus,
        durationFilter, interactionFilter, lastCallType, firstCallType,
        inOutRatioFilter, labelFilter
    ]);

    useEffect(() => {
        localStorage.setItem('callers_column_order', JSON.stringify(columnOrder));
        localStorage.setItem('callers_visible_columns', JSON.stringify(visibleColumns));
        localStorage.setItem('callers_column_widths', JSON.stringify(columnWidths));
    }, [columnOrder, visibleColumns, columnWidths]);

    const fetchLabels = async () => {
        try {
            const res = await api.get('/contacts.php?action=labels');
            if (res.data && Array.isArray(res.data.data)) {
                setAvailableLabels(res.data.data);
            }
        } catch (err) {
            console.error("Fetch labels error", err);
        }
    };

    const fetchCallers = async () => {
        if (dateRange === 'custom' && (!customRange.startDate || !customRange.endDate)) return;
        setLoading(true);
        try {
            const params = new URLSearchParams({
                action: 'callers',
                search,
                dateRange,
                connectStatus,
                noteStatus,
                reviewStatus,
                recordingStatus,
                durationFilter,
                interactionFilter,
                lastCallType,
                firstCallType,
                inOutRatioFilter,
                label: labelFilter,
                sortBy: sortConfig.key,
                sortOrder: sortConfig.direction,
                tzOffset: new Date().getTimezoneOffset(),
                page: pagination.page,
                limit: 50
            });
            if (dateRange === 'custom') {
                params.append('startDate', customRange.startDate);
                params.append('endDate', customRange.endDate);
            }
            const res = await api.get(`/contacts.php?${params.toString()}`);
            if (res.data && res.data.data) {
                const payload = res.data;
                setCallers(payload.data || []);
                setPagination(payload.pagination || { page: 1, limit: 50, total: 0, total_pages: 1 });
            }
        } catch (err) {
            console.error("Fetch callers error", err);
            toast.error('Failed to load callers');
        } finally {
            setLoading(false);
        }
    };

    const handleUpdateCaller = async (phone, data) => {
        try {
            const res = await api.post('/contacts.php', { phone, ...data });
            if (res.data.status) {
                setCallers(prev => prev.map(c => c.phone === phone ? { ...c, ...data } : c));
                toast.success('Updated successfully');
            }
        } catch (err) {
            toast.error('Update failed');
        }
    };

    const handleSort = (key) => {
        setSortConfig(prev => ({
            key,
            direction: prev.key === key && prev.direction === 'DESC' ? 'ASC' : 'DESC'
        }));
    };

    const getSortIcon = (key) => {
        if (sortConfig.key !== key) return <ArrowUpDown size={12} className="opacity-30" />;
        return sortConfig.direction === 'ASC' ? <ArrowUp size={12} className="text-blue-600" /> : <ArrowDown size={12} className="text-blue-600" />;
    };

    const handleResizeStart = (e, columnId) => {
        e.preventDefault();
        e.stopPropagation();
        const startX = e.pageX;
        const startWidth = columnWidths[columnId] || 150;

        resizingRef.current = { columnId, startX, startWidth };
        document.body.style.cursor = 'col-resize';
        document.body.style.userSelect = 'none';

        const handleMouseMove = (moveEvent) => {
            if (!resizingRef.current) return;
            const { columnId, startX, startWidth } = resizingRef.current;
            const diff = moveEvent.pageX - startX;
            const newWidth = Math.max(80, startWidth + diff);

            setColumnWidths(prev => ({
                ...prev,
                [columnId]: newWidth
            }));
        };

        const handleMouseUp = () => {
            resizingRef.current = null;
            document.body.style.cursor = '';
            document.body.style.userSelect = '';
            window.removeEventListener('mousemove', handleMouseMove);
            window.removeEventListener('mouseup', handleMouseUp);
        };

        window.addEventListener('mousemove', handleMouseMove);
        window.addEventListener('mouseup', handleMouseUp);
    };

    const handleColumnDragStart = (e, id) => {
        setDraggedColumn(id);
        e.dataTransfer.setData('text/plain', id);
    };

    const handleColumnDragOver = (e, id) => {
        e.preventDefault();
        if (draggedColumn === id) return;
        const newOrder = [...columnOrder];
        const oldIndex = newOrder.indexOf(draggedColumn);
        const newIndex = newOrder.indexOf(id);
        if (oldIndex === -1 || newIndex === -1) return;
        newOrder.splice(oldIndex, 1);
        newOrder.splice(newIndex, 0, draggedColumn);
        setColumnOrder(newOrder);
    };

    const handleColumnDragEnd = () => setDraggedColumn(null);

    const toggleColumn = (id) => {
        setVisibleColumns(prev => prev.includes(id) ? prev.filter(c => c !== id) : [...prev, id]);
    };

    const clearAllFilters = () => {
        setSearch('');
        setConnectStatus('all');
        setNoteStatus('all');
        setReviewStatus('all');
        setRecordingStatus('all');
        setDurationFilter('all');
        setInteractionFilter('all');
        setLastCallType('all');
        setFirstCallType('all');
        setInOutRatioFilter('all');
        setLabelFilter('all');
        setDateRange('7days');
    };

    const activeFilterCount = useMemo(() => {
        let count = 0;
        if (connectStatus !== 'all') count++;
        if (noteStatus !== 'all') count++;
        if (reviewStatus !== 'all') count++;
        if (recordingStatus !== 'all') count++;
        if (durationFilter !== 'all') count++;
        if (interactionFilter !== 'all') count++;
        if (lastCallType !== 'all') count++;
        if (firstCallType !== 'all') count++;
        if (inOutRatioFilter !== 'all') count++;
        if (labelFilter !== 'all') count++;
        return count;
    }, [connectStatus, noteStatus, reviewStatus, recordingStatus, durationFilter, interactionFilter, lastCallType, firstCallType, inOutRatioFilter, labelFilter]);

    const formatDuration = (sec) => {
        if (!sec) return '0s';
        const h = Math.floor(sec / 3600);
        const m = Math.floor((sec % 3600) / 60);
        const s = Math.floor(sec % 60);
        if (h > 0) return `${h}h ${m}m`;
        if (m > 0) return `${m}m ${s}s`;
        return `${s}s`;
    };

    const columnLabels = {
        contact: 'Contact',
        last_call_time: 'Last Call Time',
        person_note: 'Person Note',
        label: 'Label',
        metrics: 'Interactions Stats',
        last_call_type: 'Last Type',
        first_call_type: 'First Type',
        in_out_ratio: 'In/Out Ratio',
        recording: 'Recording'
    };

    const filterLabels = {
        connectivity: 'Connectivity',
        duration: 'Duration',
        interactions: 'Interactions',
        notes: 'Notes',
        review: 'Review Status',
        last_call_type: 'Last Type',
        first_call_type: 'First Type',
        ratio: 'In/Out Ratio',
        recordings: 'Recordings',
        label: 'Label'
    };

    return (
        <div className="relative pb-20">
            <div className={`space-y-6 ${loading && callers.length === 0 ? 'opacity-50' : ''}`}>

                {/* Header Row */}
                <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6">
                    <DateRangeFilter
                        value={dateRange}
                        onChange={setDateRange}
                        customRange={customRange}
                        onCustomRangeChange={setCustomRange}
                    />

                    <div className="flex items-center gap-3 w-full lg:w-auto ml-auto font-medium">
                        <div className="flex items-center bg-white  border border-gray-200  rounded-xl p-1 shadow-sm w-full lg:w-auto">
                            <div className="relative flex-1 lg:flex-initial">
                                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                                <input
                                    type="text"
                                    placeholder="Search callers..."
                                    value={search}
                                    onChange={(e) => { setSearch(e.target.value); setPagination(prev => ({ ...prev, page: 1 })); }}
                                    className="pl-9 pr-4 py-1.5 text-sm bg-transparent border-none outline-none w-full lg:w-56"
                                />
                            </div>
                            <div className="w-px h-5 bg-gray-200  mx-1.5"></div>
                            <button
                                onClick={() => setShowFilterBar(!showFilterBar)}
                                className={`relative p-2 rounded-lg transition-all ${showFilterBar ? 'bg-blue-600 text-white shadow-lg shadow-blue-200' : 'text-gray-400 hover:bg-gray-50'}`}
                            >
                                <Filter size={16} />
                                {activeFilterCount > 0 && (
                                    <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-red-500 text-white text-[9px] flex items-center justify-center font-bold">
                                        {activeFilterCount}
                                    </span>
                                )}
                            </button>
                        </div>
                        <button
                            onClick={() => setShowColumnCustomization(true)}
                            className="p-2 bg-white  border border-gray-200  rounded-xl text-gray-400 shadow-sm hover:text-blue-600 transition-all"
                        >
                            <Columns size={16} />
                        </button>
                    </div>
                </div>

                {/* Filter Bar */}
                {showFilterBar && (
                    <div className="bg-white  rounded-2xl border border-gray-100  shadow-md p-5 animate-in slide-in-from-top-2 duration-200">
                        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-4">
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest pl-1">Connectivity</label>
                                <select value={connectStatus} onChange={(e) => setConnectStatus(e.target.value)} className="w-full text-xs rounded-lg py-2 px-3 border border-gray-200  bg-gray-50  font-bold outline-none">
                                    <option value="all">All Connections</option>
                                    <option value="connected">Has Connected</option>
                                    <option value="never_connected">Never Connected</option>
                                </select>
                            </div>
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest pl-1">Total Duration</label>
                                <select value={durationFilter} onChange={(e) => setDurationFilter(e.target.value)} className="w-full text-xs rounded-lg py-2 px-3 border border-gray-200  bg-gray-50  font-bold outline-none">
                                    <option value="all">Any Duration</option>
                                    <option value="short">Short (&lt; 1m)</option>
                                    <option value="medium">Medium (1-10m)</option>
                                    <option value="long">Long (&gt; 10m)</option>
                                </select>
                            </div>
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest pl-1">Interactions</label>
                                <select value={interactionFilter} onChange={(e) => setInteractionFilter(e.target.value)} className="w-full text-xs rounded-lg py-2 px-3 border border-gray-200  bg-gray-50  font-bold outline-none">
                                    <option value="all">Any Interactions</option>
                                    <option value="frequent">Frequent (&gt; 10)</option>
                                    <option value="rare">Rare (1-3)</option>
                                </select>
                            </div>
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest pl-1">Notes Status</label>
                                <select value={noteStatus} onChange={(e) => setNoteStatus(e.target.value)} className="w-full text-xs rounded-lg py-2 px-3 border border-gray-200  bg-gray-50  font-bold outline-none">
                                    <option value="all">Any Note Status</option>
                                    <option value="has_note">With Notes</option>
                                    <option value="no_note">Without Notes</option>
                                </select>
                            </div>
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest pl-1">Process Status</label>
                                <select value={reviewStatus} onChange={(e) => setReviewStatus(e.target.value)} className="w-full text-xs rounded-lg py-2 px-3 border border-gray-200  bg-gray-50  font-bold outline-none">
                                    <option value="all">All Review Status</option>
                                    <option value="all_reviewed">Fully Reviewed</option>
                                    <option value="pending">Pending Review</option>
                                </select>
                            </div>
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest pl-1">Last Call Type</label>
                                <select value={lastCallType} onChange={(e) => setLastCallType(e.target.value)} className="w-full text-xs rounded-lg py-2 px-3 border border-gray-200  bg-gray-50  font-bold outline-none">
                                    <option value="all">Any Type</option>
                                    <option value="inbound">Inbound</option>
                                    <option value="outbound">Outbound</option>
                                </select>
                            </div>
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest pl-1">First Call Type</label>
                                <select value={firstCallType} onChange={(e) => setFirstCallType(e.target.value)} className="w-full text-xs rounded-lg py-2 px-3 border border-gray-200  bg-gray-50  font-bold outline-none">
                                    <option value="all">Any Type</option>
                                    <option value="inbound">Inbound</option>
                                    <option value="outbound">Outbound</option>
                                </select>
                            </div>
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest pl-1">In/Out Ratio</label>
                                <select value={inOutRatioFilter} onChange={(e) => setInOutRatioFilter(e.target.value)} className="w-full text-xs rounded-lg py-2 px-3 border border-gray-200  bg-gray-50  font-bold outline-none">
                                    <option value="all">Any Ratio</option>
                                    <option value="more_in">More Inbound</option>
                                    <option value="more_out">More Outbound</option>
                                    <option value="equal">Equal</option>
                                </select>
                            </div>
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest pl-1">Recordings</label>
                                <select value={recordingStatus} onChange={(e) => setRecordingStatus(e.target.value)} className="w-full text-xs rounded-lg py-2 px-3 border border-gray-200  bg-gray-50  font-bold outline-none">
                                    <option value="all">Any Status</option>
                                    <option value="has_recordings">Available</option>
                                    <option value="no_recordings">None</option>
                                </select>
                            </div>
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest pl-1">Labels</label>
                                <select value={labelFilter} onChange={(e) => setLabelFilter(e.target.value)} className="w-full text-xs rounded-lg py-2 px-3 border border-gray-200  bg-gray-50  font-bold outline-none">
                                    <option value="all">All Labels</option>
                                    {availableLabels.map(l => (
                                        <option key={l.label} value={l.label}>{l.label}</option>
                                    ))}
                                </select>
                            </div>
                            <div className="flex items-end pb-0.5">
                                <button onClick={clearAllFilters} className="text-[10px] font-black text-red-500 uppercase tracking-widest flex items-center gap-1.5 hover:bg-red-50 px-3 py-2 rounded-lg transition-colors bg-red-50/20">
                                    <X size={14} /> Reset Filters
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {/* Table Container */}
                <div className="card !p-0 overflow-hidden bg-white  border-gray-200  shadow-sm transition-all duration-300">
                    <div className="overflow-x-auto">
                        <table className="w-full text-left table-fixed border-separate border-spacing-0">
                            <thead className="bg-[#f8fafc]  border-b border-gray-100  text-gray-500  font-bold uppercase tracking-widest text-[10px] whitespace-nowrap sticky top-0 z-20 shadow-sm">
                                <tr>
                                    {columnOrder.filter(id => visibleColumns.includes(id)).map(id => {
                                        const width = columnWidths[id] || 150;
                                        return (
                                            <th
                                                key={id}
                                                draggable
                                                onDragStart={(e) => handleColumnDragStart(e, id)}
                                                onDragOver={(e) => handleColumnDragOver(e, id)}
                                                onDragEnd={handleColumnDragEnd}
                                                className={`relative px-6 py-4 cursor-pointer hover:bg-gray-100/50  transition-colors group select-none ${draggedColumn === id ? 'opacity-40 bg-gray-50' : ''}`}
                                                style={{ width: `${width}px`, minWidth: `${width}px`, maxWidth: `${width}px` }}
                                                onClick={() => {
                                                    if (id === 'contact') handleSort('name');
                                                    else if (id === 'metrics') handleSort('total_calls');
                                                    else if (id === 'last_call_time') handleSort('last_call');
                                                    else if (id === 'in_out_ratio') handleSort('in_out_ratio');
                                                    else if (id === 'total_duration') handleSort('total_duration');
                                                }}
                                            >
                                                <div className="flex items-center gap-2 truncate">
                                                    {id === 'contact' && <User size={12} className="shrink-0 text-blue-500" />}
                                                    {id === 'label' && <Tag size={12} className="shrink-0 text-amber-500" />}
                                                    {id === 'metrics' && <Activity size={12} className="shrink-0 text-emerald-500" />}
                                                    {id === 'last_call_time' && <Clock size={12} className="shrink-0 text-purple-500" />}
                                                    {id === 'last_call_type' && <PhoneOutgoing size={12} className="shrink-0 text-blue-500" />}
                                                    {id === 'first_call_type' && <PhoneIncoming size={12} className="shrink-0 text-emerald-500" />}
                                                    {id === 'recording' && <Volume2 size={12} className="shrink-0 text-rose-500" />}
                                                    {id === 'person_note' && <FileText size={12} className="shrink-0 text-slate-500" />}
                                                    {id === 'in_out_ratio' && <Percent size={12} className="shrink-0 text-indigo-500" />}
                                                    {columnLabels[id]}
                                                    {(id === 'contact' || id === 'metrics' || id === 'last_call_time' || id === 'in_out_ratio') && getSortIcon(id === 'contact' ? 'name' : (id === 'metrics' ? 'total_calls' : (id === 'last_call_time' ? 'last_call' : 'in_out_ratio')))}
                                                </div>
                                                <div
                                                    className="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-blue-400 z-10 transition-colors"
                                                    onMouseDown={(e) => handleResizeStart(e, id)}
                                                    onClick={(e) => e.stopPropagation()}
                                                />
                                            </th>
                                        );
                                    })}
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-50 ">
                                {callers.length === 0 && !loading ? (
                                    <tr><td colSpan={visibleColumns.length} className="px-6 py-24 text-center text-gray-400 font-bold uppercase tracking-widest text-xs bg-gray-50/50 ">No callers found match these filters</td></tr>
                                ) : callers.map((caller) => (
                                    <tr
                                        key={caller.phone}
                                        onClick={() => openPersonModal({ phone_number: caller.phone, contact_name: caller.name, person_labels: caller.label, person_note: caller.notes })}
                                        className="hover:bg-blue-50/40  transition-all cursor-pointer group"
                                    >
                                        {columnOrder.filter(id => visibleColumns.includes(id)).map(id => {
                                            const width = columnWidths[id] || 150;
                                            const cellProps = {
                                                key: `${caller.phone}-${id}`,
                                                className: "px-6 py-4",
                                                style: { width: `${width}px`, minWidth: `${width}px`, maxWidth: `${width}px` }
                                            };

                                            if (id === 'contact') return (
                                                <td {...cellProps}>
                                                    <div className="flex items-center gap-3">
                                                        <div className="relative">
                                                            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-slate-700 to-slate-900 text-white flex items-center justify-center font-black shadow-md text-sm border-2 border-white  ring-1 ring-slate-200 ">
                                                                {caller.name ? caller.name.charAt(0).toUpperCase() : <User size={16} />}
                                                            </div>
                                                            {Number(caller.unreviewed_count) === 0 && (
                                                                <div className="absolute -top-1 -right-1 bg-emerald-500 text-white p-0.5 rounded-full shadow-sm ring-2 ring-white ">
                                                                    <CheckCircle2 size={10} />
                                                                </div>
                                                            )}
                                                        </div>
                                                        <div className="min-w-0">
                                                            <div className="font-bold text-gray-900  truncate text-sm tracking-tight group-hover:text-blue-600 transition-colors">{caller.name || 'Unknown'}</div>
                                                            <div className="text-[10px] font-mono text-gray-400 flex items-center gap-1 mt-0.5"><Smartphone size={8} />{caller.phone}</div>
                                                        </div>
                                                    </div>
                                                </td>
                                            );
                                            if (id === 'last_call_time') return (
                                                <td {...cellProps}>
                                                    <div className="flex flex-col gap-0.5">
                                                        <div className="text-xs font-black text-gray-900  uppercase">
                                                            {format(new Date(caller.last_call + (caller.last_call.endsWith('Z') ? '' : 'Z')), 'p')}
                                                        </div>
                                                        <div className="text-[10px] text-gray-400 font-bold uppercase tracking-tight">
                                                            {format(new Date(caller.last_call + (caller.last_call.endsWith('Z') ? '' : 'Z')), 'd MMM')}
                                                        </div>
                                                    </div>
                                                </td>
                                            );
                                            if (id === 'person_note') return (
                                                <td {...cellProps} onClick={(e) => { e.stopPropagation(); setEditingNote({ phone: caller.phone, notes: caller.notes, name: caller.name }); }}>
                                                    {caller.notes ? (
                                                        <span className="text-gray-600  block truncate max-w-[140px] text-[11px] font-medium leading-relaxed bg-gray-50  px-2 py-1 rounded border border-gray-100  italic" title={caller.notes}>"{caller.notes}"</span>
                                                    ) : (
                                                        <button className="text-[10px] text-gray-300 hover:text-blue-500 flex items-center gap-1 font-bold uppercase transition-colors opacity-0 group-hover:opacity-100"><Plus size={10} /> Add Note</button>
                                                    )}
                                                </td>
                                            );
                                            if (id === 'label') return (
                                                <td {...cellProps}>
                                                    <LabelCell labels={caller.label} field="label" phone={caller.phone} onUpdate={handleUpdateCaller} />
                                                </td>
                                            );
                                            if (id === 'metrics') return (
                                                <td {...cellProps}>
                                                    <div className="flex items-center gap-2 xl:gap-3">
                                                        <MetricBadge label="Calls" value={caller.total_calls} icon={Hash} color="blue" />
                                                        <MetricBadge label="Duration" value={formatDuration(caller.total_duration)} icon={BarChart3} color="green" />
                                                        <div className="flex flex-col items-start min-w-[50px] gap-1">
                                                            <div className="flex items-center gap-1.5 text-[9px] font-black text-emerald-600 uppercase bg-emerald-50  px-1.5 py-0.5 rounded-md">
                                                                <ArrowUpRight size={10} /> {caller.connected_calls}
                                                            </div>
                                                            <div className="flex items-center gap-1.5 text-[9px] font-black text-rose-500 uppercase bg-rose-50  px-1.5 py-0.5 rounded-md">
                                                                <ArrowDownLeft size={10} /> {caller.missed_calls}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </td>
                                            );
                                            if (id === 'last_call_type') return (
                                                <td {...cellProps}>
                                                    <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg border text-[10px] font-black uppercase tracking-wider ${caller.last_call_type?.toLowerCase().includes('in')
                                                        ? 'bg-emerald-50 text-emerald-600 border-emerald-100  '
                                                        : 'bg-blue-50 text-blue-600 border-blue-100  '}`}>
                                                        {caller.last_call_type?.toLowerCase().includes('in') ? <PhoneIncoming size={10} /> : <PhoneOutgoing size={10} />}
                                                        {caller.last_call_type?.toLowerCase().includes('in') ? 'Inbound' : 'Outbound'}
                                                    </span>
                                                </td>
                                            );
                                            if (id === 'first_call_type') return (
                                                <td {...cellProps}>
                                                    <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg border text-[10px] font-black uppercase tracking-wider ${caller.first_call_type?.toLowerCase().includes('in')
                                                        ? 'bg-emerald-50 text-emerald-600 border-emerald-100  '
                                                        : 'bg-blue-50 text-blue-600 border-blue-100  '}`}>
                                                        {caller.first_call_type?.toLowerCase().includes('in') ? <PhoneIncoming size={10} /> : <PhoneOutgoing size={10} />}
                                                        {caller.first_call_type?.toLowerCase().includes('in') ? 'Inbound' : 'Outbound'}
                                                    </span>
                                                </td>
                                            );
                                            if (id === 'in_out_ratio') return (
                                                <td {...cellProps}>
                                                    <div className="flex items-center gap-3">
                                                        <div className="w-10 h-1.5 bg-gray-100  rounded-full overflow-hidden flex">
                                                            <div className="h-full bg-emerald-500" style={{ width: `${Math.min(100, (Number(caller.in_out_ratio) || 0) * 50)}%` }} />
                                                            <div className="h-full bg-blue-500" style={{ width: `${Math.min(100, 100 - (Number(caller.in_out_ratio) || 0) * 50)}%` }} />
                                                        </div>
                                                        <span className="text-[10px] font-black text-gray-500">{Number(caller.in_out_ratio || 0).toFixed(1)}x</span>
                                                    </div>
                                                </td>
                                            );
                                            if (id === 'recording') return (
                                                <td {...cellProps}>
                                                    {Number(caller.recordings_count) > 0 ? (
                                                        <button
                                                            onClick={(e) => { e.stopPropagation(); toast.info("See call history for recordings"); }}
                                                            className="w-10 h-10 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center hover:bg-rose-100 transition-colors shadow-sm border border-rose-100 shadow-rose-100 flex-col"
                                                        >
                                                            <Volume2 size={16} />
                                                            <span className="text-[8px] font-black leading-none mt-0.5">{caller.recordings_count}</span>
                                                        </button>
                                                    ) : (
                                                        <div className="w-10 h-10 rounded-xl border border-dashed border-gray-200 flex items-center justify-center text-gray-300">
                                                            <Volume2 size={16} />
                                                        </div>
                                                    )}
                                                </td>
                                            );
                                            return null;
                                        })}
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    {/* Pagination */}
                    <div className="flex items-center justify-between p-5 border-t border-gray-100  bg-gray-50/50  backdrop-blur-md">
                        <div className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] flex items-center gap-4">
                            <span>{pagination.total} unique contacts</span>
                            {loading && <div className="w-1.5 h-1.5 bg-blue-600 rounded-full animate-ping" />}
                        </div>
                        <div className="flex items-center gap-3 font-bold">
                            <button onClick={() => setPagination(prev => ({ ...prev, page: prev.page - 1 }))} disabled={pagination.page <= 1} className="p-2 border border-gray-200  rounded-xl disabled:opacity-30 bg-white  hover:bg-gray-50  transition-colors shadow-sm">
                                <ChevronLeft size={16} />
                            </button>
                            <span className="text-xs font-black bg-white  px-4 py-2 rounded-xl border border-gray-100  shadow-sm">PAGE {pagination.page} / {pagination.total_pages}</span>
                            <button onClick={() => setPagination(prev => ({ ...prev, page: prev.page + 1 }))} disabled={pagination.page >= pagination.total_pages} className="p-2 border border-gray-200  rounded-xl disabled:opacity-30 bg-white  hover:bg-gray-50  transition-colors shadow-sm">
                                <ChevronRight size={16} />
                            </button>
                        </div>
                    </div>
                </div>

                {/* Modals */}
                {showColumnCustomization && (
                    <CustomizeViewModal
                        onClose={() => setShowColumnCustomization(false)}
                        initialTab="columns"
                        hideTabs={true}
                        columnOrder={columnOrder}
                        setColumnOrder={setColumnOrder}
                        visibleColumns={visibleColumns}
                        setVisibleColumns={setVisibleColumns}
                        defaultColumnOrder={defaultColumnOrder}
                        columnLabels={columnLabels}
                        toggleColumn={toggleColumn}
                        handleColumnDragStart={handleColumnDragStart}
                        handleColumnDragOver={handleColumnDragOver}
                        handleColumnDragEnd={handleColumnDragEnd}
                        draggedColumn={draggedColumn}
                    />
                )}

                {editingNote && (
                    <SimpleNoteModal
                        title={editingNote.name || 'Unknown'}
                        initialValue={editingNote.notes}
                        onClose={() => setEditingNote(null)}
                        onSave={(v) => handleUpdateCaller(editingNote.phone, { notes: v })}
                        label="Person Note"
                    />
                )}
            </div>
        </div>
    );
}
