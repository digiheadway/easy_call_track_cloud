import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import { X, GripVertical, Eye, EyeOff } from 'lucide-react';
import { toast } from 'sonner';

export default function CustomizeViewModal({
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
                                    {id === 'contact' && visibleColumns.includes('contact') && contactColumnOptions && (
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

                                    {id === 'type' && visibleColumns.includes('type') && contactColumnOptions && (
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
                            {filterOrder && filterOrder.filter(id => id !== 'date').map((id) => (
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
