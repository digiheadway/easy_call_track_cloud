import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Settings, X, GripVertical, Check } from 'lucide-react';

// Default date range options
const ALL_DATE_OPTIONS = [
    { id: 'today', label: 'Today', value: 'today' },
    { id: 'yesterday', label: 'Yesterday', value: 'yesterday' },
    { id: '3days', label: 'Last 3 Days', value: '3days' },
    { id: '7days', label: 'Last 7 Days', value: '7days' },
    { id: 'this_week', label: 'This Week', value: 'this_week' },
    { id: 'last_week', label: 'Last Week', value: 'last_week' },
    { id: 'this_month', label: 'This Month', value: 'this_month' },
    { id: 'last_month', label: 'Last Month', value: 'last_month' },
    { id: '30days', label: 'Last 30 Days', value: '30days' },
    { id: '60days', label: 'Last 60 Days', value: '60days' },
    { id: '90days', label: 'Last 90 Days', value: '90days' },
    { id: '180days', label: 'Last 180 Days', value: '180days' },
    { id: 'all_time', label: 'All Time', value: 'all_time' },
    { id: 'custom', label: 'Custom', value: 'custom' },
];

// Global storage key for date filter preferences (same across all screens)
const STORAGE_KEY = 'dateFilter_global';

// Get stored preferences or defaults
const getStoredPreferences = () => {
    try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
            return JSON.parse(stored);
        }
    } catch (e) {
        console.error('Failed to parse stored preferences', e);
    }
    // Default: show Today, Yesterday, Last 3 Days, Last 7 Days, Last 14 Days, Last 30 Days, Custom
    return ['today', 'yesterday', '3days', '7days', '14days', '30days', 'custom'];
};

const savePreferences = (enabledOptions) => {
    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(enabledOptions));
        // Dispatch custom event so other components can react to the change
        window.dispatchEvent(new CustomEvent('dateFilterPrefsChanged', { detail: enabledOptions }));
    } catch (e) {
        console.error('Failed to save preferences', e);
    }
};

export default function DateRangeFilter({
    value,
    onChange,
    customRange,
    onCustomRangeChange,
    showSettingsButton = true,
}) {
    const [enabledOptions, setEnabledOptions] = useState(() => getStoredPreferences());
    const [showSettings, setShowSettings] = useState(false);

    // Listen for preference changes from other components
    useEffect(() => {
        const handlePrefsChange = (e) => {
            setEnabledOptions(e.detail);
        };
        window.addEventListener('dateFilterPrefsChanged', handlePrefsChange);
        return () => window.removeEventListener('dateFilterPrefsChanged', handlePrefsChange);
    }, []);

    // Get visible options based on enabled list
    const visibleOptions = ALL_DATE_OPTIONS.filter(opt => enabledOptions.includes(opt.id));

    const handleSettingsSave = (newEnabled) => {
        setEnabledOptions(newEnabled);
        savePreferences(newEnabled);
        setShowSettings(false);
    };

    return (
        <div className="flex flex-wrap items-center gap-2">
            {value === 'custom' ? (
                // Custom Mode: Inputs + Close Button
                <div className="flex items-center gap-2 bg-white dark:bg-gray-800 p-1.5 rounded-lg border border-gray-200 dark:border-gray-700 animate-in fade-in slide-in-from-left-2 duration-200">
                    <span className="text-xs font-semibold text-blue-600 dark:text-blue-400 px-2 uppercase tracking-wider">Custom Range</span>
                    <div className="w-px h-4 bg-gray-200 dark:bg-gray-700"></div>
                    {onCustomRangeChange && (
                        <>
                            <input
                                type="date"
                                className="text-sm bg-transparent border-none outline-none text-gray-700 dark:text-gray-300 px-2 py-1 font-medium hover:bg-gray-50 dark:hover:bg-gray-700 rounded cursor-pointer"
                                value={customRange?.startDate || ''}
                                onChange={(e) => onCustomRangeChange({ ...customRange, startDate: e.target.value })}
                            />
                            <span className="text-gray-400 font-medium">–</span>
                            <input
                                type="date"
                                className="text-sm bg-transparent border-none outline-none text-gray-700 dark:text-gray-300 px-2 py-1 font-medium hover:bg-gray-50 dark:hover:bg-gray-700 rounded cursor-pointer"
                                value={customRange?.endDate || ''}
                                onChange={(e) => onCustomRangeChange({ ...customRange, endDate: e.target.value })}
                            />
                        </>
                    )}
                    <div className="w-px h-4 bg-gray-200 dark:bg-gray-700 mx-1"></div>
                    <button
                        onClick={() => onChange('3days')}
                        className="p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-md transition-colors"
                        title="Clear custom range"
                    >
                        <X size={16} />
                    </button>
                </div>
            ) : (
                <div className="flex bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-1 gap-1 overflow-x-auto">
                    {visibleOptions.map((opt) => (
                        <button
                            key={opt.id}
                            onClick={() => onChange(opt.value)}
                            className={`whitespace-nowrap px-3 py-1.5 rounded-md text-sm font-medium transition-all ${value === opt.value
                                ? 'bg-blue-600 text-white shadow-sm'
                                : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
                                }`}
                        >
                            {opt.label}
                        </button>
                    ))}

                    {/* Settings Button */}
                    {showSettingsButton && (
                        <button
                            onClick={() => setShowSettings(true)}
                            className="p-1.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md transition-colors ml-1"
                            title="Customize date options"
                        >
                            <Settings size={16} />
                        </button>
                    )}
                </div>
            )}

            {/* Settings Modal */}
            {showSettings && (
                <DateFilterSettingsModal
                    enabledOptions={enabledOptions}
                    onSave={handleSettingsSave}
                    onClose={() => setShowSettings(false)}
                />
            )}
        </div>
    );
}

function DateFilterSettingsModal({ enabledOptions, onSave, onClose }) {
    const [selected, setSelected] = useState([...enabledOptions]);

    const toggleOption = (id) => {
        setSelected(prev => {
            if (prev.includes(id)) {
                // Don't allow removing all options
                if (prev.length <= 1) return prev;
                return prev.filter(x => x !== id);
            } else {
                return [...prev, id];
            }
        });
    };

    const handleSave = () => {
        onSave(selected);
    };

    return createPortal(
        <div
            className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
            onClick={onClose}
        >
            <div
                className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full max-w-sm overflow-hidden animate-in fade-in zoom-in-95 duration-150 border dark:border-gray-700"
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
                    <div>
                        <h3 className="font-semibold text-gray-900 dark:text-gray-100">Customize Date Filters</h3>
                        <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">Settings apply to all screens</p>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                    >
                        <X size={18} />
                    </button>
                </div>

                {/* Options List */}
                <div className="p-4 space-y-2 max-h-[400px] overflow-y-auto">
                    {ALL_DATE_OPTIONS.map((opt) => {
                        const isEnabled = selected.includes(opt.id);
                        return (
                            <button
                                key={opt.id}
                                onClick={() => toggleOption(opt.id)}
                                className={`w-full flex items-center gap-3 p-3 rounded-lg border transition-all ${isEnabled
                                    ? 'border-blue-200 bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-400'
                                    : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700'
                                    }`}
                            >
                                <div className={`w-5 h-5 rounded-md flex items-center justify-center flex-shrink-0 ${isEnabled ? 'bg-blue-600' : 'border-2 border-gray-300 dark:border-gray-500'
                                    }`}>
                                    {isEnabled && <Check size={14} className="text-white" />}
                                </div>
                                <span className="font-medium text-sm">{opt.label}</span>
                            </button>
                        );
                    })}
                </div>

                {/* Footer */}
                <div className="flex gap-3 p-4 border-t border-gray-100 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
                    <button
                        onClick={onClose}
                        className="flex-1 px-4 py-2.5 text-sm font-medium text-gray-700 dark:text-gray-200 bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleSave}
                        className="flex-1 px-4 py-2.5 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
                    >
                        Save Preferences
                    </button>
                </div>
            </div>
        </div>,
        document.body
    );
}
