import React, { useState, useEffect, useRef } from 'react';
import { Check, X } from 'lucide-react';
import toast from 'react-hot-toast';
import { formatDateTime } from '../utils/dateFormat';
import { TagInput } from './TagInput';

interface InlineEditProps {
  value: string | number | null;
  onSave: (value: string | null) => void;
  type?: 'text' | 'number' | 'select' | 'datetime' | 'tags';
  options?: string[];
  className?: string;
  placeholder?: string;
}

export const InlineEdit: React.FC<InlineEditProps> = ({
  value,
  onSave,
  type = 'text',
  options = [],
  className = '',
  placeholder = '-'
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editValue, setEditValue] = useState(value?.toString() || '');
  const [isSaving, setIsSaving] = useState(false);
  const inputRef = useRef<HTMLInputElement | HTMLSelectElement>(null);
  const previousValue = useRef(value?.toString() || '');

  useEffect(() => {
    if (isEditing && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isEditing]);

  useEffect(() => {
    setEditValue(value?.toString() || '');
    previousValue.current = value?.toString() || '';
  }, [value]);

  const handleSave = async () => {
    if (editValue === previousValue.current) {
      setIsEditing(false);
      return;
    }

    try {
      setIsSaving(true);
      await onSave(editValue.trim() === '' ? null : editValue);
      previousValue.current = editValue;
      setIsEditing(false);
      toast.success('Updated successfully', { duration: 2000 });
    } catch (error) {
      setEditValue(previousValue.current);
      toast.error('Failed to update. Please try again.', { duration: 5000 });
    } finally {
      setIsSaving(false);
    }
  };

  const handleCancel = () => {
    setEditValue(previousValue.current);
    setIsEditing(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSave();
    } else if (e.key === 'Escape') {
      handleCancel();
    }
  };

  const renderValue = () => {
    if (!value) {
      return <span className="text-gray-400">{placeholder}</span>;
    }

    if (type === 'datetime') {
      return formatDateTime(value.toString());
    }

    if (type === 'tags') {
      const tags = value.toString().split(',').filter(Boolean);
      return (
        <div className="flex flex-wrap gap-1">
          {tags.map((tag, index) => (
            <span
              key={index}
              className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-50 text-blue-700"
            >
              {tag.trim()}
            </span>
          ))}
        </div>
      );
    }

    if (type === 'select' && options.includes(value.toString())) {
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-50 text-blue-700">
          {value}
        </span>
      );
    }

    return value;
  };

  if (isEditing) {
    return (
      <div className="flex items-center gap-1 animate-fadeIn">
        {type === 'select' ? (
          <select
            ref={inputRef as React.RefObject<HTMLSelectElement>}
            value={editValue}
            onChange={(e) => setEditValue(e.target.value)}
            onKeyDown={handleKeyDown}
            className="p-1 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow min-w-[150px]"
            disabled={isSaving}
          >
            <option value="">Not Set</option>
            {options.map((option) => (
              <option key={option} value={option}>{option}</option>
            ))}
          </select>
        ) : type === 'datetime' ? (
          <input
            ref={inputRef as React.RefObject<HTMLInputElement>}
            type="datetime-local"
            value={editValue}
            onChange={(e) => setEditValue(e.target.value)}
            onKeyDown={handleKeyDown}
            className="p-1 border rounded w-full focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow min-w-[150px]"
            disabled={isSaving}
          />
        ) : type === 'tags' ? (
          <TagInput
            value={editValue.split(',').filter(Boolean)}
            onChange={(values) => setEditValue(values.join(','))}
            suggestions={options}
            allowCustom={true}
            placeholder="Add tags..."
          />
        ) : (
          <input
            ref={inputRef as React.RefObject<HTMLInputElement>}
            type={type}
            value={editValue}
            onChange={(e) => setEditValue(e.target.value)}
            onKeyDown={handleKeyDown}
            className="p-1 border rounded w-full focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow min-w-[150px]"
            disabled={isSaving}
          />
        )}
        <button
          onClick={handleSave}
          disabled={isSaving}
          className="p-1 text-green-600 hover:bg-green-50 rounded transition-colors disabled:opacity-50"
        >
          <Check size={16} />
        </button>
        <button
          onClick={handleCancel}
          disabled={isSaving}
          className="p-1 text-red-600 hover:bg-red-50 rounded transition-colors disabled:opacity-50"
        >
          <X size={16} />
        </button>
      </div>
    );
  }

  return (
    <div
      onClick={(e) => {
        e.stopPropagation();
        if (!isSaving) setIsEditing(true);
      }}
      className={`cursor-pointer hover:bg-gray-50 p-1 rounded transition-colors ${className}`}
    >
      {renderValue()}
    </div>
  );
};