import { useState, useEffect, useRef, useMemo } from 'react';
import { useLocation } from 'react-router-dom';
import { createPortal } from 'react-dom';
import api from '../api/client';
import { format } from 'date-fns';
import { usePersonModal } from '../context/PersonModalContext';
import DateRangeFilter from '../components/DateRangeFilter';
import EmployeeDropdown from '../components/EmployeeDropdown';
import { Toaster, toast } from 'sonner';
import {
    Search,
    Filter,
    ChevronLeft,
    ChevronRight,
    ChevronDown,
    PhoneIncoming,
    PhoneOutgoing,
    PhoneMissed,
    ExternalLink,
    CheckCircle2,
    Circle,
    Tag,
    Plus,
    X,
    Play,
    Pause,
    Heart,
    Undo2,
    Settings,
    Columns,
    ArrowUpDown,
    ArrowUp,
    ArrowDown,
    Save,
    Trash2,
    ListFilter,
    Layers,
    GripVertical,
    Eye,
    Edit3,
    AlertTriangle,
    Clock,
    User,
    Activity,
    PlayCircle,
    Briefcase,
    Smartphone,
    EyeOff
} from 'lucide-react';

import Modal from '../components/Modal';

import { useAudioPlayer } from '../context/AudioPlayerContext';

const LabelCell = ({ call, onUpdate }) => {
    const [showModal, setShowModal] = useState(false);
    const [labelSearch, setLabelSearch] = useState('');
    const [pendingLabels, setPendingLabels] = useState([]);
    const [suggestions, setSuggestions] = useState([]);

    const defaultSuggestions = [
        'Important', 'Follow Up', 'Resolved', 'Pending', 'Callback',
        'Wrong Number', 'Inquiry', 'Complaint', 'Order', 'Hot Lead'
    ];
    const [isAdding, setIsAdding] = useState(false);
    const [inputValue, setInputValue] = useState('');

    const currentLabels = call.labels ? call.labels.split(',').filter(Boolean) : [];

    const getLabelColor = (label) => {
        const l = label.toLowerCase();
        if (l.includes('important')) return 'bg-rose-100 text-rose-700 border-rose-200';
        if (l.includes('follow')) return 'bg-amber-100 text-amber-700 border-amber-200';
        if (l.includes('resolved')) return 'bg-emerald-100 text-emerald-700 border-emerald-200';
        if (l.includes('callback')) return 'bg-indigo-100 text-indigo-700 border-indigo-200';
        if (l.includes('lead')) return 'bg-blue-100 text-blue-700 border-blue-200';
        if (l.includes('hot')) return 'bg-orange-100 text-orange-700 border-orange-200';
        return 'bg-slate-100 text-slate-700 border-slate-200';
    };

    const openModal = async () => {
        setPendingLabels([...currentLabels]);
        setLabelSearch('');
        setShowModal(true);
        // Fetch suggestions
        try {
            const res = await api.get('/calls.php?action=labels');
            if (res.data?.data && Array.isArray(res.data.data)) {
                const apiLabels = res.data.data.map(l => l.label || l.labels).filter(Boolean);
                const allLabels = [...new Set([...defaultSuggestions, ...apiLabels])];
                setSuggestions(allLabels);
            } else {
                setSuggestions(defaultSuggestions);
            }
        } catch {
            setSuggestions(defaultSuggestions);
        }
    };

    const toggleLabel = (label) => {
        if (pendingLabels.includes(label)) {
            setPendingLabels(pendingLabels.filter(l => l !== label));
        } else {
            setPendingLabels([...pendingLabels, label]);
        }
    };

    const addCustomLabel = () => {
        if (!labelSearch.trim()) return;
        const label = labelSearch.trim();
        if (!pendingLabels.includes(label)) {
            setPendingLabels([...pendingLabels, label]);
        }
        setLabelSearch('');
    };

    const saveLabels = () => {
        onUpdate(call.id, pendingLabels.join(','), 'set');
        setShowModal(false);
    };

    const filteredSuggestions = suggestions.filter(label =>
        label.toLowerCase().includes(labelSearch.toLowerCase())
    );

    return (
        <>
            <div className="flex flex-wrap gap-1.5 max-w-[200px]" onClick={e => e.stopPropagation()}>
                {currentLabels.map(label => (
                    <span key={label} className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold border shadow-sm transition-all ${getLabelColor(label)}`}>
                        {label}
                        <button
                            onClick={(e) => { e.stopPropagation(); onUpdate(call.id, call.labels, 'remove', label); }}
                            className="hover:text-red-500"
                        >
                            <X size={8} />
                        </button>
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
                                if (e.key === 'Enter') {
                                    const val = inputValue.trim();
                                    if (val) {
                                        onUpdate(call.id, [...currentLabels, val].join(','), 'set');
                                    }
                                    setInputValue('');
                                    setIsAdding(false);
                                } else if (e.key === 'Escape') {
                                    setIsAdding(false);
                                    setInputValue('');
                                }
                            }}
                            onBlur={() => {
                                setTimeout(() => {
                                    setIsAdding(false);
                                    setInputValue('');
                                }, 200);
                            }}
                            placeholder="Type label..."
                            className="text-[11px] px-2 py-0.5 border border-blue-400 rounded-lg outline-none w-24 bg-white shadow-sm"
                        />
                        <div className="absolute left-0 top-full mt-1 w-48 bg-white border border-gray-100 rounded-lg shadow-xl z-50 max-h-72 overflow-y-auto p-1 flex flex-col gap-0.5">
                            {defaultSuggestions
                                .filter(s => s.toLowerCase().includes(inputValue.toLowerCase()) && !currentLabels.includes(s))
                                .map(s => (
                                    <button
                                        key={s}
                                        onMouseDown={(e) => {
                                            e.preventDefault();
                                            onUpdate(call.id, [...currentLabels, s].join(','), 'set');
                                            setInputValue('');
                                            setIsAdding(false);
                                        }}
                                        className="w-full text-left px-2 py-1.5 text-[11px] hover:bg-blue-50 rounded transition-colors font-semibold text-gray-700 whitespace-nowrap"
                                    >
                                        {s}
                                    </button>
                                ))}
                            {inputValue.trim() && !defaultSuggestions.some(s => s.toLowerCase() === inputValue.toLowerCase()) && (
                                <button
                                    onMouseDown={(e) => {
                                        e.preventDefault();
                                        onUpdate(call.id, [...currentLabels, inputValue.trim()].join(','), 'set');
                                        setInputValue('');
                                        setIsAdding(false);
                                    }}
                                    className="w-full text-left px-2 py-1.5 text-[11px] hover:bg-green-50 text-green-700 rounded transition-colors font-bold border-t border-gray-50 mt-1 pt-1"
                                >
                                    + Add "{inputValue.trim()}"
                                </button>
                            )}
                            {inputValue === '' && defaultSuggestions.every(s => currentLabels.includes(s)) && (
                                <div className="p-2 text-[10px] text-gray-400 italic text-center">All suggests used</div>
                            )}
                        </div>
                    </div>
                ) : (
                    <button
                        onClick={(e) => { e.stopPropagation(); setIsAdding(true); }}
                        className="w-6 h-6 rounded-lg flex items-center justify-center border border-dashed border-gray-300 text-gray-400 hover:border-blue-400 hover:text-blue-500 transition-all bg-white hover:bg-blue-50/50 shadow-sm"
                        title="Add Label"
                    >
                        <Plus size={14} />
                    </button>
                )}
            </div>


        </>
    );
};


const CallTypeBadge = ({ type, duration, showDuration = true }) => {
    const t = type?.toLowerCase() || '';
    let icon = <PhoneIncoming size={14} />;
    let colors = 'bg-emerald-50 text-emerald-700 border-emerald-100';
    let label = type;

    if (t.includes('missed')) {
        icon = <PhoneMissed size={14} />;
        colors = 'bg-rose-50 text-rose-700 border-rose-100';
    } else if (t.includes('outgoing') || t.includes('out')) {
        icon = <PhoneOutgoing size={14} />;
        colors = 'bg-blue-50 text-blue-700 border-blue-100';
    } else if (t.includes('reject') || t.includes('block')) {
        icon = <PhoneMissed size={14} className="rotate-45" />;
        colors = 'bg-amber-50 text-amber-700 border-amber-100';
    }

    return (
        <div className="flex flex-col items-start">
            <span className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border ${colors} font-medium text-xs uppercase tracking-wider shadow-sm transition-transform hover:scale-105`}>
                {icon}
                <div className="flex items-center gap-2">
                    <span>{label}</span>
                    {duration > 0 && showDuration && (
                        <>
                            <span className="w-1 h-1 rounded-full bg-current opacity-30" />
                            <span className="font-semibold opacity-80">
                                {duration >= 60 ? `${Math.floor(duration / 60)}m ${duration % 60}s` : `${duration}s`}
                            </span>
                        </>
                    )}
                </div>
            </span>
        </div>
    );
};

export default function CallsPage() {
    const location = useLocation();
    const { openPersonModal } = usePersonModal();
    const { playRecording, currentCall, isPlaying, currentTime, duration: activeDuration } = useAudioPlayer();
    const [calls, setCalls] = useState([]);
    const [pagination, setPagination] = useState({ page: 1, limit: 20, total: 0, total_pages: 1 });
    const [loading, setLoading] = useState(true);

    // Filters (persisted)
    const [search, setSearch] = useState('');
    const [direction, setDirection] = useState(() => {
        return localStorage.getItem('calls_filter_direction') || 'all';
    });
    const [reviewedFilter, setReviewedFilter] = useState(() => {
        return localStorage.getItem('calls_filter_reviewed') || 'all';
    });
    const [dateRange, setDateRange] = useState(() => {
        return localStorage.getItem('calls_filter_dateRange') || '7days';
    });
    const [customRange, setCustomRange] = useState(() => {
        const saved = localStorage.getItem('calls_filter_customRange');
        return saved ? JSON.parse(saved) : { startDate: '', endDate: '' };
    });
    const [selectedEmployee, setSelectedEmployee] = useState(() => {
        return localStorage.getItem('calls_filter_employee') || '';
    });
    const [employees, setEmployees] = useState([]);

    // Additional Filters (persisted)
    const [connectedFilter, setConnectedFilter] = useState(() => {
        return localStorage.getItem('calls_filter_connected') || 'all';
    });
    const [noteFilter, setNoteFilter] = useState(() => {
        return localStorage.getItem('calls_filter_note') || 'all';
    });
    const [recordingFilter, setRecordingFilter] = useState(() => {
        return localStorage.getItem('calls_filter_recording') || 'all';
    });
    const [durationFilter, setDurationFilter] = useState(() => {
        return localStorage.getItem('calls_filter_duration') || 'all';
    });
    const [labelFilter, setLabelFilter] = useState(() => {
        return localStorage.getItem('calls_filter_label') || 'all';
    });
    const [availableLabels, setAvailableLabels] = useState([]);
    const [nameFilter, setNameFilter] = useState(() => {
        return localStorage.getItem('calls_filter_name') || 'all';
    });

    // Sorting (persisted)
    const [sortConfig, setSortConfig] = useState(() => {
        const saved = localStorage.getItem('calls_sort_config');
        return saved ? JSON.parse(saved) : { key: 'call_time', direction: 'DESC' };
    });

    // Customization Modals
    const [showColumnCustomization, setShowColumnCustomization] = useState(false);
    const [showFilterCustomization, setShowFilterCustomization] = useState(false);

    // Filter Bar Customization
    const defaultFilterOrder = ['name', 'employee', 'date', 'type', 'connected', 'status', 'notes', 'recording', 'duration', 'labels', 'segments'];
    const [filterOrder, setFilterOrder] = useState(() => {
        const saved = localStorage.getItem('calls_filter_order');
        return saved ? JSON.parse(saved) : defaultFilterOrder;
    });
    const [visibleFilters, setVisibleFilters] = useState(() => {
        const saved = localStorage.getItem('calls_visible_filters');
        return saved ? JSON.parse(saved) : defaultFilterOrder;
    });

    // Migration: Ensure 'name' filter is present if it was missing from saved state
    useEffect(() => {
        setFilterOrder(prev => {
            if (!prev.includes('name')) return ['name', ...prev];
            return prev;
        });
        setVisibleFilters(prev => {
            if (!prev.includes('name')) return ['name', ...prev];
            return prev;
        });
    }, []);

    const [draggedFilter, setDraggedFilter] = useState(null);

    // Column Settings
    const defaultColumnOrder = ['time', 'contact', 'type', 'person_note', 'note', 'recording', 'labels', 'employee', 'device_phone'];

    const [columnOrder, setColumnOrder] = useState(() => {
        const saved = localStorage.getItem('calls_column_order');
        return saved ? JSON.parse(saved) : defaultColumnOrder;
    });

    const [visibleColumns, setVisibleColumns] = useState(() => {
        const saved = localStorage.getItem('calls_visible_columns');
        return saved ? JSON.parse(saved) : defaultColumnOrder;
    });

    const [columnWidths, setColumnWidths] = useState(() => {
        const saved = localStorage.getItem('calls_column_widths');
        return saved ? JSON.parse(saved) : {
            time: 150,
            contact: 250,
            type: 120,
            person_note: 200,
            note: 200,
            recording: 180,
            labels: 180,
            employee: 150,
            device_phone: 150
        };
    });

    const resizingRef = useRef(null);

    const [contactColumnOptions, setContactColumnOptions] = useState(() => {
        const saved = localStorage.getItem('calls_contact_column_options');
        return saved ? JSON.parse(saved) : { showPhone: true, showReview: true, showDuration: true };
    });

    // New Calls Notification Logic
    const [clickedNewCalls, setClickedNewCalls] = useState(() => {
        try {
            const saved = localStorage.getItem('calls_clicked_new');
            return saved ? JSON.parse(saved) : {};
        } catch (e) { return {}; }
    });

    const markCallClicked = (callId) => {
        setClickedNewCalls(prev => {
            const updated = { ...prev, [callId]: Date.now() };
            // Cleanup: Keep only items from the last 10 minutes to stay lean
            const limit = Date.now() - 10 * 60 * 1000;
            const cleaned = Object.fromEntries(
                Object.entries(updated).filter(([_, time]) => time > limit)
            );
            localStorage.setItem('calls_clicked_new', JSON.stringify(cleaned));
            return cleaned;
        });
    };

    const newArrivals = useMemo(() => {
        const twoMinsAgo = Date.now() - 2 * 60 * 1000;
        return calls
            .filter(call => {
                const callTime = new Date(call.call_time + (call.call_time.endsWith('Z') ? '' : 'Z')).getTime();
                return callTime > twoMinsAgo && !clickedNewCalls[call.id];
            })
            .sort((a, b) => new Date(b.call_time).getTime() - new Date(a.call_time).getTime())
            .slice(0, 5);
    }, [calls, clickedNewCalls]);

    const [draggedColumn, setDraggedColumn] = useState(null);

    // Advanced Filters (persisted)
    const [customFilters, setCustomFilters] = useState(() => {
        const saved = localStorage.getItem('calls_custom_filters');
        return saved ? JSON.parse(saved) : [];
    });
    const [savedFilterSets, setSavedFilterSets] = useState(() => {
        const saved = localStorage.getItem('calls_saved_filter_sets');
        return saved ? JSON.parse(saved) : [];
    });
    const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);

    // UI States (some persisted)
    const [showCustomDate, setShowCustomDate] = useState(false);
    const [editingNote, setEditingNote] = useState(null);
    const [showSegmentsDropdown, setShowSegmentsDropdown] = useState(false);
    const [filterSetName, setFilterSetName] = useState('');
    const [showFilterBar, setShowFilterBar] = useState(() => {
        const saved = localStorage.getItem('calls_show_filter_bar');
        return saved !== null ? JSON.parse(saved) : true;
    });
    const [activeSegement, setActiveSegement] = useState(null);
    const [isProcessing, setIsProcessing] = useState(false);

    // Confirmation Modal State
    const [confirmModal, setConfirmModal] = useState({
        isOpen: false,
        title: '',
        message: '',
        onConfirm: null,
        isDestructive: false
    });

    // Prompt Modal State
    const [promptModal, setPromptModal] = useState({
        isOpen: false,
        title: '',
        message: '',
        value: '',
        onConfirm: null
    });

    // Calculate active filter count
    const activeFilterCount = useMemo(() => {
        let count = 0;
        if (direction !== 'all') count++;
        if (reviewedFilter !== 'all') count++;
        if (connectedFilter !== 'all') count++;
        if (noteFilter !== 'all') count++;
        if (recordingFilter !== 'all') count++;
        if (durationFilter !== 'all') count++;
        if (labelFilter !== 'all') count++;
        if (nameFilter !== 'all') count++;
        if (selectedEmployee) count++;
        if (dateRange !== '7days') count++;
        if (customFilters.length > 0) count += customFilters.length;
        return count;
    }, [direction, reviewedFilter, connectedFilter, noteFilter, recordingFilter, durationFilter, labelFilter, nameFilter, selectedEmployee, dateRange, customFilters]);

    const [segmentsMenuPosition, setSegmentsMenuPosition] = useState({ top: 0, left: 0 });
    const segmentsButtonRef = useRef(null);

    // Resize Logic
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
            const newWidth = Math.max(50, startWidth + diff);

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

    useEffect(() => {
        if (location.state?.date) {
            setDateRange('custom');
            setCustomRange({
                startDate: location.state.date,
                endDate: location.state.date
            });
            // Ensure the UI reflects the custom date picker is active/relevant if needed
            // But 'dateRange' state handles the filter logic. 
        }
    }, [location.state]);

    useEffect(() => {
        fetchEmployees();
        fetchLabels();
    }, []);

    const fetchLabels = async () => {
        try {
            const res = await api.get('/calls.php?action=labels');
            if (res.data?.data && Array.isArray(res.data.data)) {
                setAvailableLabels(res.data.data.map(l => l.label).filter(Boolean));
            }
        } catch (err) {
            console.error("Failed to fetch labels", err);
        }
    };

    // Reset to page 1 when filters change (except page itself)
    useEffect(() => {
        setPagination(prev => ({ ...prev, page: 1 }));
    }, [direction, dateRange, customRange.startDate, customRange.endDate, selectedEmployee, reviewedFilter, connectedFilter, noteFilter, recordingFilter, durationFilter, JSON.stringify(customFilters)]);

    // --- Settings Sync Logic ---
    const isFirstLoad = useRef(true);

    // Save Settings to API (Debounced)
    useEffect(() => {
        if (isFirstLoad.current) return;

        const timer = setTimeout(() => {
            const settings = {
                filters: {
                    direction, reviewed: reviewedFilter, dateRange, customRange, employee: selectedEmployee,
                    connected: connectedFilter, note: noteFilter, recording: recordingFilter,
                    duration: durationFilter, label: labelFilter, name: nameFilter,
                    visible: visibleFilters, order: filterOrder
                },
                columns: {
                    visible: visibleColumns, widths: columnWidths, order: columnOrder, options: contactColumnOptions
                },
                ui: {
                    sort: sortConfig, showFilterBar
                },
                customFilters: {
                    active: customFilters, sets: savedFilterSets
                }
            };

            api.post('/user_settings.php', {
                key: 'calls_page_config',
                value: settings
            }).catch(err => console.error("Failed to save settings", err));

        }, 2000); // 2 second debounce

        return () => clearTimeout(timer);
    }, [
        direction, reviewedFilter, dateRange, customRange, selectedEmployee,
        connectedFilter, noteFilter, recordingFilter, durationFilter, labelFilter, nameFilter,
        visibleFilters, filterOrder,
        visibleColumns, columnWidths, columnOrder, contactColumnOptions,
        sortConfig, showFilterBar,
        customFilters, savedFilterSets
    ]);

    // Load Settings from API on Mount
    useEffect(() => {
        const loadSettings = async () => {
            try {
                const res = await api.get('/user_settings.php?key=calls_page_config');
                if (res.data?.data?.value) {
                    const s = res.data.data.value;

                    // Restore Filters
                    if (s.filters) {
                        if (s.filters.direction) setDirection(s.filters.direction);
                        if (s.filters.reviewed) setReviewedFilter(s.filters.reviewed);
                        if (s.filters.dateRange) setDateRange(s.filters.dateRange);
                        if (s.filters.customRange) setCustomRange(s.filters.customRange);
                        if (s.filters.employee) setSelectedEmployee(s.filters.employee);
                        if (s.filters.connected) setConnectedFilter(s.filters.connected);
                        if (s.filters.note) setNoteFilter(s.filters.note);
                        if (s.filters.recording) setRecordingFilter(s.filters.recording);
                        if (s.filters.duration) setDurationFilter(s.filters.duration);
                        if (s.filters.label) setLabelFilter(s.filters.label);
                        if (s.filters.name) setNameFilter(s.filters.name);
                        if (s.filters.visible) setVisibleFilters(s.filters.visible);
                        if (s.filters.order) setFilterOrder(s.filters.order);
                    }

                    // Restore Columns
                    if (s.columns) {
                        if (s.columns.visible) setVisibleColumns(s.columns.visible);
                        if (s.columns.widths) setColumnWidths(s.columns.widths);
                        if (s.columns.order) setColumnOrder(s.columns.order);
                        if (s.columns.options) setContactColumnOptions(s.columns.options);
                    }

                    // Restore UI
                    if (s.ui) {
                        if (s.ui.sort) setSortConfig(s.ui.sort);
                        if (s.ui.showFilterBar !== undefined) setShowFilterBar(s.ui.showFilterBar);
                    }

                    // Restore Custom Filters
                    if (s.customFilters) {
                        if (s.customFilters.active) setCustomFilters(s.customFilters.active);
                        if (s.customFilters.sets) setSavedFilterSets(s.customFilters.sets);
                    }
                }
            } catch (err) {
                console.error("Failed to load settings", err);
            } finally {
                isFirstLoad.current = false;
            }
        };
        loadSettings();
    }, []);

    // Save column settings (Keep localStorage for backup/offline speed)
    useEffect(() => {
        localStorage.setItem('calls_visible_columns', JSON.stringify(visibleColumns));
    }, [visibleColumns]);

    useEffect(() => {
        localStorage.setItem('calls_column_widths', JSON.stringify(columnWidths));
    }, [columnWidths]);

    useEffect(() => {
        localStorage.setItem('calls_contact_column_options', JSON.stringify(contactColumnOptions));
    }, [contactColumnOptions]);

    // Save column order
    useEffect(() => {
        localStorage.setItem('calls_column_order', JSON.stringify(columnOrder));
    }, [columnOrder]);

    // Save filter customization
    useEffect(() => {
        localStorage.setItem('calls_visible_filters', JSON.stringify(visibleFilters));
    }, [visibleFilters]);

    useEffect(() => {
        localStorage.setItem('calls_filter_order', JSON.stringify(filterOrder));
    }, [filterOrder]);

    // Save filter sets
    useEffect(() => {
        localStorage.setItem('calls_saved_filter_sets', JSON.stringify(savedFilterSets));
    }, [savedFilterSets]);

    // Persist filter states
    useEffect(() => {
        localStorage.setItem('calls_filter_direction', direction);
    }, [direction]);

    useEffect(() => {
        localStorage.setItem('calls_filter_reviewed', reviewedFilter);
    }, [reviewedFilter]);

    useEffect(() => {
        localStorage.setItem('calls_filter_dateRange', dateRange);
    }, [dateRange]);

    useEffect(() => {
        localStorage.setItem('calls_filter_customRange', JSON.stringify(customRange));
    }, [customRange]);

    useEffect(() => {
        localStorage.setItem('calls_filter_employee', selectedEmployee);
    }, [selectedEmployee]);

    useEffect(() => {
        localStorage.setItem('calls_custom_filters', JSON.stringify(customFilters));
    }, [customFilters]);

    useEffect(() => {
        localStorage.setItem('calls_sort_config', JSON.stringify(sortConfig));
    }, [sortConfig]);

    useEffect(() => {
        localStorage.setItem('calls_show_filter_bar', JSON.stringify(showFilterBar));
    }, [showFilterBar]);

    // Persist additional filters
    useEffect(() => {
        localStorage.setItem('calls_filter_connected', connectedFilter);
    }, [connectedFilter]);

    useEffect(() => {
        localStorage.setItem('calls_filter_note', noteFilter);
    }, [noteFilter]);

    useEffect(() => {
        localStorage.setItem('calls_filter_recording', recordingFilter);
    }, [recordingFilter]);

    useEffect(() => {
        localStorage.setItem('calls_filter_duration', durationFilter);
    }, [durationFilter]);

    useEffect(() => {
        localStorage.setItem('calls_filter_name', nameFilter);
    }, [nameFilter]);

    useEffect(() => {
        localStorage.setItem('calls_filter_label', labelFilter);
    }, [labelFilter]);

    // Fetch calls when page or any filter changes
    useEffect(() => {
        const fetchData = async () => {
            // Don't fetch if custom range is selected but incomplete
            if (dateRange === 'custom' && (!customRange.startDate || !customRange.endDate)) {
                return;
            }

            setLoading(true);
            try {
                const paramsParams = {
                    page: pagination.page,
                    limit: pagination.limit,
                    dateRange, // Always send dateRange as it's a primary filter
                    search,
                    tzOffset: new Date().getTimezoneOffset(),
                    sortBy: sortConfig.key,
                    sortOrder: sortConfig.direction,
                    customFilters: JSON.stringify(customFilters),
                };

                if (direction !== 'all') paramsParams.direction = direction;
                if (selectedEmployee) paramsParams.employeeId = selectedEmployee;
                if (connectedFilter !== 'all') paramsParams.connected = connectedFilter;
                if (noteFilter !== 'all') paramsParams.noteFilter = noteFilter;
                if (recordingFilter !== 'all') paramsParams.recordingFilter = recordingFilter;
                if (durationFilter !== 'all') paramsParams.durationFilter = durationFilter;
                if (recordingFilter !== 'all') paramsParams.recordingFilter = recordingFilter;
                if (durationFilter !== 'all') paramsParams.durationFilter = durationFilter;
                if (labelFilter !== 'all') paramsParams.label = labelFilter;
                if (nameFilter !== 'all') paramsParams.nameFilter = nameFilter;

                const params = new URLSearchParams(paramsParams);

                if (reviewedFilter !== 'all') {
                    params.append('reviewed', reviewedFilter === 'reviewed' ? 'reviewed' : 'unreviewed');
                }

                if (dateRange === 'custom') {
                    params.append('startDate', customRange.startDate);
                    params.append('endDate', customRange.endDate);
                }

                console.log('Fetching calls with params:', params.toString());
                const res = await api.get(`/calls.php?${params.toString()}`);
                console.log('Calls response:', res.data);

                if (res.data && Array.isArray(res.data.data)) {
                    console.log('Successfully fetched calls:', res.data.data.length);
                    setCalls(res.data.data);
                    if (res.data.pagination) {
                        setPagination(prev => ({
                            ...prev,
                            total: res.data.pagination.total,
                            total_pages: res.data.pagination.total_pages
                        }));
                    }
                } else if (Array.isArray(res.data)) {
                    console.log('Successfully fetched calls (legacy format):', res.data.length);
                    setCalls(res.data);
                } else {
                    console.warn('Backend returned no data or unexpected format:', res.data);
                    setCalls([]);
                }
            } catch (err) {
                console.error('Error fetching calls:', err);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [pagination.page, pagination.limit, direction, dateRange, customRange.startDate, customRange.endDate, selectedEmployee, search, sortConfig, customFilters, reviewedFilter, connectedFilter, noteFilter, recordingFilter, durationFilter, labelFilter, nameFilter]);

    const fetchEmployees = async () => {
        try {
            const res = await api.get('/employees.php');
            if (res.data && Array.isArray(res.data)) {
                setEmployees(res.data);
            }
        } catch (err) {
            console.error("Failed to fetch employees", err);
        }
    };

    const handleUpdateCall = async (callId, updates) => {
        try {
            const targetCall = calls.find(c => c.id === callId);
            const targetPhone = targetCall ? targetCall.phone_number : null;

            // Optimistic update
            setCalls(prev => prev.map(c => {
                if (c.id === callId) {
                    return { ...c, ...updates };
                }
                if (targetPhone && c.phone_number === targetPhone) {
                    const newValues = {};
                    if (updates.person_note !== undefined) newValues.person_note = updates.person_note;
                    if (updates.contact_name !== undefined) newValues.contact_name = updates.contact_name;

                    if (Object.keys(newValues).length > 0) {
                        return { ...c, ...newValues };
                    }
                }
                return c;
            }));

            // Removed undefined selectedCall logic
            // if (selectedCall && selectedCall.id === callId) {
            //     setSelectedCall(prev => ({ ...prev, ...updates }));
            // }

            await api.post(`/calls.php?action=update&id=${callId}`, updates);
            toast.success("Updated successfully");
        } catch (err) {
            console.error("Failed to update call", err);
            // fetchCalls(); // Removed: not accessible
            toast.error("Failed to update");
        }
    };

    const handleToggleReviewed = async (callId, currentReviewed) => {
        const newReviewed = !currentReviewed;
        setCalls(prev => prev.map(c =>
            c.id === callId ? { ...c, reviewed: newReviewed } : c
        ));
        try {
            await api.post(`/calls.php?action=update&id=${callId}`, { reviewed: newReviewed });
            toast.success(newReviewed ? "Marked as reviewed" : "Marked as unreviewed");
        } catch (err) {
            console.error("Failed to update reviewed status", err);
            setCalls(prev => prev.map(c =>
                c.id === callId ? { ...c, reviewed: currentReviewed } : c
            ));
            toast.error("Failed to update status");
        }
    };

    const handleLike = async (callId, currentLiked) => {
        // Toggle 1/0 or true/false logic. Backend uses TinyInt(1).
        const newLiked = !currentLiked;
        setCalls(prev => prev.map(c =>
            c.id === callId ? { ...c, is_liked: newLiked } : c
        ));
        try {
            await api.post(`/calls.php?action=update&id=${callId}`, { is_liked: newLiked });
            toast.success(newLiked ? "Added to favorites" : "Removed from favorites");
        } catch (err) {
            console.error("Failed to update like status", err);
            setCalls(prev => prev.map(c =>
                c.id === callId ? { ...c, is_liked: currentLiked } : c
            ));
            toast.error("Failed to update");
        }
    };

    const handleUpdateLabels = async (callId, currentLabelsStr, action, label) => {
        let newLabelsStr;

        if (action === 'set') {
            // Direct set from modal
            newLabelsStr = currentLabelsStr; // In this case, currentLabelsStr is the new labels
        } else {
            // Legacy add/remove
            const labels = currentLabelsStr ? currentLabelsStr.split(',').filter(Boolean) : [];
            let newLabels = [...labels];

            if (action === 'add') {
                if (!newLabels.includes(label)) newLabels.push(label);
            } else {
                newLabels = newLabels.filter(l => l !== label);
            }
            newLabelsStr = newLabels.join(',');
        }

        // Optimistic update
        const originalLabels = calls.find(c => c.id === callId)?.labels || '';
        setCalls(prev => prev.map(c =>
            c.id === callId ? { ...c, labels: newLabelsStr } : c
        ));

        try {
            await api.post(`/calls.php?action=update&id=${callId}`, { labels: newLabelsStr });
            if (action === 'set') {
                toast.success("Labels updated");
            } else {
                toast.success(action === 'add' ? `Label "${label}" added` : "Label removed");
            }
        } catch (err) {
            console.error("Failed to update labels", err);
            setCalls(prev => prev.map(c =>
                c.id === callId ? { ...c, labels: originalLabels } : c
            ));
            toast.error("Failed to update labels");
        }
    };

    const handlePageChange = (newPage) => {
        if (newPage >= 1 && newPage <= pagination.total_pages) {
            setPagination(prev => ({ ...prev, page: newPage }));
        }
    };

    const getTypeIcon = (type) => {
        const lowerType = type?.toLowerCase() || '';
        if (lowerType.includes('in')) return <PhoneIncoming size={16} className="text-gray-400 group-hover:text-blue-500 transition-colors" />;
        if (lowerType.includes('out')) return <PhoneOutgoing size={16} className="text-gray-400 group-hover:text-green-500 transition-colors" />;
        if (lowerType.includes('reject') || lowerType.includes('block')) return <PhoneMissed size={16} className="text-gray-400" />;
        return <PhoneMissed size={16} className="text-red-400" />;
    };

    const handleSort = (key) => {
        setSortConfig(prev => ({
            key,
            direction: prev.key === key && prev.direction === 'ASC' ? 'DESC' : 'ASC'
        }));
    };

    const getSortIcon = (key) => {
        if (sortConfig.key !== key) return <ArrowUpDown size={14} className="text-gray-300 ml-1" />;
        return sortConfig.direction === 'ASC'
            ? <ArrowUp size={14} className="text-blue-500 ml-1" />
            : <ArrowDown size={14} className="text-blue-500 ml-1" />;
    };

    const toggleColumn = (col) => {
        setVisibleColumns(prev =>
            prev.includes(col) ? prev.filter(c => c !== col) : [...prev, col]
        );
    };

    const handleColumnDragStart = (e, columnId) => {
        setDraggedColumn(columnId);
        e.dataTransfer.setData('text/plain', columnId);
        e.dataTransfer.effectAllowed = 'move';
    };

    const handleColumnDragOver = (e, targetColumnId) => {
        e.preventDefault();
        if (draggedColumn === targetColumnId) return;

        const newOrder = [...columnOrder];
        const draggingIdx = newOrder.indexOf(draggedColumn);
        const hoverIdx = newOrder.indexOf(targetColumnId);

        if (draggingIdx === -1) return;

        newOrder.splice(draggingIdx, 1);
        newOrder.splice(hoverIdx, 0, draggedColumn);
        setColumnOrder(newOrder);
    };

    const handleColumnDragEnd = () => {
        setDraggedColumn(null);
    };

    const handleFilterDragStart = (e, id) => {
        setDraggedFilter(id);
        e.dataTransfer.effectAllowed = 'move';
    };

    const handleFilterDragOver = (e, id) => {
        e.preventDefault();
        if (draggedFilter === id) return;

        const items = [...filterOrder];
        const draggedIdx = items.indexOf(draggedFilter);
        const hoverIdx = items.indexOf(id);

        items.splice(draggedIdx, 1);
        items.splice(hoverIdx, 0, draggedFilter);
        setFilterOrder(items);
    };

    const handleFilterDragEnd = () => {
        setDraggedFilter(null);
    };

    const toggleFilter = (id) => {
        setVisibleFilters(prev =>
            prev.includes(id) ? prev.filter(f => f !== id) : [...prev, id]
        );
    };

    const addCustomFilter = () => {
        setCustomFilters(prev => [...prev, { id: Date.now(), key: 'contact_name', operator: 'contains', value: '' }]);
    };

    const removeCustomFilter = (id) => {
        setCustomFilters(prev => prev.filter(f => f.id !== id));
    };

    const updateCustomFilter = (id, updates) => {
        setCustomFilters(prev => prev.map(f => f.id === id ? { ...f, ...updates } : f));
    };

    const saveFilterSet = () => {
        if (!filterSetName.trim()) {
            toast.error("Please enter a name for the filter set");
            return;
        }
        const newSet = {
            id: Date.now(),
            name: filterSetName,
            filters: customFilters,
            dateRange,
            direction,
            reviewedFilter
        };
        setSavedFilterSets(prev => [...prev, newSet]);
        setFilterSetName('');
        toast.success(`Filter set "${newSet.name}" saved`);
        toast.info("You can access this view later from the 'Saved' section.");
    };

    const updateFilterSet = () => {
        if (!activeSegement) return;
        if (!filterSetName.trim()) {
            toast.error("Please enter a name for the filter set");
            return;
        }

        setSavedFilterSets(prev => prev.map(s => s.id === activeSegement.id ? {
            ...s,
            name: filterSetName,
            filters: customFilters,
            dateRange,
            direction,
            reviewedFilter
        } : s));

        setActiveSegement(prev => ({
            ...prev,
            name: filterSetName,
            filters: customFilters,
            dateRange,
            direction,
            reviewedFilter
        }));

        toast.success(`Segment "${filterSetName}" updated`);
    };

    const deleteFilterSet = (id, e) => {
        if (e) e.stopPropagation();
        const setToDelete = savedFilterSets.find(s => s.id === id);

        setConfirmModal({
            isOpen: true,
            title: 'Delete Segment',
            message: `Are you sure you want to delete the segment "${setToDelete?.name}"?`,
            isDestructive: true,
            onConfirm: async () => {
                setSavedFilterSets(prev => prev.filter(s => s.id !== id));
                if (activeSegement?.id === id) {
                    setActiveSegement(null);
                }
                toast.success("Segment deleted");
            }
        });
    };

    const renameFilterSet = (id, e) => {
        if (e) e.stopPropagation();
        const setToRename = savedFilterSets.find(s => s.id === id);

        setPromptModal({
            isOpen: true,
            title: 'Rename Segment',
            message: 'Enter new name for the segment:',
            value: setToRename?.name || '',
            onConfirm: (newName) => {
                if (newName && newName.trim()) {
                    setSavedFilterSets(prev => prev.map(s => s.id === id ? { ...s, name: newName.trim() } : s));
                    if (activeSegement?.id === id) {
                        setActiveSegement(prev => ({ ...prev, name: newName.trim() }));
                    }
                    toast.success("Segment renamed");
                }
            }
        });
    };

    const applyFilterSet = (set) => {
        setCustomFilters(set.filters);
        setDateRange(set.dateRange);
        setDirection(set.direction);
        setReviewedFilter(set.reviewedFilter || 'all');
        setShowSegmentsDropdown(false);
        setActiveSegement(set);
        setFilterSetName(set.name);
        toast.success(`Filter set "${set.name}" applied`);
    };

    const clearAllFilters = () => {
        setCustomFilters([]);
        setDirection('all');
        setReviewedFilter('all');
        setConnectedFilter('all');
        setNoteFilter('all');
        setRecordingFilter('all');
        setDurationFilter('all');
        setLabelFilter('all');
        setNameFilter('all');
        setDateRange('7days');
        setSelectedEmployee('');
        setActiveSegement(null);
        toast.success("All filters cleared");
    }

    const removeActiveSegment = () => {
        setCustomFilters([]);
        // Optionally reset other filters if they were part of the segment, but typically we just clear the 'custom' part or reset all?
        // Let's assume removing a segment just clears the custom filters it applied and resets the segment tracker.
        // Or if we want to revert to "default" (all), we can do:
        clearAllFilters();
    }

    const columnLabels = {
        time: 'Time',
        contact: 'Contact',
        type: 'Type',
        person_note: 'Person Note',
        note: 'Call Note',
        recording: 'Recording',
        labels: 'Labels',
        employee: 'Employee',
        device_phone: 'Device Phone'
    };

    const filterableKeys = [
        { id: 'contact_name', label: 'Contact Name' },
        { id: 'phone_number', label: 'Phone Number' },
        { id: 'duration', label: 'Duration (sec)' },
        { id: 'type', label: 'Call Type' },
        { id: 'note', label: 'Call Note' },
        { id: 'person_note', label: 'Person Note' },
        { id: 'labels', label: 'Labels' },
        { id: 'employee_name', label: 'Employee Name' }
    ];

    const filterOperators = [
        { id: 'equal', label: 'is equal to' },
        { id: 'not_equal', label: 'is not equal' },
        { id: 'contains', label: 'contains' },
        { id: 'not_contains', label: 'does not contain' },
        { id: 'greater_than', label: 'is greater than' },
        { id: 'less_than', label: 'is less than' },
        { id: 'starts_with', label: 'starts with' },
        { id: 'ends_with', label: 'ends with' },
        { id: 'is_empty', label: 'is empty' },
        { id: 'is_not_empty', label: 'is not empty' }
    ];

    return (
        <div className="relative">
            {/* Floating Loading Indicator */}
            {loading && calls.length > 0 && (
                <div className="absolute inset-x-0 top-12 z-10 flex justify-center pointer-events-none">
                    <div className="bg-white shadow-md border px-4 py-2 rounded-full flex items-center gap-2">
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
                        <span className="text-sm font-medium text-gray-600">Updating...</span>
                    </div>
                </div>
            )}

            <div className={`space-y-6 ${loading ? 'opacity-60 transition-opacity duration-200' : ''}`}>
                {/* Header Row */}
                <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6">
                    {/* Left side: Date Range Filter - Always Visible */}
                    <DateRangeFilter
                        value={dateRange}
                        onChange={setDateRange}
                        customRange={customRange}
                        onCustomRangeChange={setCustomRange}
                        showSettingsButton={true}
                    />

                    {/* Right side: Search + Filter Toggle + Employee + Stats */}
                    <div className="flex items-center gap-3 w-full lg:w-auto ml-auto">
                        <div className="flex items-center bg-white border border-gray-200 rounded-xl p-1.5 shadow-sm w-full lg:w-auto">
                            <div className="relative flex-1 lg:flex-initial">
                                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                                <input
                                    type="text"
                                    placeholder="Search calls..."
                                    value={search}
                                    onChange={(e) => setSearch(e.target.value)}
                                    className="pl-10 pr-4 py-2.5 text-base bg-transparent border-none outline-none w-full lg:w-72 font-medium"
                                />
                            </div>
                            <div className="w-px h-6 bg-gray-200 mx-2"></div>
                            <button
                                onClick={() => setShowFilterBar(!showFilterBar)}
                                className={`relative p-2 rounded-lg transition-all ${showFilterBar ? 'bg-blue-600 text-white shadow-lg shadow-blue-100' : 'text-gray-400 hover:text-gray-600 hover:bg-gray-50'}`}
                                title={showFilterBar ? "Hide Filters" : "Show Filters"}
                            >
                                <Filter size={18} />
                                {activeFilterCount > 0 && (
                                    <span className={`absolute -top-1 -right-1 min-w-[18px] h-[18px] flex items-center justify-center text-[10px] font-bold rounded-full px-1 ${showFilterBar ? 'bg-white text-blue-600' : 'bg-blue-600 text-white'}`}>
                                        {activeFilterCount}
                                    </span>
                                )}
                            </button>
                        </div>

                        <div className="flex items-center bg-white border border-gray-200 rounded-xl p-1.5 shadow-sm">
                            <button
                                onClick={() => setShowColumnCustomization(true)}
                                className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-all"
                                title="Customize Table Columns"
                            >
                                <Columns size={18} />
                            </button>
                        </div>

                    </div>
                </div>

                {/* New Arrivals Banner */}
                {newArrivals.length > 0 && (
                    <div className="flex flex-wrap gap-2 animate-in fade-in slide-in-from-top-2 duration-300">
                        {newArrivals.map(call => (
                            <button
                                key={`new-${call.id}`}
                                onClick={() => {
                                    markCallClicked(call.id);
                                    openPersonModal(call);
                                }}
                                className="flex items-center gap-2 px-3 py-1.5 bg-blue-50 border border-blue-100 rounded-full hover:bg-blue-100 transition-all group"
                            >
                                <span className="flex h-2 w-2 rounded-full bg-blue-600 animate-pulse"></span>
                                <span className="text-xs font-bold text-blue-700">NEW:</span>
                                <span className="text-xs font-medium text-blue-900 group-hover:underline">
                                    {call.contact_name || call.phone_number || 'Unknown'}
                                </span>
                                <ChevronRight size={12} className="text-blue-400" />
                            </button>
                        ))}
                    </div>
                )}

                {/* Filters Bar & Table Container */}
                <div className="space-y-6">
                    {showFilterBar && (
                        <div className="relative group bg-white rounded-2xl border border-gray-100 shadow-md animate-in fade-in slide-in-from-top-4 duration-300">
                            <div className="flex items-center overflow-x-auto scrollbar-thin py-5 px-5 gap-3 pr-[180px]"> {/* pr-[180px] for sticky button space */}
                                {/* Filter Label Indicator */}
                                <div className="flex items-center gap-2 px-3 py-1.5 bg-gray-50 rounded-lg mr-2 border border-gray-100 flex-shrink-0">
                                    <Filter size={14} className="text-gray-400" />
                                    <span className="text-xs font-bold text-gray-500 uppercase tracking-widest">Filters</span>
                                </div>


                                {filterOrder.filter(id => visibleFilters.includes(id) && id !== 'date').map(id => {
                                    if (id === 'employee') return (
                                        <div key="employee" className="relative group">
                                            <select
                                                value={selectedEmployee || ''}
                                                onChange={(e) => setSelectedEmployee(e.target.value)}
                                                className={`appearance-none text-sm rounded-xl py-2 pl-4 pr-10 bg-gray-50 outline-none transition-all cursor-pointer w-full min-w-[150px] ${selectedEmployee ? 'border-2 border-blue-400 bg-blue-50 text-blue-700 font-bold shadow-sm' : 'border border-gray-200 text-gray-700 font-medium hover:bg-gray-100'}`}
                                            >
                                                <option value="">All Employees</option>
                                                {employees.map(emp => (
                                                    <option key={emp.id} value={emp.id}>{emp.name}</option>
                                                ))}
                                            </select>
                                            <ChevronDown size={14} className={`absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none transition-colors ${selectedEmployee ? 'text-blue-500' : 'text-gray-400 group-hover:text-gray-600'}`} />
                                        </div>
                                    );
                                    if (id === 'type') return (
                                        <div key="type" className="relative group">
                                            <select
                                                value={direction}
                                                onChange={(e) => setDirection(e.target.value)}
                                                className={`appearance-none text-sm rounded-xl py-2 pl-4 pr-10 bg-gray-50 outline-none transition-all cursor-pointer ${direction !== 'all' ? 'border-2 border-blue-400 bg-blue-50 text-blue-700 font-bold shadow-sm' : 'border border-gray-200 text-gray-700 font-medium hover:bg-gray-100'}`}
                                            >
                                                <option value="all">All Types</option>
                                                <option value="inbound">Inbound</option>
                                                <option value="outbound">Outbound</option>
                                                <option value="missed">Missed</option>
                                                <option value="rejected">Rejected</option>
                                                <option value="blocked">Blocked</option>
                                            </select>
                                            <ChevronDown size={14} className={`absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none transition-colors ${direction !== 'all' ? 'text-blue-500' : 'text-gray-400 group-hover:text-gray-600'}`} />
                                        </div>
                                    );
                                    if (id === 'connected') return (
                                        <div key="connected" className="relative group">
                                            <select
                                                value={connectedFilter}
                                                onChange={(e) => setConnectedFilter(e.target.value)}
                                                className={`appearance-none text-sm rounded-xl py-2 pl-4 pr-10 bg-gray-50 outline-none transition-all cursor-pointer ${connectedFilter !== 'all' ? 'border-2 border-blue-400 bg-blue-50 text-blue-700 font-bold shadow-sm' : 'border border-gray-200 text-gray-700 font-medium hover:bg-gray-100'}`}
                                            >
                                                <option value="all">All Connections</option>
                                                <option value="connected">Connected</option>
                                                <option value="not_connected">Not Connected</option>
                                            </select>
                                            <ChevronDown size={14} className={`absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none transition-colors ${connectedFilter !== 'all' ? 'text-blue-500' : 'text-gray-400 group-hover:text-gray-600'}`} />
                                        </div>
                                    );
                                    if (id === 'name') return (
                                        <div key="name" className="relative group">
                                            <select
                                                value={nameFilter}
                                                onChange={(e) => setNameFilter(e.target.value)}
                                                className={`appearance-none text-sm rounded-xl py-2 pl-4 pr-10 bg-gray-50 outline-none transition-all cursor-pointer ${nameFilter !== 'all' ? 'border-2 border-blue-400 bg-blue-50 text-blue-700 font-bold shadow-sm' : 'border border-gray-200 text-gray-700 font-medium hover:bg-gray-100'}`}
                                            >
                                                <option value="all">All Names</option>
                                                <option value="has_name">Has Name</option>
                                                <option value="no_name">No Name</option>
                                            </select>
                                            <ChevronDown size={14} className={`absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none transition-colors ${nameFilter !== 'all' ? 'text-blue-500' : 'text-gray-400 group-hover:text-gray-600'}`} />
                                        </div>
                                    );
                                    if (id === 'status') return (
                                        <div key="status" className="relative group">
                                            <select
                                                value={reviewedFilter}
                                                onChange={(e) => setReviewedFilter(e.target.value)}
                                                className={`appearance-none text-sm rounded-xl py-2 pl-4 pr-10 bg-gray-50 outline-none transition-all cursor-pointer ${reviewedFilter !== 'all' ? 'border-2 border-blue-400 bg-blue-50 text-blue-700 font-bold shadow-sm' : 'border border-gray-200 text-gray-700 font-medium hover:bg-gray-100'}`}
                                            >
                                                <option value="all">All Status</option>
                                                <option value="reviewed">Reviewed</option>
                                                <option value="not_reviewed">Not Reviewed</option>
                                            </select>
                                            <ChevronDown size={14} className={`absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none transition-colors ${reviewedFilter !== 'all' ? 'text-blue-500' : 'text-gray-400 group-hover:text-gray-600'}`} />
                                        </div>
                                    );
                                    if (id === 'notes') return (
                                        <div key="notes" className="relative group">
                                            <select
                                                value={noteFilter}
                                                onChange={(e) => setNoteFilter(e.target.value)}
                                                className={`appearance-none text-sm rounded-xl py-2 pl-4 pr-10 bg-gray-50 outline-none transition-all cursor-pointer ${noteFilter !== 'all' ? 'border-2 border-blue-400 bg-blue-50 text-blue-700 font-bold shadow-sm' : 'border border-gray-200 text-gray-700 font-medium hover:bg-gray-100'}`}
                                            >
                                                <option value="all">All Notes</option>
                                                <option value="has_call_note">Has Call Note</option>
                                                <option value="no_call_note">No Call Note</option>
                                                <option value="has_person_note">Has Person Note</option>
                                                <option value="no_person_note">No Person Note</option>
                                                <option value="has_any_note">Has Any Note</option>
                                            </select>
                                            <ChevronDown size={14} className={`absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none transition-colors ${noteFilter !== 'all' ? 'text-blue-500' : 'text-gray-400 group-hover:text-gray-600'}`} />
                                        </div>
                                    );
                                    if (id === 'recording') return (
                                        <div key="recording" className="relative group">
                                            <select
                                                value={recordingFilter}
                                                onChange={(e) => setRecordingFilter(e.target.value)}
                                                className={`appearance-none text-base rounded-xl py-2.5 pl-4 pr-10 bg-gray-50 outline-none transition-all cursor-pointer ${recordingFilter !== 'all' ? 'border-2 border-blue-400 bg-blue-50 text-blue-700 font-bold shadow-sm' : 'border border-gray-200 text-gray-700 font-medium hover:bg-gray-100'}`}
                                            >
                                                <option value="all">All Recording</option>
                                                <option value="has_recording">Has Recording</option>
                                                <option value="no_recording">No Recording</option>
                                                <option value="pending_upload">Pending Upload</option>
                                            </select>
                                            <ChevronDown size={16} className={`absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none transition-colors ${recordingFilter !== 'all' ? 'text-blue-500' : 'text-gray-400 group-hover:text-gray-600'}`} />
                                        </div>
                                    );
                                    if (id === 'duration') return (
                                        <div key="duration" className="relative group">
                                            <select
                                                value={durationFilter}
                                                onChange={(e) => setDurationFilter(e.target.value)}
                                                className={`appearance-none text-sm rounded-xl py-2 pl-4 pr-10 bg-gray-50 outline-none transition-all cursor-pointer ${durationFilter !== 'all' ? 'border-2 border-blue-400 bg-blue-50 text-blue-700 font-bold shadow-sm' : 'border border-gray-200 text-gray-700 font-medium hover:bg-gray-100'}`}
                                            >
                                                <option value="all">Any Duration</option>
                                                <option value="under_30s">Under 30s</option>
                                                <option value="30s_to_1m">30s - 1min</option>
                                                <option value="1m_to_5m">1 - 5 min</option>
                                                <option value="over_5m">Over 5 min</option>
                                            </select>
                                            <ChevronDown size={14} className={`absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none transition-colors ${durationFilter !== 'all' ? 'text-blue-500' : 'text-gray-400 group-hover:text-gray-600'}`} />
                                        </div>
                                    );
                                    if (id === 'labels') return (
                                        <div key="labels" className="relative group">
                                            <select
                                                value={labelFilter}
                                                onChange={(e) => setLabelFilter(e.target.value)}
                                                className={`appearance-none text-sm rounded-xl py-2 pl-4 pr-10 bg-gray-50 outline-none transition-all cursor-pointer ${labelFilter !== 'all' ? 'border-2 border-blue-400 bg-blue-50 text-blue-700 font-bold shadow-sm' : 'border border-gray-200 text-gray-700 font-medium hover:bg-gray-100'}`}
                                            >
                                                <option value="all">All Labels</option>
                                                {availableLabels.map(label => (
                                                    <option key={label} value={label}>{label}</option>
                                                ))}
                                            </select>
                                            <ChevronDown size={14} className={`absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none transition-colors ${labelFilter !== 'all' ? 'text-blue-500' : 'text-gray-400 group-hover:text-gray-600'}`} />
                                        </div>
                                    );
                                    if (id === 'segments') return (
                                        <div key="segments" className="relative">
                                            <button
                                                ref={segmentsButtonRef}
                                                onClick={(e) => {
                                                    const rect = e.currentTarget.getBoundingClientRect();
                                                    setSegmentsMenuPosition({
                                                        top: rect.bottom + 8,
                                                        left: rect.left
                                                    });
                                                    setShowSegmentsDropdown(!showSegmentsDropdown);
                                                }}
                                                className={`flex items-center justify-between text-sm border rounded-xl py-2 px-4 outline-none transition-all font-medium min-w-[180px] group ${activeSegement ? 'bg-blue-600 text-white border-blue-600' : 'bg-gray-50 border-gray-200 text-gray-700 hover:bg-gray-100'}`}
                                            >
                                                <span className="truncate">{activeSegement ? activeSegement.name : 'Saved Segments'}</span>
                                                <ChevronDown size={14} className={`transition-transform ${showSegmentsDropdown ? 'rotate-180' : ''} ${activeSegement ? 'text-blue-100' : 'text-gray-400'}`} />
                                            </button>

                                            {showSegmentsDropdown && createPortal(
                                                <>
                                                    <div className="fixed inset-0 z-[190]" onClick={() => setShowSegmentsDropdown(false)} />
                                                    <div
                                                        style={{
                                                            position: 'fixed',
                                                            top: `${segmentsMenuPosition.top}px`,
                                                            left: `${segmentsMenuPosition.left}px`,
                                                            width: '288px' // w-72 equivalent
                                                        }}
                                                        className="bg-white rounded-2xl shadow-2xl border border-gray-100 py-2.5 z-[200] animate-in fade-in slide-in-from-top-2 duration-200"
                                                    >
                                                        <div className="px-4 pb-2 mb-1.5 border-b border-gray-50 flex items-center justify-between">
                                                            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest leading-none">Your Segments</p>
                                                            <span className="text-[10px] bg-gray-100 text-gray-500 px-1.5 py-0.5 rounded-full font-bold">{savedFilterSets.length}</span>
                                                        </div>
                                                        {savedFilterSets.length === 0 ? (
                                                            <div className="px-5 py-6 text-center">
                                                                <ListFilter size={24} className="mx-auto text-gray-200 mb-2" />
                                                                <p className="text-xs text-gray-400">Apply filters and save them as a segment to quickly access them later.</p>
                                                            </div>
                                                        ) : (
                                                            <div className="max-h-[300px] overflow-y-auto custom-scrollbar">
                                                                {savedFilterSets.map(set => (
                                                                    <div
                                                                        key={set.id}
                                                                        className={`group flex items-center justify-between px-2 mx-1 rounded-xl py-2 text-sm cursor-pointer transition-all ${activeSegement?.id === set.id ? 'bg-blue-50 text-blue-700' : 'text-gray-700 hover:bg-gray-50'}`}
                                                                        onClick={() => { applyFilterSet(set); setShowSegmentsDropdown(false); }}
                                                                    >
                                                                        <div className="flex items-center gap-2.5 flex-1 min-w-0">
                                                                            <div className={`w-1.5 h-1.5 rounded-full ${activeSegement?.id === set.id ? 'bg-blue-500' : 'bg-transparent group-hover:bg-gray-300'}`} />
                                                                            <span className="truncate flex-1 font-semibold">{set.name}</span>
                                                                        </div>
                                                                        <div className="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                                                                            <button
                                                                                onClick={(e) => renameFilterSet(set.id, e)}
                                                                                className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-100/50 rounded-lg transition-colors"
                                                                                title="Rename"
                                                                            >
                                                                                <Edit3 size={14} />
                                                                            </button>
                                                                            <button
                                                                                onClick={(e) => deleteFilterSet(set.id, e)}
                                                                                className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-100/50 rounded-lg transition-colors"
                                                                                title="Delete"
                                                                            >
                                                                                <Trash2 size={14} />
                                                                            </button>
                                                                        </div>
                                                                    </div>
                                                                ))}
                                                            </div>
                                                        )}
                                                        {activeSegement && (
                                                            <>
                                                                <div className="border-t border-gray-100 mt-1.5 pt-1.5 px-1">
                                                                    <button
                                                                        onClick={() => { setActiveSegement(null); setShowSegmentsDropdown(false); clearAllFilters(); }}
                                                                        className="w-full px-3 py-2 text-left text-xs text-red-500 hover:bg-red-50 rounded-xl transition-colors font-bold flex items-center gap-2"
                                                                    >
                                                                        <X size={14} />
                                                                        Clear active segment
                                                                    </button>
                                                                </div>
                                                            </>
                                                        )}
                                                    </div>
                                                </>,
                                                document.body
                                            )}
                                        </div>
                                    );
                                    return null;
                                })}

                                {/* Active Segment Chip */}

                                <button
                                    onClick={() => setShowAdvancedFilters(true)}
                                    className="whitespace-nowrap flex-shrink-0 flex items-center gap-2 px-4 py-2 text-sm font-bold text-gray-600 hover:text-blue-600 bg-gray-50 hover:bg-blue-50 rounded-xl border border-gray-100 transition-all group"
                                >
                                    <ListFilter size={16} className="group-hover:rotate-12 transition-transform" />
                                    Custom Filter
                                    {customFilters.length > 0 && (
                                        <span className="flex items-center justify-center w-5 h-5 text-[10px] bg-blue-600 text-white rounded-full shadow-sm">
                                            {customFilters.length}
                                        </span>
                                    )}
                                </button>
                            </div>

                            {/* Sticky Filter Settings Button */}
                            {/* Sticky Filter Settings Button + Clear Filters */}
                            <div className="absolute right-0 top-0 bottom-0 flex items-center gap-2 pr-5 pl-10 bg-gradient-to-l from-white via-white to-transparent pointer-events-auto">
                                <button
                                    onClick={() => setShowFilterCustomization(true)}
                                    className="p-2.5 text-gray-400 hover:text-blue-600 hover:bg-white bg-gray-50 rounded-xl transition-all shadow-sm border border-gray-100 group"
                                    title="Filter Settings"
                                >
                                    <Settings size={18} className="group-hover:rotate-90 transition-transform" />
                                </button>
                                <button
                                    onClick={() => setShowFilterBar(false)}
                                    className="p-2.5 text-gray-400 hover:text-red-500 hover:bg-red-50 bg-gray-50 rounded-xl transition-all shadow-sm border border-gray-100 group"
                                    title="Close Filters"
                                >
                                    <X size={18} />
                                </button>
                                {activeFilterCount > 0 && (
                                    <button
                                        onClick={clearAllFilters}
                                        className="whitespace-nowrap flex-shrink-0 flex items-center gap-1.5 px-3 py-2 text-xs font-bold text-red-600 bg-red-50 hover:bg-red-100 rounded-xl transition-all border border-red-100 shadow-sm"
                                    >
                                        <X size={12} />
                                        Clear Filters
                                    </button>
                                )}

                            </div>
                        </div>
                    )}

                    <Modal
                        isOpen={showAdvancedFilters}
                        onClose={() => setShowAdvancedFilters(false)}
                        title="Custom Logic Filters"
                        maxWidth="max-w-2xl"
                    >
                        <div className="space-y-6">
                            <div className="flex flex-col gap-4">
                                <div className="space-y-3 max-h-[50vh] overflow-y-auto px-1">
                                    {customFilters.length === 0 ? (
                                        <div className="text-center py-8 bg-gray-50 rounded-xl border border-dashed border-gray-200">
                                            <Filter className="mx-auto text-gray-300 mb-2" size={32} />
                                            <p className="text-sm text-gray-500">No custom filters added yet.</p>
                                            <button
                                                onClick={addCustomFilter}
                                                className="mt-3 text-sm text-blue-600 font-medium hover:underline"
                                            >
                                                Add your first rule
                                            </button>
                                        </div>
                                    ) : (
                                        customFilters.map((filter) => (
                                            <div key={filter.id} className="flex items-center gap-3 bg-gray-50 p-3 rounded-lg border border-gray-100">
                                                <select
                                                    value={filter.key}
                                                    onChange={(e) => updateCustomFilter(filter.id, { key: e.target.value })}
                                                    className="text-sm bg-white border border-gray-200 rounded-md px-3 py-2 outline-none focus:ring-1 focus:ring-blue-500 w-40"
                                                >
                                                    {filterableKeys.map(k => <option key={k.id} value={k.id}>{k.label}</option>)}
                                                </select>

                                                <select
                                                    value={filter.operator}
                                                    onChange={(e) => updateCustomFilter(filter.id, { operator: e.target.value })}
                                                    className="text-sm bg-white border border-gray-200 rounded-md px-3 py-2 outline-none focus:ring-1 focus:ring-blue-500 w-40"
                                                >
                                                    {filterOperators.map(op => <option key={op.id} value={op.id}>{op.label}</option>)}
                                                </select>

                                                {!['is_empty', 'is_not_empty'].includes(filter.operator) && (
                                                    <input
                                                        type="text"
                                                        value={filter.value}
                                                        onChange={(e) => updateCustomFilter(filter.id, { value: e.target.value })}
                                                        placeholder="Value..."
                                                        className="text-sm bg-white border border-gray-200 rounded-md px-3 py-2 flex-1 outline-none focus:ring-1 focus:ring-blue-500"
                                                    />
                                                )}

                                                <button
                                                    onClick={() => removeCustomFilter(filter.id)}
                                                    className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-md transition-all"
                                                >
                                                    <Trash2 size={16} />
                                                </button>
                                            </div>
                                        ))
                                    )}
                                </div>

                                {customFilters.length > 0 && (
                                    <button
                                        onClick={addCustomFilter}
                                        className="flex items-center gap-2 text-sm text-blue-600 hover:text-blue-700 font-medium px-4 py-3 hover:bg-blue-50 rounded-lg transition-all border border-dashed border-blue-200 w-full justify-center"
                                    >
                                        <Plus size={16} />
                                        Add Another Rule
                                    </button>
                                )}
                            </div>

                            <div className="pt-4 border-t border-gray-100 flex flex-col gap-5">
                                <div className="flex flex-col gap-2">
                                    <label className="text-sm font-semibold text-gray-500 uppercase tracking-wider">
                                        {activeSegement ? `Managing Segment: ${activeSegement.name}` : 'Save as Segment'}
                                    </label>
                                    <div className="flex gap-2">
                                        <input
                                            type="text"
                                            placeholder="Segment Name (e.g. Sales High Duration)"
                                            value={filterSetName}
                                            onChange={(e) => setFilterSetName(e.target.value)}
                                            className="flex-1 text-base px-4 py-2.5 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-blue-100"
                                        />
                                        <div className="flex gap-2">
                                            {activeSegement && (
                                                <button
                                                    onClick={updateFilterSet}
                                                    disabled={customFilters.length === 0 || !filterSetName.trim()}
                                                    className="flex items-center gap-2 px-6 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium transition-all shadow-sm disabled:opacity-50"
                                                >
                                                    <Save size={18} />
                                                    Update
                                                </button>
                                            )}
                                            <button
                                                onClick={saveFilterSet}
                                                disabled={customFilters.length === 0 || !filterSetName.trim()}
                                                className={`flex items-center gap-2 px-6 py-2.5 rounded-lg text-sm font-medium transition-all disabled:opacity-50 ${activeSegement ? 'bg-gray-100 hover:bg-gray-200 text-gray-700' : 'bg-blue-600 hover:bg-blue-700 text-white shadow-sm'}`}
                                            >
                                                {activeSegement ? 'Save as New' : 'Save'}
                                            </button>
                                        </div>
                                    </div>
                                </div>

                                <div className="flex gap-3">
                                    <button
                                        onClick={() => {
                                            setCustomFilters([]);
                                            setShowAdvancedFilters(false);
                                        }}
                                        className="flex-1 px-4 py-2.5 text-sm font-medium text-red-600 hover:bg-red-50 rounded-lg transition-colors border border-red-100"
                                    >
                                        Clear & Close
                                    </button>
                                    <button
                                        onClick={() => setShowAdvancedFilters(false)}
                                        className="flex-1 px-4 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium transition-all shadow-md shadow-blue-100"
                                    >
                                        Apply Filters
                                    </button>
                                </div>
                            </div>
                        </div>
                    </Modal>

                    <div className="flex justify-end px-2">
                        {/* This button was moved to the main header */}
                    </div>

                    <div className="card !p-0 overflow-hidden border border-gray-200 shadow-sm w-full max-w-full">
                        <div className="overflow-x-auto w-full">
                            <table className="w-full text-sm text-left table-fixed">
                                <thead className="bg-[#f8fafc] border-b-2 border-gray-100 text-gray-500 font-bold uppercase tracking-widest text-[11px] whitespace-nowrap sticky top-0 z-20 shadow-sm transition-all">
                                    <tr>
                                        {columnOrder.filter(id => visibleColumns.includes(id)).map(id => {
                                            const width = columnWidths[id] || 150;
                                            const isResizing = resizingRef.current?.columnId === id;

                                            const commonProps = {
                                                key: id,
                                                className: `relative px-6 py-4 cursor-pointer hover:bg-gray-100 transition-colors group select-none ${draggedColumn === id ? 'opacity-50 bg-gray-100' : ''}`,
                                                style: { width: `${width}px`, minWidth: `${width}px`, maxWidth: `${width}px` },
                                                draggable: true,
                                                onDragStart: (e) => handleColumnDragStart(e, id),
                                                onDragOver: (e) => handleColumnDragOver(e, id),
                                                onDragEnd: handleColumnDragEnd,
                                            };

                                            const ResizeHandle = () => (
                                                <div
                                                    className="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-blue-400 group-hover:bg-gray-300 transition-colors z-10"
                                                    onMouseDown={(e) => handleResizeStart(e, id)}
                                                    onClick={(e) => e.stopPropagation()}
                                                />
                                            );

                                            if (id === 'time') return (
                                                <th {...commonProps} onClick={() => handleSort('call_time')}>
                                                    <div className="flex items-center justify-between">
                                                        <div className="flex items-center gap-2 overflow-hidden"><Clock size={12} className="text-gray-400 shrink-0" /> Time {getSortIcon('call_time')}</div>
                                                    </div>
                                                    <ResizeHandle />
                                                </th>
                                            );
                                            if (id === 'contact') return (
                                                <th {...commonProps} onClick={() => handleSort('contact_name')}>
                                                    <div className="flex items-center justify-between">
                                                        <div className="flex items-center gap-2 overflow-hidden"><User size={12} className="text-gray-400 shrink-0" /> Contact {getSortIcon('contact_name')}</div>
                                                    </div>
                                                    <ResizeHandle />
                                                </th>
                                            );
                                            if (id === 'type') return (
                                                <th {...commonProps} onClick={() => handleSort('type')}>
                                                    <div className="flex items-center justify-between">
                                                        <div className="flex items-center gap-2 overflow-hidden"><Activity size={12} className="text-gray-400 shrink-0" /> Type {getSortIcon('type')}</div>
                                                    </div>
                                                    <ResizeHandle />
                                                </th>
                                            );
                                            if (id === 'person_note') return (
                                                <th {...commonProps} className={`${commonProps.className} !cursor-default`} onClick={undefined}>
                                                    <div className="flex items-center justify-between">
                                                        <span className="overflow-hidden flex items-center gap-2"><Edit3 size={12} className="text-gray-400 shrink-0" /> Person Note</span>
                                                    </div>
                                                    <ResizeHandle />
                                                </th>
                                            );
                                            if (id === 'note') return (
                                                <th {...commonProps} className={`${commonProps.className} !cursor-default`} onClick={undefined}>
                                                    <div className="flex items-center justify-between">
                                                        <span className="overflow-hidden flex items-center gap-2"><Tag size={12} className="text-gray-400 shrink-0" /> Call Note</span>
                                                    </div>
                                                    <ResizeHandle />
                                                </th>
                                            );
                                            if (id === 'recording') return (
                                                <th {...commonProps} onClick={() => handleSort('duration')}>
                                                    <div className="flex items-center justify-between">
                                                        <div className="flex items-center gap-2 overflow-hidden"><PlayCircle size={12} className="text-gray-400 shrink-0" /> Recording {getSortIcon('duration')}</div>
                                                    </div>
                                                    <ResizeHandle />
                                                </th>
                                            );
                                            if (id === 'labels') return (
                                                <th {...commonProps} className={`${commonProps.className} !cursor-default`} onClick={undefined}>
                                                    <div className="flex items-center justify-between">
                                                        <span className="overflow-hidden flex items-center gap-2"><Layers size={12} className="text-gray-400 shrink-0" /> Labels</span>
                                                    </div>
                                                    <ResizeHandle />
                                                </th>
                                            );
                                            if (id === 'employee') return (
                                                <th {...commonProps} onClick={() => handleSort('employee_name')}>
                                                    <div className="flex items-center justify-between">
                                                        <div className="flex items-center gap-2 overflow-hidden"><Briefcase size={12} className="text-gray-400 shrink-0" /> Employee {getSortIcon('employee_name')}</div>
                                                    </div>
                                                    <ResizeHandle />
                                                </th>
                                            );
                                            if (id === 'device_phone') return (
                                                <th {...commonProps} onClick={() => handleSort('phone_number')}>
                                                    <div className="flex items-center justify-between">
                                                        <div className="flex items-center gap-2 overflow-hidden"><Smartphone size={12} className="text-gray-400 shrink-0" /> Device Phone {getSortIcon('phone_number')}</div>
                                                    </div>
                                                    <ResizeHandle />
                                                </th>
                                            );
                                            return null;
                                        })}
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                    {calls.map((call, index) => (
                                        <tr
                                            key={call.id}
                                            className="hover:bg-blue-50/40 transition-all group cursor-pointer border-l-4 border-l-transparent hover:border-l-blue-500 py-6"
                                            onClick={() => {
                                                markCallClicked(call.id);
                                                openPersonModal(call);
                                            }}
                                        >
                                            {columnOrder.filter(id => visibleColumns.includes(id)).map(id => {
                                                if (id === 'time') return (
                                                    <td key="time" className="px-6 py-5 whitespace-nowrap">
                                                        <div className="flex flex-col gap-0.5">
                                                            <span className="text-sm font-semibold text-slate-700 leading-tight">
                                                                {format(new Date(call.call_time + (call.call_time.endsWith('Z') ? '' : 'Z')), 'h:mm a')}
                                                            </span>
                                                            <span className="text-xs text-gray-400 font-medium">
                                                                {format(new Date(call.call_time + (call.call_time.endsWith('Z') ? '' : 'Z')), 'MMM d, yyyy')}
                                                            </span>
                                                        </div>
                                                    </td>
                                                );
                                                if (id === 'contact') return (
                                                    <td key="contact" className="px-6 py-5 truncate">
                                                        <div className="flex items-center gap-3">
                                                            {contactColumnOptions.showReview && (
                                                                <button
                                                                    onClick={(e) => { e.stopPropagation(); handleToggleReviewed(call.id, call.reviewed); }}
                                                                    className="shrink-0 transition-all active:scale-95 group/review"
                                                                    title={Number(call.reviewed) ? "Mark as unreviewed" : "Mark as reviewed"}
                                                                >
                                                                    {Number(call.reviewed) ? (
                                                                        <div className="p-1 rounded-full bg-green-50 text-green-600 border border-green-100 shadow-sm">
                                                                            <CheckCircle2 size={16} fill="currentColor" fillOpacity={0.2} />
                                                                        </div>
                                                                    ) : (
                                                                        <div className="p-1 rounded-full bg-gray-50 text-gray-300 border border-gray-100 group-hover/review:text-green-500 group-hover/review:bg-green-50 transition-colors">
                                                                            <Circle size={16} />
                                                                        </div>
                                                                    )}
                                                                </button>
                                                            )}
                                                            <div className="min-w-0 flex-1">
                                                                <div className="font-semibold text-slate-700 truncate flex items-center gap-2 group-hover:text-blue-600 transition-colors text-sm" title={call.contact_name || call.phone_number}>
                                                                    <span className="truncate">{call.contact_name || ((!contactColumnOptions.showPhone && !call.contact_name) ? call.phone_number : (contactColumnOptions.showPhone ? (call.contact_name ? call.contact_name : call.phone_number) : 'Unknown'))}</span>
                                                                    {newArrivals.some(n => n.id === call.id) && (
                                                                        <span className="px-2 py-0.5 bg-blue-600 text-white text-[11px] font-bold rounded shadow-lg shadow-blue-200 uppercase tracking-tighter animate-pulse shrink-0">New</span>
                                                                    )}
                                                                </div>
                                                                {contactColumnOptions.showPhone && call.contact_name && (
                                                                    <div className="text-xs font-semibold text-gray-400 mt-1 tracking-tight group-hover:text-gray-500">{call.phone_number}</div>
                                                                )}
                                                            </div>
                                                        </div>
                                                    </td>
                                                );
                                                if (id === 'type') return (
                                                    <td key="type" className="px-6 py-4">
                                                        <CallTypeBadge type={call.type} duration={call.duration} showDuration={contactColumnOptions.showDuration} />
                                                    </td>
                                                );
                                                if (id === 'person_note') return (
                                                    <td
                                                        key="person_note"
                                                        className="px-6 py-4 border-l border-transparent hover:border-blue-200"
                                                        onClick={(e) => { e.stopPropagation(); setEditingNote({ call, field: 'person_note' }); }}
                                                    >
                                                        {call.person_note ? (
                                                            <span className="text-gray-700 block max-w-[150px] truncate" title={call.person_note}>{call.person_note}</span>
                                                        ) : (
                                                            <span className="text-gray-400 italic text-xs hover:text-blue-500 transition-colors">Add note...</span>
                                                        )}
                                                    </td>
                                                );
                                                if (id === 'note') return (
                                                    <td
                                                        key="note"
                                                        className="px-6 py-4 border-l border-transparent hover:border-blue-200"
                                                        onClick={(e) => { e.stopPropagation(); setEditingNote({ call, field: 'note' }); }}
                                                    >
                                                        {call.note ? (
                                                            <span className="text-gray-700 block max-w-[150px] truncate" title={call.note}>{call.note}</span>
                                                        ) : (
                                                            <span className="text-gray-400 italic text-xs hover:text-blue-500 transition-colors">Add note...</span>
                                                        )}
                                                    </td>
                                                );
                                                if (id === 'recording') return (
                                                    <td key="recording" className="px-6 py-4" onClick={(e) => e.stopPropagation()}>
                                                        {call.recording_url ? (
                                                            <button
                                                                onClick={() => playRecording(call)}
                                                                className={`flex items-center gap-3 px-4 py-2 rounded-xl border transition-all group/player shadow-sm ${currentCall?.id === call.id ? 'bg-blue-600 border-blue-600 text-white shadow-blue-200 scale-105' : 'bg-white border-gray-200 hover:border-blue-400 hover:bg-blue-50 text-gray-700 hover:shadow-md'}`}
                                                                title="Play Recording"
                                                            >
                                                                <div className={`w-7 h-7 rounded-lg flex items-center justify-center transition-colors ${currentCall?.id === call.id ? 'bg-white/20' : 'bg-blue-100 text-blue-600 group-hover/player:bg-blue-600 group-hover/player:text-white'}`}>
                                                                    {currentCall?.id === call.id && isPlaying ? <Pause size={12} fill="currentColor" /> : <Play size={12} fill="currentColor" className={currentCall?.id === call.id ? '' : 'ml-0.5'} />}
                                                                </div>
                                                                <span className={`text-sm font-bold font-mono tracking-tight ${currentCall?.id === call.id ? 'text-white' : 'text-gray-600 group-hover/player:text-blue-700'}`}>
                                                                    {currentCall?.id === call.id ? (
                                                                        `${Math.floor(currentTime / 60)}:${String(Math.floor(currentTime % 60)).padStart(2, '0')}`
                                                                    ) : (
                                                                        call.duration > 0 ? (
                                                                            call.duration >= 60 ? `${Math.floor(call.duration / 60)}:${String(call.duration % 60).padStart(2, '0')}` : `0:${String(call.duration).padStart(2, '0')}`
                                                                        ) : '0:00'
                                                                    )}
                                                                </span>
                                                            </button>
                                                        ) : (
                                                            call.duration === 0 || call.duration === '0' ? (
                                                                <span className="text-gray-300">-</span>
                                                            ) : call.upload_status === 'pending' ? (
                                                                <span className="text-xs text-amber-500 italic flex items-center gap-1">
                                                                    <span className="w-1.5 h-1.5 bg-amber-400 rounded-full animate-pulse"></span>
                                                                    Pending Upload
                                                                </span>
                                                            ) : (
                                                                <span className="text-xs text-gray-300 italic">No recording</span>
                                                            )
                                                        )}
                                                    </td>
                                                );
                                                if (id === 'labels') return (
                                                    <td key="labels" className="px-6 py-4" onClick={(e) => e.stopPropagation()}>
                                                        <LabelCell call={call} onUpdate={handleUpdateLabels} />
                                                    </td>
                                                );
                                                if (id === 'employee') return (
                                                    <td key="employee" className="px-6 py-4 whitespace-nowrap">
                                                        <div className="flex items-center gap-2">
                                                            <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-blue-50 to-blue-100 flex items-center justify-center text-xs font-extrabold text-blue-600 border border-blue-200 uppercase shadow-sm">
                                                                {(call.employee_name || 'S')[0]}
                                                            </div>
                                                            <span className="text-sm font-bold text-gray-700">{call.employee_name || 'System'}</span>
                                                        </div>
                                                    </td>
                                                );
                                                if (id === 'device_phone') return (
                                                    <td key="device_phone" className="px-6 py-4 whitespace-nowrap">
                                                        <div className="flex items-center gap-2 text-gray-500 font-bold text-xs tracking-tight">
                                                            <div className="p-1 rounded-md bg-gray-50 border border-gray-100">
                                                                <Smartphone size={12} className="text-gray-400" />
                                                            </div>
                                                            {call.device_phone || '-'}
                                                        </div>
                                                    </td>
                                                );
                                                return null;
                                            })}
                                        </tr>
                                    ))}

                                    {loading && calls.length === 0 && (
                                        <tr><td colSpan={visibleColumns.length} className="p-8 text-center text-gray-500">Loading...</td></tr>
                                    )}

                                    {!loading && calls.length === 0 && (
                                        <tr><td colSpan={visibleColumns.length} className="p-8 text-center text-gray-500">No calls found</td></tr>
                                    )}
                                </tbody>
                            </table>
                        </div>

                        {/* Pagination */}
                        <div className="flex items-center justify-between p-4 border-t border-gray-100 bg-[#f8fafc]">
                            <div className="text-sm text-gray-500">
                                Showing <span className="font-medium">{calls.length}</span> of <span className="font-medium">{pagination.total}</span> results
                            </div>
                            <div className="flex items-center gap-2">
                                <button
                                    onClick={() => handlePageChange(pagination.page - 1)}
                                    disabled={pagination.page === 1}
                                    className="p-2 rounded-lg hover:bg-white border border-transparent hover:border-gray-200 disabled:opacity-50 transition-all text-gray-500"
                                >
                                    <ChevronLeft size={18} />
                                </button>
                                <span className="text-sm font-medium px-2">
                                    Page {pagination.page} of {pagination.total_pages}
                                </span>
                                <button
                                    onClick={() => handlePageChange(pagination.page + 1)}
                                    disabled={pagination.page === pagination.total_pages}
                                    className="p-2 rounded-lg hover:bg-white border border-transparent hover:border-gray-200 disabled:opacity-50 transition-all text-gray-500"
                                >
                                    <ChevronRight size={18} />
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* Simple Note Modal */}
                    {
                        editingNote && (
                            <SimpleNoteModal
                                noteData={editingNote}
                                onClose={() => setEditingNote(null)}
                                onUpdate={handleUpdateCall}
                            />
                        )
                    }

                    {/* Customize Filter Modal */}
                    {
                        showFilterCustomization && (
                            <CustomizeViewModal
                                onClose={() => setShowFilterCustomization(false)}
                                initialTab="filters"
                                hideTabs={true}
                                columnOrder={columnOrder}
                                setColumnOrder={setColumnOrder}
                                visibleColumns={visibleColumns}
                                setVisibleColumns={setVisibleColumns}
                                defaultColumnOrder={defaultColumnOrder}
                                columnLabels={columnLabels}
                                filterOrder={filterOrder}
                                setFilterOrder={setFilterOrder}
                                visibleFilters={visibleFilters}
                                setVisibleFilters={setVisibleFilters}
                                defaultFilterOrder={defaultFilterOrder}
                                filterLabels={filterLabels}
                                handleColumnDragStart={handleColumnDragStart}
                                handleColumnDragOver={handleColumnDragOver}
                                handleColumnDragEnd={handleColumnDragEnd}
                                handleFilterDragStart={handleFilterDragStart}
                                handleFilterDragOver={handleFilterDragOver}
                                handleFilterDragEnd={handleFilterDragEnd}
                                toggleColumn={toggleColumn}
                                toggleFilter={toggleFilter}
                                draggedColumn={draggedColumn}
                                draggedFilter={draggedFilter}
                            />
                        )
                    }

                    {/* Customize Column Modal */}
                    {
                        showColumnCustomization && (
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
                                filterOrder={filterOrder}
                                setFilterOrder={setFilterOrder}
                                visibleFilters={visibleFilters}
                                setVisibleFilters={setVisibleFilters}
                                defaultFilterOrder={defaultFilterOrder}
                                filterLabels={filterLabels}
                                handleColumnDragStart={handleColumnDragStart}
                                handleColumnDragOver={handleColumnDragOver}
                                handleColumnDragEnd={handleColumnDragEnd}
                                handleFilterDragStart={handleFilterDragStart}
                                handleFilterDragOver={handleFilterDragOver}
                                handleFilterDragEnd={handleFilterDragEnd}
                                toggleColumn={toggleColumn}
                                toggleFilter={toggleFilter}
                                draggedColumn={draggedColumn}
                                draggedFilter={draggedFilter}
                                contactColumnOptions={contactColumnOptions}
                                setContactColumnOptions={setContactColumnOptions}
                            />
                        )
                    }
                </div>
            </div>
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

            {/* Prompt Modal */}
            <Modal
                isOpen={promptModal.isOpen}
                onClose={() => setPromptModal({ ...promptModal, isOpen: false })}
                title={promptModal.title}
                maxWidth="max-w-sm"
            >
                <div className="space-y-4">
                    <p className="text-sm font-medium text-gray-600">{promptModal.message}</p>
                    <input
                        type="text"
                        value={promptModal.value}
                        onChange={(e) => setPromptModal({ ...promptModal, value: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-blue-500"
                        autoFocus
                        onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                                promptModal.onConfirm(promptModal.value);
                                setPromptModal({ ...promptModal, isOpen: false });
                            }
                        }}
                    />
                    <div className="flex gap-3 pt-2">
                        <button
                            onClick={() => setPromptModal({ ...promptModal, isOpen: false })}
                            className="flex-1 px-4 py-2 border border-gray-200 text-gray-600 rounded-lg hover:bg-gray-50 font-medium"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={() => {
                                promptModal.onConfirm(promptModal.value);
                                setPromptModal({ ...promptModal, isOpen: false });
                            }}
                            className="flex-1 btn btn-primary"
                        >
                            Save
                        </button>
                    </div>
                </div>
            </Modal>
        </div >
    );
}

const filterLabels = {
    employee: 'Employee',
    date: 'Date Range',
    type: 'Call Type',
    connected: 'Connection',
    status: 'Review Status',
    notes: 'Notes Filter',
    recording: 'Recording',
    duration: 'Duration',
    labels: 'Labels',
    segments: 'Segments',
    name: 'Name',
};

function CustomizeViewModal({
    onClose,
    initialTab = 'columns',
    hideTabs = false,
    columnOrder, setColumnOrder, visibleColumns, setVisibleColumns, defaultColumnOrder, columnLabels,
    filterOrder, setFilterOrder, visibleFilters, setVisibleFilters, defaultFilterOrder, filterLabels,
    handleColumnDragStart, handleColumnDragOver, handleColumnDragEnd,
    handleFilterDragStart, handleFilterDragOver, handleFilterDragEnd,
    toggleColumn, toggleFilter, draggedColumn, draggedFilter,
    contactColumnOptions, setContactColumnOptions
}) {
    const [activeTab, setActiveTab] = useState(initialTab);

    return createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm" onClick={onClose}>
            <div
                className="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in-95 duration-200"
                onClick={e => e.stopPropagation()}
            >
                <div className="bg-gray-50 px-6 py-4 border-b border-gray-100 flex justify-between items-center">
                    <div>
                        <h3 className="font-bold text-gray-900 text-lg">Customize {activeTab === 'columns' ? 'Columns' : 'Filters'}</h3>
                        <p className="text-sm text-gray-500">{activeTab === 'columns' ? 'Configure table column visibility and order' : 'Configure filters bar visibility and order'}</p>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-gray-200 rounded-full transition-colors">
                        <X size={20} className="text-gray-500" />
                    </button>
                </div>

                {!hideTabs && (
                    <div className="flex border-b border-gray-100">
                        <button
                            onClick={() => setActiveTab('columns')}
                            className={`flex-1 py-3 text-sm font-semibold transition-all border-b-2 ${activeTab === 'columns' ? 'text-blue-600 border-blue-600' : 'text-gray-500 border-transparent hover:text-gray-700'}`}
                        >
                            Columns
                        </button>
                        <button
                            onClick={() => setActiveTab('filters')}
                            className={`flex-1 py-3 text-sm font-semibold transition-all border-b-2 ${activeTab === 'filters' ? 'text-blue-600 border-blue-600' : 'text-gray-500 border-transparent hover:text-gray-700'}`}
                        >
                            Filters Bar
                        </button>
                    </div>
                )}

                <div className="p-6 max-h-[60vh] overflow-y-auto custom-scrollbar">
                    {activeTab === 'columns' ? (
                        <div className="space-y-1">
                            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-3 px-2">Drag to reorder • Toggle visibility</p>
                            {columnOrder.map((id) => (
                                <div key={id} className="flex flex-col">
                                    <div
                                        draggable
                                        onDragStart={(e) => handleColumnDragStart(e, id)}
                                        onDragOver={(e) => handleColumnDragOver(e, id)}
                                        onDragEnd={handleColumnDragEnd}
                                        className={`flex items-center gap-3 px-3 py-2.5 hover:bg-gray-50 rounded-xl transition-all group ${draggedColumn === id ? 'opacity-40 bg-gray-100' : ''}`}
                                    >
                                        <div className="cursor-grab active:cursor-grabbing text-gray-300 group-hover:text-gray-500 transition-colors">
                                            <GripVertical size={18} />
                                        </div>
                                        <div className="flex-1 flex items-center justify-between">
                                            <span className="text-sm font-semibold text-gray-700">{columnLabels[id]}</span>
                                            <button
                                                onClick={() => toggleColumn(id)}
                                                className={`p-1.5 rounded-lg transition-all ${visibleColumns.includes(id) ? 'bg-blue-50 text-blue-600' : 'bg-gray-100 text-gray-400'}`}
                                                title={visibleColumns.includes(id) ? "Hide Column" : "Show Column"}
                                            >
                                                {visibleColumns.includes(id) ? <Eye size={18} /> : <EyeOff size={18} />}
                                            </button>
                                        </div>
                                    </div>
                                    {id === 'contact' && visibleColumns.includes('contact') && (
                                        <div className="ml-12 mr-3 my-1 space-y-2 border-l-2 border-gray-100 pl-4 py-1">
                                            <div className="flex items-center justify-between">
                                                <span className="text-xs font-medium text-gray-500">Show Phone Number</span>
                                                <button
                                                    onClick={() => setContactColumnOptions(prev => ({ ...prev, showPhone: !prev.showPhone }))}
                                                    className={`relative inline-flex h-4 w-7 items-center rounded-full transition-colors focus:outline-none ${contactColumnOptions?.showPhone ? 'bg-blue-600' : 'bg-gray-200'}`}
                                                >
                                                    <span
                                                        className={`inline-block h-3 w-3 transform rounded-full bg-white transition-transform ${contactColumnOptions?.showPhone ? 'translate-x-[14px]' : 'translate-x-[2px]'}`}
                                                    />
                                                </button>
                                            </div>
                                            <div className="flex items-center justify-between">
                                                <span className="text-xs font-medium text-gray-500">Show Review Status</span>
                                                <button
                                                    onClick={() => setContactColumnOptions(prev => ({ ...prev, showReview: !prev.showReview }))}
                                                    className={`relative inline-flex h-4 w-7 items-center rounded-full transition-colors focus:outline-none ${contactColumnOptions?.showReview ? 'bg-blue-600' : 'bg-gray-200'}`}
                                                >
                                                    <span
                                                        className={`inline-block h-3 w-3 transform rounded-full bg-white transition-transform ${contactColumnOptions?.showReview ? 'translate-x-[14px]' : 'translate-x-[2px]'}`}
                                                    />
                                                </button>
                                            </div>
                                        </div>
                                    )}

                                    {id === 'type' && visibleColumns.includes('type') && (
                                        <div className="ml-12 mr-3 my-1 space-y-2 border-l-2 border-gray-100 pl-4 py-1">
                                            <div className="flex items-center justify-between">
                                                <span className="text-xs font-medium text-gray-500">Show Duration</span>
                                                <button
                                                    onClick={() => setContactColumnOptions(prev => ({ ...prev, showDuration: !prev.showDuration }))}
                                                    className={`relative inline-flex h-4 w-7 items-center rounded-full transition-colors focus:outline-none ${contactColumnOptions?.showDuration ? 'bg-blue-600' : 'bg-gray-200'}`}
                                                >
                                                    <span
                                                        className={`inline-block h-3 w-3 transform rounded-full bg-white transition-transform ${contactColumnOptions?.showDuration ? 'translate-x-[14px]' : 'translate-x-[2px]'}`}
                                                    />
                                                </button>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="space-y-1">
                            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-3 px-2">Drag to reorder • Toggle visibility</p>
                            {filterOrder.filter(id => id !== 'date').map((id) => (
                                <div
                                    key={id}
                                    draggable
                                    onDragStart={(e) => handleFilterDragStart(e, id)}
                                    onDragOver={(e) => handleFilterDragOver(e, id)}
                                    onDragEnd={handleFilterDragEnd}
                                    className={`flex items-center gap-3 px-3 py-2.5 hover:bg-gray-50 rounded-xl transition-all group ${draggedFilter === id ? 'opacity-40 bg-gray-100' : ''}`}
                                >
                                    <div className="cursor-grab active:cursor-grabbing text-gray-300 group-hover:text-gray-500 transition-colors">
                                        <GripVertical size={18} />
                                    </div>
                                    <div className="flex-1 flex items-center justify-between">
                                        <span className="text-sm font-semibold text-gray-700">{filterLabels[id]}</span>
                                        <button
                                            onClick={() => toggleFilter(id)}
                                            className={`p-1.5 rounded-lg transition-all ${visibleFilters.includes(id) ? 'bg-blue-50 text-blue-600' : 'bg-gray-100 text-gray-400'}`}
                                            title={visibleFilters.includes(id) ? "Hide Filter" : "Show Filter"}
                                        >
                                            {visibleFilters.includes(id) ? <Eye size={18} /> : <EyeOff size={18} />}
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <div className="px-6 py-4 bg-gray-50 border-t border-gray-100 flex justify-between items-center">
                    <button
                        onClick={() => {
                            if (activeTab === 'columns') {
                                setColumnOrder(defaultColumnOrder);
                                setVisibleColumns(defaultColumnOrder);
                            } else {
                                setFilterOrder(defaultFilterOrder);
                                setVisibleFilters(defaultFilterOrder);
                            }
                            toast.success("Reset to defaults");
                        }}
                        className="text-xs font-bold text-gray-400 hover:text-gray-600 uppercase tracking-widest hover:underline"
                    >
                        Reset Defaults
                    </button>
                    <button
                        onClick={onClose}
                        className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-xl text-sm font-bold shadow-lg shadow-blue-200 transition-all active:scale-95"
                    >
                        Save Changes
                    </button>
                </div>
            </div>
        </div >,
        document.body
    );
}

function SimpleNoteModal({ noteData, onClose, onUpdate }) {
    const { call, field } = noteData;
    const [value, setValue] = useState(call[field] || '');
    const [isSaving, setIsSaving] = useState(false);
    const inputRef = useRef(null);

    useEffect(() => {
        if (inputRef.current) {
            inputRef.current.focus();
            const len = inputRef.current.value.length;
            inputRef.current.setSelectionRange(len, len);
        }
    }, []);

    const handleSave = async () => {
        if (isSaving) return;
        setIsSaving(true);
        console.log('SimpleNoteModal: Saving note...', { callId: call.id, field, value });
        try {
            await onUpdate(call.id, { [field]: value });
            console.log('SimpleNoteModal: Saved successfully');
            onClose();
        } catch (error) {
            console.error('SimpleNoteModal: Save failed', error);
            // Error handling is likely done in onUpdate (handleUpdateCall) with toast
            setIsSaving(false);
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSave();
        }
        if (e.key === 'Escape') onClose();
    };

    const label = field === 'person_note' ? 'Person Note' : 'Call Note';

    return createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/20 backdrop-blur-[1px]" onClick={onClose}>
            <div
                className="bg-white rounded-xl shadow-2xl w-full max-w-sm overflow-hidden animate-in fade-in zoom-in-95 duration-100"
                onClick={e => e.stopPropagation()}
            >
                <div className="bg-gray-50 px-4 py-3 border-b border-gray-100 flex justify-between items-center">
                    <div>
                        <h3 className="font-semibold text-gray-900 text-sm truncate max-w-[180px]">{call.contact_name || 'Unknown Contact'}</h3>
                        <p className="text-xs text-gray-500 font-mono">{call.phone_number}</p>
                    </div>
                    <span className="text-[10px] font-bold text-blue-600 uppercase tracking-wider bg-blue-50 px-2 py-1 rounded-full">{label}</span>
                </div>
                <div className="p-4">
                    <textarea
                        ref={inputRef}
                        className="w-full text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 p-3 min-h-[100px] outline-none resize-none disabled:bg-gray-50 disabled:text-gray-500"
                        value={value}
                        onChange={e => setValue(e.target.value)}
                        onKeyDown={handleKeyDown}
                        placeholder={`Enter ${label.toLowerCase()}...`}
                        disabled={isSaving}
                    />
                    <div className="mt-2 text-xs text-gray-400 flex justify-between items-center">
                        <span>Press <strong>Enter</strong> to save</span>
                        <button
                            onClick={handleSave}
                            disabled={isSaving}
                            className={`px-3 py-1 rounded text-xs font-medium transition-colors ${isSaving ? 'bg-gray-300 text-gray-500 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700 text-white'}`}
                        >
                            {isSaving ? 'Saving...' : 'Save'}
                        </button>
                    </div>
                </div>
            </div>
        </div>,
        document.body
    );
}
