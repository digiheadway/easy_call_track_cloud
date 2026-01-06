import React, { useState, useRef, useEffect } from 'react';
import { Search, Settings, LayoutGrid, LayoutList, ArrowUpDown, Filter, IndianRupee, Coins } from 'lucide-react';
import { Lead } from '../types/lead';
import { InlineEdit } from './InlineEdit';
import { LeadForm } from './LeadForm';
import { LeadFiltersModal } from './LeadFiltersModal';
import { ColumnCustomization } from './ColumnCustomization';
import { formatDateTime } from '../utils/dateFormat';
import { Shimmer, LeadCardShimmer } from './Shimmer';
import { LeadCard } from './LeadCard';
import { 
  LEAD_STAGES, 
  NEXT_ACTIONS, 
  PROPERTY_TYPES, 
  SEGMENTS,
  TAGS,
  ASSIGNEES,
  SOURCES,
  MEDIUMS,
  LISTS,
  PLACEMENTS 
} from '../types/options';

interface Column {
  key: keyof Lead;
  label: string;
  visible: boolean;
  editable?: boolean;
  sortable?: boolean;
  minWidth?: number;
}

interface LeadListProps {
  leads: Lead[];
  view: 'list' | 'grid';
  onEdit: (leadId: number, field: keyof Lead, value: any) => void;
  onDelete: (id: number) => Promise<void>;
  isLoading: boolean;
  page: number;
  setPage: (page: number) => void;
  totalPages: number;
  sortField: keyof Lead;
  sortDirection: 'asc' | 'desc';
  onSort: (field: keyof Lead) => void;
  onFiltersChange: (filters: Record<string, string>) => void;
  onTagsChange: (tags: string[]) => void;
  filters: Record<string, string>;
  tags: string[];
  error: string | null;
  isFiltersOpen: boolean;
  setIsFiltersOpen: (open: boolean) => void;
  isColumnsOpen: boolean;
  setIsColumnsOpen: (open: boolean) => void;
  perPage: number;
  totalResults: number;
  onLeadSelect: (lead: Lead) => void;
  isAdmin: boolean;
}

const DEFAULT_COLUMNS: Column[] = [
  // Primary Information
  { key: 'id', label: 'ID', visible: true, editable: false, sortable: true, minWidth: 40 },
  { key: 'name', label: 'Name', visible: true, editable: false, sortable: true, minWidth: 150 },
  { key: 'phone', label: 'Phone', visible: true, editable: false, sortable: true, minWidth: 120 },
  { key: 'stage', label: 'Stage', visible: true, editable: true, sortable: true, minWidth: 150 },
  
  // Follow-up Information
  { key: 'next_action', label: 'Next Action', visible: true, editable: true, sortable: true, minWidth: 180 },
  { key: 'next_action_time', label: 'Next Action Time', visible: true, editable: true, sortable: true, minWidth: 150 },
  { key: 'next_action_note', label: 'Action Notes', visible: false, editable: true, sortable: false, minWidth: 200 },
  
  // Requirements
  { key: 'requirement_description', label: 'Requirement', visible: true, editable: true, sortable: false, minWidth: 300 },
  { key: 'budget', label: 'Budget (L)', visible: true, editable: true, sortable: true, minWidth: 120 },
  { key: 'preferred_type', label: 'Property Type', visible: true, editable: true, sortable: true, minWidth: 150 },
  { key: 'preferred_area', label: 'Area', visible: false, editable: true, sortable: true, minWidth: 150 },
  { key: 'purposes', label: 'Purpose', visible: false, editable: true, sortable: true, minWidth: 120 },
  
  // Classification
  { key: 'segment', label: 'Segment', visible: true, editable: true, sortable: true, minWidth: 120 },
  { key: 'priority', label: 'Priority', visible: true, editable: true, sortable: true, minWidth: 100 },
  { key: 'intent', label: 'Intent', visible: true, editable: true, sortable: true, minWidth: 80 },
  { key: 'tags', label: 'Tags', visible: true, editable: true, sortable: false, minWidth: 200 },
  
  // Source Information
  { key: 'source', label: 'Source', visible: true, editable: true, sortable: true, minWidth: 150 },
  { key: 'medium', label: 'Medium', visible: true, editable: true, sortable: true, minWidth: 120 },
  { key: 'placement', label: 'Placement', visible: false, editable: true, sortable: true, minWidth: 150 },
  
  // Assignment & Lists
  { key: 'assigned_to', label: 'Assigned To', visible: true, editable: true, sortable: true, minWidth: 150 },
  { key: 'list_name', label: 'Lists', visible: true, editable: true, sortable: true, minWidth: 150 },
  
  // Additional Contact
  { key: 'alternative_contact_details', label: 'Alt. Contact', visible: false, editable: true, sortable: false, minWidth: 150 },
  { key: 'address', label: 'Address', visible: false, editable: true, sortable: false, minWidth: 200 },
  { key: 'about_him', label: 'About Person', visible: false, editable: true, sortable: false, minWidth: 200 },
  
  // Notes & Custom Fields
  { key: 'note', label: 'Notes', visible: false, editable: true, sortable: false, minWidth: 300 },
  { key: 'data_1', label: 'Custom Field 1', visible: false, editable: true, sortable: false, minWidth: 150 },
  { key: 'data_2', label: 'Custom Field 2', visible: false, editable: true, sortable: false, minWidth: 150 },
  { key: 'data_3', label: 'Custom Field 3', visible: false, editable: true, sortable: false, minWidth: 150 },
  
  // System Fields
  { key: 'created_at', label: 'Created At', visible: false, editable: false, sortable: true, minWidth: 150 },
  { key: 'updated_at', label: 'Updated At', visible: false, editable: false, sortable: true, minWidth: 150 }
];

export const LeadList: React.FC<LeadListProps> = ({
  leads,
  view,
  onEdit,
  onDelete,
  isLoading,
  page,
  setPage,
  totalPages,
  sortField,
  sortDirection,
  onSort,
  onFiltersChange,
  onTagsChange,
  filters,
  tags,
  error,
  isFiltersOpen,
  setIsFiltersOpen,
  isColumnsOpen,
  setIsColumnsOpen,
  perPage,
  totalResults,
  onLeadSelect,
  isAdmin
}) => {
  const tableContainerRef = useRef<HTMLDivElement>(null);
  
  // Initialize columns from localStorage or use defaults
  const [columns, setColumns] = useState<Column[]>(() => {
    const savedColumns = localStorage.getItem('leadColumnsVisibility');
    if (savedColumns) {
      const parsedColumns = JSON.parse(savedColumns);
      // Merge saved visibility with default columns to ensure all properties are present
      return DEFAULT_COLUMNS.map(defaultCol => ({
        ...defaultCol,
        visible: parsedColumns[defaultCol.key] ?? defaultCol.visible
      }));
    }
    return DEFAULT_COLUMNS;
  });

  // Save column visibility to localStorage whenever it changes
  useEffect(() => {
    const visibilityState = columns.reduce((acc, col) => ({
      ...acc,
      [col.key]: col.visible
    }), {});
    localStorage.setItem('leadColumnsVisibility', JSON.stringify(visibilityState));
  }, [columns]);

  const handleColumnChange = (newColumns: Column[]) => {
    setColumns(newColumns);
  };

  const getFieldOptions = (column: Column): string[] => {
    switch (column.key) {
      case 'stage':
        return LEAD_STAGES;
      case 'next_action':
        return NEXT_ACTIONS;
      case 'preferred_type':
        return PROPERTY_TYPES;
      case 'segment':
        return SEGMENTS;
      case 'tags':
        return TAGS;
      case 'assigned_to':
        return ASSIGNEES;
      case 'source':
        return SOURCES;
      case 'medium':
        return MEDIUMS;
      case 'list_name':
        return LISTS;
      case 'placement':
        return PLACEMENTS;
      default:
        return [];
    }
  };

  const getFieldType = (column: Column): 'text' | 'number' | 'select' | 'datetime' | 'tags' => {
    switch (column.key) {
      case 'stage':
      case 'next_action':
      case 'segment':
      case 'preferred_type':
      case 'purposes':
      case 'medium':
        return 'select';
      case 'tags':
      case 'assigned_to':
      case 'source':
      case 'list_name':
        return 'tags';
      case 'next_action_time':
      case 'created_at':
      case 'updated_at':
        return 'datetime';
      case 'budget':
      case 'intent':
      case 'priority':
        return 'number';
      default:
        return 'text';
    }
  };

  const formatValue = (value: any, column: Column): React.ReactNode => {
    if (value === null || value === undefined || value === '') {
      return <span className="text-gray-400">-</span>;
    }
    
    if (column.key === 'next_action_time' || column.key === 'created_at' || column.key === 'updated_at') {
      return formatDateTime(value);
    }
    
    if (column.key === 'budget') {
      return (
        <div className="flex items-center gap-1">
          <IndianRupee size={14} className="text-emerald-600" />
          <span>{value}</span>
          <Coins size={14} className="text-emerald-600" />
        </div>
      );
    }
    
    if (column.key === 'priority') {
      const priorities = { 1: 'Low', 2: 'Medium', 3: 'High' };
      const colors = { 1: 'text-blue-600', 2: 'text-orange-600', 3: 'text-red-600' };
      return <span className={colors[value as keyof typeof colors]}>{priorities[value as keyof typeof priorities]}</span>;
    }

    if (column.key === 'tags' || column.key === 'assigned_to' || column.key === 'list_name') {
      return (
        <div className="flex flex-wrap gap-1">
          {value.split(',').map((tag: string, index: number) => (
            <span
              key={index}
              className="text-xs px-2 py-0.5 bg-orange-50 text-orange-600 rounded-full"
            >
              {tag.trim()}
            </span>
          ))}
        </div>
      );
    }
    
    return value.toString();
  };

  const handleSort = (column: Column) => {
    if (column.sortable) {
      onSort(column.key);
    }
  };

  const getSortIcon = (column: Column) => {
    if (!column.sortable) return null;
    
    if (sortField === column.key) {
      return (
        <span className={`ml-1 inline-block transition-transform ${sortDirection === 'desc' ? 'rotate-180' : ''}`}>
          ↑
        </span>
      );
    }
    return <ArrowUpDown size={14} className="ml-1 text-gray-400" />;
  };

  const handleRowClick = (e: React.MouseEvent, lead: Lead) => {
    const target = e.target as HTMLElement;
    if (
      target.tagName.toLowerCase() === 'input' ||
      target.tagName.toLowerCase() === 'button' ||
      target.closest('.inline-edit')
    ) {
      return;
    }
    onLeadSelect(lead);
  };

  const renderGridView = () => (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 p-2">
      {isLoading ? (
        Array.from({ length: 6 }).map((_, index) => (
          <LeadCardShimmer key={index} />
        ))
      ) : leads.length === 0 ? (
        <div className="col-span-full text-center py-8 text-light">
          No leads found
        </div>
      ) : (
        leads.map((lead) => (
          <LeadCard
            key={lead.id}
            lead={lead}
            onClick={() => onLeadSelect(lead)}
          />
        ))
      )}
    </div>
  );

  const getVisiblePages = () => {
    const delta = 2;
    const range = [];
    const rangeWithDots = [];
    let l;

    for (let i = 1; i <= totalPages; i++) {
      if (i === 1 || i === totalPages || (i >= page - delta && i <= page + delta)) {
        range.push(i);
      }
    }

    range.forEach(i => {
      if (l) {
        if (i - l === 2) {
          rangeWithDots.push(l + 1);
        } else if (i - l !== 1) {
          rangeWithDots.push('...');
        }
      }
      rangeWithDots.push(i);
      l = i;
    });

    return rangeWithDots;
  };

  return (
    <div className="w-full flex flex-col h-full p-2">
      {error ? (
        <div className="flex items-center justify-center p-8 text-red-600">
          {error}
        </div>
      ) : (
        <div className="flex-1 overflow-hidden">
          {view === 'grid' ? (
            <div className="h-full overflow-y-auto">
              {renderGridView()}
            </div>
          ) : (
            <div ref={tableContainerRef} className="h-full overflow-y-auto">
              <div className="rounded-lg overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50 sticky top-0 z-10">
                    <tr>
                      {columns.filter(col => col.visible).map(column => (
                        <th
                          key={column.key}
                          onClick={() => handleSort(column)}
                          className={`px-4 py-3 text-left text-sm font-medium text-gray-500 ${
                            column.sortable ? 'cursor-pointer hover:bg-gray-100' : ''
                          }`}
                          style={{ minWidth: column.minWidth }}
                        >
                          <div className="flex items-center">
                            {column.label}
                            {getSortIcon(column)}
                          </div>
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200 bg-white">
                    {isLoading ? (
                      Array.from({ length: 5 }).map((_, index) => (
                        <tr key={index}>
                          {columns.filter(col => col.visible).map((column, colIndex) => (
                            <td key={colIndex} className="px-4 py-3">
                              <Shimmer className="h-6 rounded" />
                            </td>
                          ))}
                        </tr>
                      ))
                    ) : leads.length === 0 ? (
                      <tr>
                        <td colSpan={columns.filter(col => col.visible).length} className="px-4 py-4 text-center text-gray-500">
                          No leads found
                        </td>
                      </tr>
                    ) : (
                      leads.map((lead) => (
                        <tr 
                          key={lead.id} 
                          className="hover:bg-gray-50 cursor-pointer transition-colors"
                          onClick={(e) => handleRowClick(e, lead)}
                        >
                          {columns.filter(col => col.visible).map(column => (
                            <td key={column.key} className="px-4 py-3 whitespace-normal">
                              {column.editable && (isAdmin || column.key !== 'stage') ? (
                                <div className="inline-edit min-w-[150px]">
                                  <InlineEdit
                                    value={lead[column.key]}
                                    onSave={(value) => onEdit(lead.id, column.key, value)}
                                    type={getFieldType(column)}
                                    options={getFieldOptions(column)}
                                    className={column.key === 'requirement_description' || column.key === 'note' ? 'line-clamp-2' : ''}
                                    placeholder={`-`}
                                  />
                                </div>
                              ) : (
                                <div className={`text-sm text-gray-900 ${
                                  column.key === 'requirement_description' || column.key === 'note' ? 'line-clamp-2' : ''
                                }`}>
                                  {formatValue(lead[column.key], column)}
                                </div>
                              )}
                            </td>
                          ))}
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      <div className="border-t border-gray-200 px-4 py-3 bg-white mb-2">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="text-sm text-gray-700">
            Showing <span className="font-medium">{leads.length}</span> of{' '}
            <span className="font-medium">{totalResults}</span> leads
          </div>
          <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px pagination-container">
            <button
              onClick={() => setPage(1)}
              disabled={page === 1}
              className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              First
            </button>
            <button
              onClick={() => setPage(page > 1 ? page - 1 : 1)}
              disabled={page === 1}
              className="relative inline-flex items-center px-2 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            {getVisiblePages().map((pageNum, index) => (
              pageNum === '...' ? (
                <span
                  key={`ellipsis-${index}`}
                  className="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700"
                >
                  ...
                </span>
              ) : (
                <button
                  key={pageNum}
                  onClick={() => setPage(pageNum as number)}
                  className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                    page === pageNum
                      ? 'z-10 bg-blue-50 border-blue-500 text-blue-600'
                      : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
                  }`}
                >
                  {pageNum}
                </button>
              )
            ))}
            <button
              onClick={() => setPage(page < totalPages ? page + 1 : totalPages)}
              disabled={page === totalPages}
              className="relative inline-flex items-center px-2 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
            <button
              onClick={() => setPage(totalPages)}
              disabled={page === totalPages}
              className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Last
            </button>
          </nav>
        </div>
      </div>

      <LeadFiltersModal
        isOpen={isFiltersOpen}
        onClose={() => setIsFiltersOpen(false)}
        onFiltersChange={onFiltersChange}
        onTagsChange={onTagsChange}
        currentFilters={filters}
        currentTags={tags}
      />

      <ColumnCustomization
        isOpen={isColumnsOpen}
        onClose={() => setIsColumnsOpen(false)}
        columns={columns}
        onColumnsChange={handleColumnChange}
      />
    </div>
  );
};