import React, { useState, useEffect } from 'react';
import Modal from '../common/Modal';
import { useAppContext } from '../../contexts/AppContext';
import { FilterOption } from '../../types/o-index';
import { dropdownOptions, tagOptions } from '../../data/options';
import Dropdown from '../common/Dropdown';
import TagInput from '../common/TagInput';
import { X } from 'lucide-react';

interface LeadFilterModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const LeadFilterModal: React.FC<LeadFilterModalProps> = ({ isOpen, onClose }) => {
  const { leadFilters, setLeadFilters, clearLeadFilters } = useAppContext();
  const [activeFilters, setActiveFilters] = useState<FilterOption[]>(leadFilters);

  const [stage, setStage] = useState('');
  const [priority, setPriority] = useState('');
  const [source, setSource] = useState('');
  const [segment, setSegment] = useState('');
  const [purpose, setPurpose] = useState('');
  const [purchaseTimeline, setPurchaseTimeline] = useState('');
  const [propertyTypes, setPropertyTypes] = useState<string[]>([]);
  const [preferredSizes, setPreferredSizes] = useState<string[]>([]);
  const [tags, setTags] = useState<string[]>([]);
  const [assignedTo, setAssignedTo] = useState<string[]>([]);
  const [budget, setBudget] = useState<{ min?: string; max?: string }>({});

  // Sync activeFilters with leadFilters when they change
  useEffect(() => {
    setActiveFilters(leadFilters);
  }, [leadFilters]);

  const handleApplyFilters = () => {
    const newFilters: FilterOption[] = [];

    if (stage) {
      newFilters.push({ field: 'stage', operator: '=', value: stage });
    }
    if (priority) {
      newFilters.push({ field: 'priority', operator: '=', value: priority });
    }
    if (source) {
      newFilters.push({ field: 'source', operator: '=', value: source });
    }
    if (segment) {
      newFilters.push({ field: 'segment', operator: '=', value: segment });
    }
    if (purpose) {
      newFilters.push({ field: 'purpose', operator: '=', value: purpose });
    }
    if (purchaseTimeline) {
      newFilters.push({ field: 'purchaseTimeline', operator: '=', value: purchaseTimeline });
    }
    if (propertyTypes.length > 0) {
      newFilters.push({ field: 'propertyType', operator: '=', value: propertyTypes });
    }
    if (preferredSizes.length > 0) {
      newFilters.push({ field: 'preferredSize', operator: '=', value: preferredSizes });
    }
    if (tags.length > 0) {
      newFilters.push({ field: 'tags', operator: '=', value: tags });
    }
    if (assignedTo.length > 0) {
      newFilters.push({ field: 'assignedTo', operator: '=', value: assignedTo });
    }
    if (budget.min) {
      newFilters.push({ field: 'budget', operator: '>=', value: parseFloat(budget.min) });
    }
    if (budget.max) {
      newFilters.push({ field: 'budget', operator: '<=', value: parseFloat(budget.max) });
    }

    setLeadFilters(newFilters);
    onClose();
  };

  const handleRemoveFilter = (index: number) => {
    const newFilters = [...activeFilters];
    newFilters.splice(index, 1);
    setActiveFilters(newFilters);
    setLeadFilters(newFilters);
  };

  const handleClearFilters = () => {
    setStage('');
    setPriority('');
    setSource('');
    setSegment('');
    setPurpose('');
    setPurchaseTimeline('');
    setPropertyTypes([]);
    setPreferredSizes([]);
    setTags([]);
    setAssignedTo([]);
    setBudget({});
    setActiveFilters([]);
    clearLeadFilters();
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Filters"
      size="lg"
    >
      <div className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Stage</label>
            <Dropdown
              options={dropdownOptions.stage}
              value={stage}
              onChange={setStage}
              placeholder="All Stages"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Priority</label>
            <Dropdown
              options={dropdownOptions.priority}
              value={priority}
              onChange={setPriority}
              placeholder="All Priorities"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Source</label>
            <Dropdown
              options={dropdownOptions.source}
              value={source}
              onChange={setSource}
              placeholder="All Sources"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Segment</label>
            <Dropdown
              options={dropdownOptions.segment}
              value={segment}
              onChange={setSegment}
              placeholder="All Segments"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Purpose</label>
            <Dropdown
              options={dropdownOptions.purpose}
              value={purpose}
              onChange={setPurpose}
              placeholder="All Purposes"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Purchase Timeline</label>
            <Dropdown
              options={dropdownOptions.purchaseTimeline}
              value={purchaseTimeline}
              onChange={setPurchaseTimeline}
              placeholder="All Timelines"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Property Type</label>
            <TagInput
              options={tagOptions.propertyType}
              value={propertyTypes}
              onChange={setPropertyTypes}
              placeholder="Select property types..."
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Size</label>
            <TagInput
              options={tagOptions.preferredSize}
              value={preferredSizes}
              onChange={setPreferredSizes}
              placeholder="Select sizes..."
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Tags</label>
            <TagInput
              options={tagOptions.tags}
              value={tags}
              onChange={setTags}
              placeholder="Select tags..."
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Assigned To</label>
            <TagInput
              options={tagOptions.assignedTo}
              value={assignedTo}
              onChange={setAssignedTo}
              placeholder="Select team members..."
            />
          </div>

          <div className="col-span-1 sm:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">Budget Range (in Lakhs)</label>
            <div className="grid grid-cols-2 gap-4">
              <input
                type="number"
                placeholder="Min"
                className="input"
                value={budget.min || ''}
                onChange={(e) => setBudget({ ...budget, min: e.target.value })}
              />
              <input
                type="number"
                placeholder="Max"
                className="input"
                value={budget.max || ''}
                onChange={(e) => setBudget({ ...budget, max: e.target.value })}
              />
            </div>
          </div>
        </div>

        {activeFilters.length > 0 && (
          <div className="mt-4">
            <h3 className="text-sm font-medium text-gray-700 mb-2">Active Filters</h3>
            <div className="flex flex-wrap gap-2">
              {activeFilters.map((filter, index) => (
                <div
                  key={index}
                  className="inline-flex items-center px-3 py-1 rounded-full bg-gray-100 text-sm"
                >
                  <span>{filter.field}: {filter.value.toString()}</span>
                  <button
                    onClick={() => handleRemoveFilter(index)}
                    className="ml-2 text-gray-500 hover:text-gray-700"
                  >
                    <X size={14} />
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      <div className="flex justify-end mt-6 space-x-3">
        <button 
          className="btn btn-outline"
          onClick={handleClearFilters}
        >
          Clear Filters
        </button>
        <button 
          className="btn btn-primary"
          onClick={handleApplyFilters}
        >
          Apply Filters
        </button>
      </div>
    </Modal>
  );
};

export default LeadFilterModal;