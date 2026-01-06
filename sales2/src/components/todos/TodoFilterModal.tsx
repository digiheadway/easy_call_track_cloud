import React, { useState } from 'react';
import Modal from '../common/Modal';
import { useAppContext } from '../../contexts/AppContext';
import { FilterOption, DropdownOption } from '../../types';
import { dropdownOptions } from '../../data/options';
import Dropdown from '../common/Dropdown';
import { X } from 'lucide-react';
import Badge from '../ui/Badge';

interface TodoFilterModalProps {
  isOpen: boolean;
  onClose: () => void;
  defaultType?: string;
}

const TodoFilterModal: React.FC<TodoFilterModalProps> = ({ 
  isOpen, 
  onClose,
  defaultType 
}) => {
  const { todoFilters, setTodoFilters, clearTodoFilters } = useAppContext();
  const [activeFilters, setActiveFilters] = useState<FilterOption[]>(todoFilters);

  const [field, setField] = useState<string>('');
  const [operator, setOperator] = useState<string>('=');
  const [value, setValue] = useState<string>('');

  const fieldOptions: DropdownOption[] = [
    { value: 'type', label: 'Type' },
    { value: 'status', label: 'Status' },
    { value: 'leadId', label: 'Lead ID' },
    { value: 'description', label: 'Description' },
  ];

  const operatorOptions: DropdownOption[] = [
    { value: '=', label: 'Equals' },
    { value: 'contains', label: 'Contains' },
  ];

  // Get value options based on selected field
  const getValueOptions = (): DropdownOption[] => {
    switch (field) {
      case 'type':
        return dropdownOptions.todoType;
      case 'status':
        return dropdownOptions.todoStatus;
      default:
        return [];
    }
  };

  const isLeadIdField = field === 'leadId';
  const isTextField = field === 'description';
  const showValueDropdown = !isLeadIdField && !isTextField && field !== '';

  const addFilter = () => {
    if (!field || !value) return;
    
    let filterValue: string | number = value;
    
    if (isLeadIdField && value) {
      filterValue = parseInt(value);
    }
    
    const newFilter: FilterOption = {
      field,
      operator,
      value: filterValue,
    };
    
    setActiveFilters([...activeFilters, newFilter]);
    
    // Reset form
    setField('');
    setOperator('=');
    setValue('');
  };

  const removeFilter = (index: number) => {
    const newFilters = [...activeFilters];
    newFilters.splice(index, 1);
    setActiveFilters(newFilters);
  };

  const handleApply = () => {
    // If defaultType is provided, ensure it's always included in filters
    let finalFilters = [...activeFilters];
    if (defaultType) {
      const typeFilterExists = finalFilters.some(f => f.field === 'type' && f.value === defaultType);
      if (!typeFilterExists) {
        finalFilters.push({
          field: 'type',
          operator: '=',
          value: defaultType
        });
      }
    }
    setTodoFilters(finalFilters);
    onClose();
  };

  const handleClear = () => {
    let clearedFilters: FilterOption[] = [];
    // If defaultType is provided, keep it as the only filter
    if (defaultType) {
      clearedFilters = [{
        field: 'type',
        operator: '=',
        value: defaultType
      }];
    }
    setActiveFilters(clearedFilters);
    setTodoFilters(clearedFilters);
    onClose();
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Filter Tasks"
      size="md"
    >
      <div className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Field</label>
            <Dropdown
              options={fieldOptions}
              value={field}
              onChange={setField}
              placeholder="Select field"
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Operator</label>
            <Dropdown
              options={operatorOptions}
              value={operator}
              onChange={setOperator}
              placeholder="Select operator"
            />
          </div>
          
          {isLeadIdField && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Value</label>
              <input
                type="number"
                className="input"
                value={value}
                onChange={(e) => setValue(e.target.value)}
                placeholder="Enter lead ID"
              />
            </div>
          )}
          
          {isTextField && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Value</label>
              <input
                type="text"
                className="input"
                value={value}
                onChange={(e) => setValue(e.target.value)}
                placeholder={`Enter ${field}`}
              />
            </div>
          )}
          
          {showValueDropdown && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Value</label>
              <Dropdown
                options={getValueOptions()}
                value={value}
                onChange={setValue}
                placeholder="Select value"
              />
            </div>
          )}
        </div>
        
        <div className="mt-4">
          <button 
            className="btn btn-primary w-full"
            onClick={addFilter}
            disabled={!field || !value}
          >
            Add Filter
          </button>
        </div>
        
        {activeFilters.length > 0 && (
          <div className="mt-4">
            <h3 className="text-sm font-medium text-gray-700 mb-2">Active Filters</h3>
            <div className="bg-gray-50 p-3 rounded-md border border-gray-200">
              <div className="flex flex-wrap gap-2">
                {activeFilters.map((filter, index) => (
                  <div 
                    key={index} 
                    className="bg-white px-3 py-1 rounded-full border border-gray-300 flex items-center"
                  >
                    <span className="text-sm font-medium text-gray-700">{filter.field}</span>
                    <span className="text-xs text-gray-500 mx-1">
                      {filter.operator === '=' ? 'is' : 
                       filter.operator === 'contains' ? 'contains' : filter.operator}
                    </span>
                    <span className="text-sm text-gray-900">{filter.value.toString()}</span>
                    {/* Don't show remove button for default type filter */}
                    {!(defaultType && filter.field === 'type' && filter.value === defaultType) && (
                      <button 
                        className="ml-2 text-gray-400 hover:text-gray-600"
                        onClick={() => removeFilter(index)}
                      >
                        <X size={14} />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>
      
      <div className="flex justify-end mt-6 space-x-3">
        <button 
          className="btn btn-outline"
          onClick={handleClear}
        >
          Clear All
        </button>
        <button 
          className="btn btn-primary"
          onClick={handleApply}
          disabled={activeFilters.length === 0}
        >
          Apply Filters
        </button>
      </div>
    </Modal>
  );
};

export default TodoFilterModal;