import React, { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';

export default function SimpleNoteModal({
    title,
    subTitle,
    initialValue = '',
    onSave,
    onClose,
    label = 'Note',
    placeholder = 'Enter note...'
}) {
    const [value, setValue] = useState(initialValue);
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
        try {
            await onSave(value);
            onClose();
        } catch (error) {
            console.error('SimpleNoteModal: Save failed', error);
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

    return createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/20 backdrop-blur-[1px]" onClick={onClose}>
            <div
                className="bg-white rounded-xl shadow-2xl w-full max-w-sm overflow-hidden animate-in fade-in zoom-in-95 duration-100"
                onClick={e => e.stopPropagation()}
            >
                <div className="bg-gray-50 px-4 py-3 border-b border-gray-100 flex justify-between items-center">
                    <div>
                        <h3 className="font-semibold text-gray-900 text-sm truncate max-w-[180px]">{title}</h3>
                        {subTitle && <p className="text-xs text-gray-500 font-mono">{subTitle}</p>}
                    </div>
                    {label && <span className="text-[10px] font-bold text-blue-600 uppercase tracking-wider bg-blue-50 px-2 py-1 rounded-full">{label}</span>}
                </div>
                <div className="p-4">
                    <textarea
                        ref={inputRef}
                        className="w-full text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 p-3 min-h-[100px] outline-none resize-none disabled:bg-gray-50 disabled:text-gray-500"
                        value={value}
                        onChange={e => setValue(e.target.value)}
                        onKeyDown={handleKeyDown}
                        placeholder={placeholder}
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
