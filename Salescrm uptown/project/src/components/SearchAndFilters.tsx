import React, { useEffect, useRef } from 'react';
import { Search, Filter, Settings, LayoutGrid, LayoutList } from 'lucide-react';
import { FilterDisplay } from './FilterDisplay';
import { FilterState, getFilterCount } from '../types/filters';

interface SearchAndFiltersProps {
  searchTerm: string;
  setSearchTerm: (term: string) => void;
  view: 'list' | 'grid';
  onViewChange: (view: 'list' | 'grid') => void;
  onOpenFilters: () => void;
  onOpenColumns: () => void;
  appliedFilters: FilterState;
  onRemoveFilter: (key: string) => void;
  onClearFilters: () => void;
  isAdmin: boolean;
}

export const SearchAndFilters: React.FC<SearchAndFiltersProps> = ({
  searchTerm,
  setSearchTerm,
  view,
  onViewChange,
  onOpenFilters,
  onOpenColumns,
  appliedFilters,
  onRemoveFilter,
  onClearFilters,
  isAdmin,
}) => {
  const filterCount = getFilterCount(appliedFilters);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Handle clipboard data on mount and focus
  useEffect(() => {
    const handleClipboardData = async () => {
      try {
        const text = await navigator.clipboard.readText();
        const phoneNumber = text.replace(/[\s+]/g, '');
        if (/^\d{10,}$/.test(phoneNumber)) {
          let processedNumber = phoneNumber;
          if (phoneNumber.length > 10 && phoneNumber.startsWith('91')) {
            processedNumber = phoneNumber.slice(2);
          }
          if (processedNumber.length === 10) {
            setSearchTerm(processedNumber);
          }
        }
      } catch (err) {
        // Silent fail if clipboard access is denied
      }
    };

    const input = searchInputRef.current;
    if (input) {
      input.addEventListener('focus', handleClipboardData);
      return () => input.removeEventListener('focus', handleClipboardData);
    }
  }, []);

  return (
    <div className="space-y-4 bg-white p-4">
      <div className="flex items-center gap-4">
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
          <input
            ref={searchInputRef}
            type="text"
            placeholder="Search leads..."
            className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={onOpenFilters}
            className="relative p-2 hover:bg-gray-100 rounded-lg transition-colors flex items-center gap-1"
          >
            <Filter size={20} />
            {filterCount > (isAdmin ? 0 : 1) && (
              <span className="absolute -top-1 -right-1 bg-blue-500 text-white text-xs rounded-full w-4 h-4 flex items-center justify-center">
                {filterCount - (isAdmin ? 0 : 1)}
              </span>
            )}
          </button>
          <button
            onClick={onOpenColumns}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors flex items-center gap-1"
          >
            <Settings size={20} />
          </button>
          <div className="flex gap-1 bg-gray-100 p-1 rounded-lg">
            <button
              className={`p-2 rounded-md transition-all ${view === 'list' ? 'bg-white shadow' : ''}`}
              onClick={() => onViewChange('list')}
            >
              <LayoutList size={20} />
            </button>
            <button
              className={`p-2 rounded-md transition-all ${view === 'grid' ? 'bg-white shadow' : ''}`}
              onClick={() => onViewChange('grid')}
            >
              <LayoutGrid size={20} />
            </button>
          </div>
        </div>
      </div>

      <FilterDisplay
        filters={appliedFilters}
        onRemoveFilter={onRemoveFilter}
        onClearFilters={onClearFilters}
        isAdmin={isAdmin}
      />
    </div>
  );
}