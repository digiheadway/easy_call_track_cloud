import React from 'react';
import { X } from 'lucide-react';
import { FilterDisplayProps } from '../types/filters';
import { formatNextActionTimeFilter } from '../types/filters';

interface FilterDisplayProps {
  filters: Record<string, any>;
  onRemoveFilter: (key: string) => void;
  onClearFilters: () => void;
  isAdmin: boolean;
}

const formatFilterLabel = (key: string, value: string | string[]): string => {
  // Format the key
  const formattedKey = key
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');

  // Special handling for next action time filters
  if (key === 'next_action_time') {
    const formatted = formatNextActionTimeFilter(value.toString());
    if (formatted) return formatted;
  }

  return `${formattedKey}: ${value}`;
};

export const FilterDisplay: React.FC<FilterDisplayProps> = ({
  filters,
  onRemoveFilter,
  onClearFilters,
  isAdmin
}) => {
  const hasFilters = Object.entries(filters).some(([key, value]) => 
    value !== undefined && value !== null && 
    (Array.isArray(value) ? value.length > 0 : value !== '') &&
    (isAdmin || key !== 'assigned_to')
  );

  const hasRemovableFilters = Object.entries(filters).some(([key, value]) => 
    value !== undefined && value !== null && 
    (Array.isArray(value) ? value.length > 0 : value !== '') &&
    (isAdmin || key !== 'assigned_to')
  );

  if (!hasFilters) return null;

  return (
    <div className="flex flex-wrap items-center gap-2 text-sm">
      {Object.entries(filters).map(([key, value]) => {
        // Skip rendering assigned_to filter for non-admin users
        if (!isAdmin && key === 'assigned_to') return null;
        if (!value || (Array.isArray(value) && value.length === 0) || key === 'custom_date') return null;
        
        return (
          <div key={key} className="bg-blue-50 text-blue-700 px-3 py-1 rounded-full flex items-center gap-2">
            <span>{formatFilterLabel(key, value)}</span>
            {(isAdmin || key !== 'assigned_to') && (
              <button
                onClick={() => onRemoveFilter(key)}
                className="hover:bg-blue-100 rounded-full p-1"
              >
                <X size={14} />
              </button>
            )}
          </div>
        );
      })}
      {/* Only show clear filters button for admin users and when there are removable filters */}
      {isAdmin && hasRemovableFilters && (
        <button
          onClick={onClearFilters}
          className="text-gray-600 hover:text-gray-900 text-sm"
        >
          Clear all filters
        </button>
      )}
    </div>
  );
};